package wypozyczalnia.cache;

import org.bson.types.ObjectId;

/**
 * Generator kluczy dla cache'a. Zapewnia spójne nazewnictwo kluczy w Redis.
 */
public class CacheKeys {

    private static final String SEPARATOR = ":";

    // Prefiksy dla różnych typów danych
    public static final String NAJEMCA_PREFIX = "najemca";
    public static final String NAJEMCA_LOGIN_PREFIX = "najemca_login";
    public static final String NIERUCHOMOSC_PREFIX = "nieruchomosc";
    public static final String DOSTEPNOSC_PREFIX = "dostepnosc";

    /**
     * Klucz dla najemcy po ID
     */
    public static String najemcaById(ObjectId id) {
        return NAJEMCA_PREFIX + SEPARATOR + id.toHexString();
    }

    /**
     * Klucz dla najemcy po loginie
     */
    public static String najemcaByLogin(String login) {
        return NAJEMCA_LOGIN_PREFIX + SEPARATOR + login;
    }

    /**
     * Klucz dla nieruchomości po ID
     */
    public static String nieruchomoscById(ObjectId id) {
        return NIERUCHOMOSC_PREFIX + SEPARATOR + id.toHexString();
    }

    /**
     * Klucz dla dostępności nieruchomości
     */
    public static String dostepnosc(ObjectId nieruchomoscId, String period) {
        return DOSTEPNOSC_PREFIX + SEPARATOR + nieruchomoscId.toHexString() + SEPARATOR + period;
    }

    /**
     * Prefiks dla wszystkich kluczy najemców
     */
    public static String allNajemcyPrefix() {
        return NAJEMCA_PREFIX + SEPARATOR;
    }

    /**
     * Prefiks dla wszystkich kluczy nieruchomości
     */
    public static String allNieruchomosciPrefix() {
        return NIERUCHOMOSC_PREFIX + SEPARATOR;
    }

    /**
     * Prefiks dla dostępności konkretnej nieruchomości
     */
    public static String dostepnoscPrefix(ObjectId nieruchomoscId) {
        return DOSTEPNOSC_PREFIX + SEPARATOR + nieruchomoscId.toHexString();
    }
}
