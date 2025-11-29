package wypozyczalnia.repositories.cache;

import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import wypozyczalnia.cache.CacheKeys;
import wypozyczalnia.cache.CacheManager;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;
import wypozyczalnia.repositories.NieruchomoscRepozytorium;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Dekorator dla NieruchomoscRepozytorium dodający obsługę cache'a Redis.
 */
public class CachedNieruchomoscRepozytorium extends NieruchomoscRepozytorium {

    private final NieruchomoscRepozytorium originalRepository;
    private final CacheManager cacheManager;

    // TTL dla cache'a nieruchomości (10 minut)
    private static final Duration NIERUCHOMOSC_TTL = Duration.ofMinutes(10);
    // TTL dla cache'a dostępności (2 minuty - często się zmienia)
    private static final Duration DOSTEPNOSC_TTL = Duration.ofMinutes(2);

    public CachedNieruchomoscRepozytorium(NieruchomoscRepozytorium originalRepository, CacheManager cacheManager, MongoDatabase database) {
        super(database);
        this.originalRepository = originalRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    public void dodaj(Nieruchomosc nieruchomosc) {
        // Dodaj do bazy danych
        originalRepository.dodaj(nieruchomosc);

        // Dodaj do cache'a jeśli ma ID
        if (nieruchomosc.getId() != null && cacheManager.isAvailable()) {
            String key = CacheKeys.nieruchomoscById(nieruchomosc.getId());
            cacheManager.put(key, nieruchomosc, NIERUCHOMOSC_TTL);
        }
    }

    @Override
    public Nieruchomosc znajdz(ObjectId id) {
        String key = CacheKeys.nieruchomoscById(id);

        // Spróbuj pobrać z cache'a
        Nieruchomosc nieruchomosc = cacheManager.get(key, Nieruchomosc.class);
        if (nieruchomosc != null) {
            return nieruchomosc;
        }

        // Jeśli nie ma w cache'u lub Redis niedostępny, pobierz z MongoDB
        nieruchomosc = originalRepository.znajdz(id);
        if (nieruchomosc != null && cacheManager.isAvailable()) {
            // Zapisz w cache'u
            cacheManager.put(key, nieruchomosc, NIERUCHOMOSC_TTL);
        }

        return nieruchomosc;
    }

    @Override
    public void aktualizuj(Nieruchomosc nieruchomosc) {
        // Usuń z cache'a przed aktualizacją
        invalidateCache(nieruchomosc);

        // Aktualizuj w bazie danych
        originalRepository.aktualizuj(nieruchomosc);

        // Dodaj zaktualizowany obiekt do cache'a
        if (nieruchomosc.getId() != null && cacheManager.isAvailable()) {
            String key = CacheKeys.nieruchomoscById(nieruchomosc.getId());
            cacheManager.put(key, nieruchomosc, NIERUCHOMOSC_TTL);
        }
    }

    @Override
    public void usun(Nieruchomosc nieruchomosc) {
        // Usuń z cache'a
        invalidateCache(nieruchomosc);

        // Usuń z bazy danych
        originalRepository.usun(nieruchomosc);
    }

    /**
     * Sprawdza czy nieruchomość jest zajęta w podanym okresie. Cache'uje wyniki
     * sprawdzenia dostępności.
     */
    @Override
    public boolean czyJestZajeta(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        // Generuj klucz cache'a dla tego okresu
        String period = start.toString() + "_" + koniec.toString();
        String key = CacheKeys.dostepnosc(nieruchomosc.getId(), period);

        // Sprawdź cache
        Boolean cached = cacheManager.get(key, Boolean.class);
        if (cached != null) {
            return cached;
        }

        // Sprawdź w bazie danych
        boolean zajeta = originalRepository.czyJestZajeta(nieruchomosc, start, koniec);

        // Zapisz w cache'u jeśli Redis jest dostępny
        if (cacheManager.isAvailable()) {
            cacheManager.put(key, zajeta, DOSTEPNOSC_TTL);
        }

        return zajeta;
    }

    /**
     * Próbuje zarezerwować nieruchomość. Inwaliduje cache dostępności po udanej
     * rezerwacji.
     */
    @Override
    public boolean sprobujZarezerwowac(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        boolean zarezerwowano = originalRepository.sprobujZarezerwowac(nieruchomosc, start, koniec);

        if (zarezerwowano && cacheManager.isAvailable()) {
            // Usuń cache dostępności dla tej nieruchomości
            String prefix = CacheKeys.dostepnoscPrefix(nieruchomosc.getId());
            cacheManager.deleteByPrefix(prefix);

            // Zaktualizuj cache'owaną nieruchomość (wersja się zmieniła)
            String nieruchomoscKey = CacheKeys.nieruchomoscById(nieruchomosc.getId());
            cacheManager.put(nieruchomoscKey, nieruchomosc, NIERUCHOMOSC_TTL);
        }

        return zarezerwowano;
    }

    /**
     * Usuwa nieruchomość z cache'a (inwalidacja).
     */
    private void invalidateCache(Nieruchomosc nieruchomosc) {
        if (nieruchomosc.getId() != null && cacheManager.isAvailable()) {
            // Usuń cache'owaną nieruchomość
            String key = CacheKeys.nieruchomoscById(nieruchomosc.getId());
            cacheManager.delete(key);

            // Usuń cache dostępności
            String dostepnoscPrefix = CacheKeys.dostepnoscPrefix(nieruchomosc.getId());
            cacheManager.deleteByPrefix(dostepnoscPrefix);
        }
    }

    /**
     * Czyści cały cache nieruchomości.
     */
    public void clearCache() {
        cacheManager.deleteByPrefix(CacheKeys.NIERUCHOMOSC_PREFIX);
        cacheManager.deleteByPrefix(CacheKeys.DOSTEPNOSC_PREFIX);
    }
}
