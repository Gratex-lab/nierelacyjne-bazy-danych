package wypozyczalnia.cache;

import java.time.Duration;

/**
 * Interfejs do zarządzania cache'em. Umożliwia zapisywanie, pobieranie i
 * usuwanie danych z cache'a.
 */
public interface CacheManager {

    /**
     * Zapisuje obiekt w cache'u z określonym TTL.
     */
    <T> void put(String key, T value, Duration ttl);

    /**
     * Pobiera obiekt z cache'a.
     */
    <T> T get(String key, Class<T> type);

    /**
     * Usuwa obiekt z cache'a.
     */
    void delete(String key);

    /**
     * Usuwa wszystkie obiekty z cache'a z określonym prefiksem.
     */
    void deleteByPrefix(String prefix);

    /**
     * Sprawdza czy klucz istnieje w cache'u.
     */
    boolean exists(String key);

    /**
     * Sprawdza czy Redis jest dostępny.
     */
    boolean isAvailable();
}
