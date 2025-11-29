package wypozyczalnia.repositories.cache;

import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import wypozyczalnia.cache.CacheKeys;
import wypozyczalnia.cache.CacheManager;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.repositories.NajemcaRepozytorium;

import java.time.Duration;

/**
 * Dekorator dla NajemcaRepozytorium dodający obsługę cache'a Redis.
 */
public class CachedNajemcaRepozytorium extends NajemcaRepozytorium {

    private final NajemcaRepozytorium originalRepository;
    private final CacheManager cacheManager;

    // TTL dla cache'a najemców (15 minut)
    private static final Duration NAJEMCA_TTL = Duration.ofMinutes(15);

    public CachedNajemcaRepozytorium(NajemcaRepozytorium originalRepository, CacheManager cacheManager, MongoDatabase database) {
        super(database);
        this.originalRepository = originalRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    public void dodaj(Najemca najemca) {
        // Dodaj do bazy danych
        originalRepository.dodaj(najemca);

        // Dodaj do cache'a jeśli ma ID (zostało przypisane przez MongoDB)
        if (najemca.getId() != null) {
            String keyById = CacheKeys.najemcaById(najemca.getId());
            cacheManager.put(keyById, najemca, NAJEMCA_TTL);

            if (najemca.getLogin() != null) {
                String keyByLogin = CacheKeys.najemcaByLogin(najemca.getLogin());
                cacheManager.put(keyByLogin, najemca, NAJEMCA_TTL);
            }
        }
    }

    @Override
    public Najemca znajdz(ObjectId id) {
        String key = CacheKeys.najemcaById(id);

        // Spróbuj pobrać z cache'a
        Najemca najemca = cacheManager.get(key, Najemca.class);
        if (najemca != null) {
            return najemca;
        }

        // Jeśli nie ma w cache'u lub Redis niedostępny, pobierz z MongoDB
        najemca = originalRepository.znajdz(id);
        if (najemca != null && cacheManager.isAvailable()) {
            // Zapisz w cache'u
            cacheManager.put(key, najemca, NAJEMCA_TTL);

            // Zapisz też pod kluczem loginu
            if (najemca.getLogin() != null) {
                String keyByLogin = CacheKeys.najemcaByLogin(najemca.getLogin());
                cacheManager.put(keyByLogin, najemca, NAJEMCA_TTL);
            }
        }

        return najemca;
    }

    @Override
    public Najemca znajdzLogin(String login) {
        String key = CacheKeys.najemcaByLogin(login);

        // Spróbuj pobrać z cache'a
        Najemca najemca = cacheManager.get(key, Najemca.class);
        if (najemca != null) {
            return najemca;
        }

        // Jeśli nie ma w cache'u lub Redis niedostępny, pobierz z MongoDB
        najemca = originalRepository.znajdzLogin(login);
        if (najemca != null && cacheManager.isAvailable()) {
            // Zapisz w cache'u
            cacheManager.put(key, najemca, NAJEMCA_TTL);

            // Zapisz też pod kluczem ID
            if (najemca.getId() != null) {
                String keyById = CacheKeys.najemcaById(najemca.getId());
                cacheManager.put(keyById, najemca, NAJEMCA_TTL);
            }
        }

        return najemca;
    }

    @Override
    public void aktualizuj(Najemca najemca) {
        // Usuń z cache'a przed aktualizacją
        invalidateCache(najemca);

        // Aktualizuj w bazie danych
        originalRepository.aktualizuj(najemca);

        // Dodaj zaktualizowany obiekt do cache'a
        if (najemca.getId() != null && cacheManager.isAvailable()) {
            String keyById = CacheKeys.najemcaById(najemca.getId());
            cacheManager.put(keyById, najemca, NAJEMCA_TTL);

            if (najemca.getLogin() != null) {
                String keyByLogin = CacheKeys.najemcaByLogin(najemca.getLogin());
                cacheManager.put(keyByLogin, najemca, NAJEMCA_TTL);
            }
        }
    }

    @Override
    public void usun(Najemca najemca) {
        // Usuń z cache'a
        invalidateCache(najemca);

        // Usuń z bazy danych
        originalRepository.usun(najemca);
    }

    /**
     * Usuwa najemcę z cache'a (inwalidacja).
     */
    private void invalidateCache(Najemca najemca) {
        if (najemca.getId() != null) {
            String keyById = CacheKeys.najemcaById(najemca.getId());
            cacheManager.delete(keyById);
        }

        if (najemca.getLogin() != null) {
            String keyByLogin = CacheKeys.najemcaByLogin(najemca.getLogin());
            cacheManager.delete(keyByLogin);
        }
    }

    /**
     * Czyści cały cache najemców.
     */
    public void clearCache() {
        cacheManager.deleteByPrefix(CacheKeys.NAJEMCA_PREFIX);
        cacheManager.deleteByPrefix(CacheKeys.NAJEMCA_LOGIN_PREFIX);
    }
}
