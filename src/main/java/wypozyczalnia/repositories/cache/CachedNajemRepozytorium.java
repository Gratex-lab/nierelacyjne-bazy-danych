package wypozyczalnia.repositories.cache;

import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import wypozyczalnia.cache.CacheManager;
import wypozyczalnia.objects.Najem;
import wypozyczalnia.repositories.NajemRepozytorium;

import java.time.Duration;
import java.util.List;

/**
 * Dekorator dla NajemRepozytorium dodający obsługę cache'a Redis.
 */
public class CachedNajemRepozytorium extends NajemRepozytorium {

    private final NajemRepozytorium originalRepository;
    private final CacheManager cacheManager;

    // TTL dla cache'a najmów (5 minut)
    private static final Duration NAJEM_TTL = Duration.ofMinutes(5);

    public CachedNajemRepozytorium(NajemRepozytorium originalRepository, CacheManager cacheManager, MongoDatabase database) {
        super(database);
        this.originalRepository = originalRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    public void dodaj(Najem najem) {
        // Dodaj do bazy danych
        originalRepository.dodaj(najem);

        // Dodaj do cache'a jeśli ma ID
        if (najem.getId() != null && cacheManager.isAvailable()) {
            String key = najemKey(najem.getId());
            cacheManager.put(key, najem, NAJEM_TTL);
        }

        // Inwaliduj cache listy najmów dla tego najemcy i nieruchomości
        invalidateListCache(najem);
    }

    @Override
    public Najem znajdz(ObjectId id) {
        String key = najemKey(id);

        // Spróbuj pobrać z cache'a
        Najem najem = cacheManager.get(key, Najem.class);
        if (najem != null) {
            return najem;
        }

        // Jeśli nie ma w cache'u lub Redis niedostępny, pobierz z MongoDB
        najem = originalRepository.znajdz(id);
        if (najem != null && cacheManager.isAvailable()) {
            cacheManager.put(key, najem, NAJEM_TTL);
        }

        return najem;
    }

    @Override
    public void aktualizuj(Najem najem) {
        // Usuń z cache'a przed aktualizacją
        if (najem.getId() != null) {
            String key = najemKey(najem.getId());
            cacheManager.delete(key);
        }

        // Inwaliduj cache listy
        invalidateListCache(najem);

        // Aktualizuj w bazie danych
        originalRepository.aktualizuj(najem);

        // Dodaj zaktualizowany obiekt do cache'a
        if (najem.getId() != null && cacheManager.isAvailable()) {
            String key = najemKey(najem.getId());
            cacheManager.put(key, najem, NAJEM_TTL);
        }
    }

    @Override
    public void usun(Najem najem) {
        // Usuń z cache'a
        if (najem.getId() != null) {
            String key = najemKey(najem.getId());
            cacheManager.delete(key);
        }

        // Inwaliduj cache listy
        invalidateListCache(najem);

        // Usuń z bazy danych
        originalRepository.usun(najem);
    }

    /**
     * Znajduje najmy dla danego najemcy i nieruchomości. Cache'uje wyniki na
     * krótki czas.
     */
    @Override
    public List<Najem> znajdzNajemcowi(ObjectId nieruchomoscId, ObjectId najemcaId) {
        String key = najmyListKey(nieruchomoscId, najemcaId);

        // Sprawdź cache (dla list używamy krótszego TTL)
        @SuppressWarnings("unchecked")
        List<Najem> cached = cacheManager.get(key, List.class);
        if (cached != null) {
            return cached;
        }

        // Pobierz z bazy danych
        List<Najem> najmy = originalRepository.znajdzNajemcowi(nieruchomoscId, najemcaId);

        // Zapisz w cache'u (krótszy TTL dla list)
        if (cacheManager.isAvailable()) {
            cacheManager.put(key, najmy, Duration.ofMinutes(2));
        }

        return najmy;
    }

    /**
     * Generuje klucz cache'a dla pojedynczego najmu.
     */
    private String najemKey(ObjectId id) {
        return "najem:" + id.toHexString();
    }

    /**
     * Generuje klucz cache'a dla listy najmów.
     */
    private String najmyListKey(ObjectId nieruchomoscId, ObjectId najemcaId) {
        return "najmy_list:" + nieruchomoscId.toHexString() + ":" + najemcaId.toHexString();
    }

    /**
     * Inwaliduje cache list najmów związanych z tym najmem.
     */
    private void invalidateListCache(Najem najem) {
        if (najem.getNieruchomoscId() != null && najem.getNajemcaId() != null) {
            String listKey = najmyListKey(najem.getNieruchomoscId(), najem.getNajemcaId());
            cacheManager.delete(listKey);
        }
    }

    /**
     * Czyści cały cache najmów.
     */
    public void clearCache() {
        cacheManager.deleteByPrefix("najem:");
        cacheManager.deleteByPrefix("najmy_list:");
    }
}
