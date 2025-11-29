package wypozyczalnia.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;

import org.opentest4j.TestAbortedException;

/**
 * Testy jednostkowe dla RedisCacheManager. Testuje operacje dodawania i
 * odczytywania danych z Redis.
 */
public class RedisCacheManagerTest {

    private RedisCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        try {
            cacheManager = new RedisCacheManager();
            // Sprawdź czy Redis jest dostępny
            Assumptions.assumeTrue(cacheManager.isAvailable(),
                    "Redis niedostępny. Uruchom: docker-compose up redis-stack");
        } catch (TestAbortedException e) {
            Assumptions.assumeTrue(false, "Nie można połączyć z Redis: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        if (cacheManager != null && cacheManager.isAvailable()) {
            // Wyczyść testowe klucze
            cacheManager.deleteByPrefix("test:");
            cacheManager.close();
        }
    }

    @Test
    @DisplayName("Test dodawania danych do Redis")
    void shouldAddDataToRedis() {
        String key = "test:add-data";
        TestObject testData = new TestObject("testValue", 42, LocalDateTime.now());
        Duration ttl = Duration.ofMinutes(5);
        cacheManager.put(key, testData, ttl);
        assertTrue(cacheManager.exists(key), "Klucz powinien istnieć w Redis");

        TestObject retrieved = cacheManager.get(key, TestObject.class);
        assertNotNull(retrieved, "Dane powinny być pobrane z Redis");
        assertEquals("testValue", retrieved.name);
        assertEquals(42, retrieved.value);
        assertNotNull(retrieved.timestamp);
    }

    @Test
    @DisplayName("Test odczytywania danych z Redis")
    void shouldReadDataFromRedis() {
        String key = "test:read-data";
        TestObject originalData = new TestObject("readValue", 123, LocalDateTime.now().minusHours(1));
        cacheManager.put(key, originalData, Duration.ofMinutes(10));

        TestObject readData = cacheManager.get(key, TestObject.class);

        assertNotNull(readData, "Dane powinny zostać odczytane");
        assertEquals(originalData.name, readData.name);
        assertEquals(originalData.value, readData.value);
        assertEquals(originalData.timestamp, readData.timestamp);
    }

    @Test
    @DisplayName("Test dodawania i odczytywania różnych typów danych")
    void shouldHandleComplexDataTypes() {
        // Test String
        String stringKey = "test:string";
        String stringValue = "xyz";
        cacheManager.put(stringKey, stringValue, Duration.ofMinutes(5));
        assertEquals(stringValue, cacheManager.get(stringKey, String.class));

        // Test Integer
        String intKey = "test:integer";
        Integer intValue = 42;
        cacheManager.put(intKey, intValue, Duration.ofMinutes(5));
        assertEquals(intValue, cacheManager.get(intKey, Integer.class));

        // Test Boolean
        String boolKey = "test:boolean";
        Boolean boolValue = true;
        cacheManager.put(boolKey, boolValue, Duration.ofMinutes(5));
        assertEquals(boolValue, cacheManager.get(boolKey, Boolean.class));
    }

    @Test
    @DisplayName("Test TTL - dane powinny wygasnąć")
    void shouldExpireDataAfterTTL() throws InterruptedException {
        String key = "test:ttl";
        TestObject data = new TestObject("expiring", 999, LocalDateTime.now());
        Duration shortTtl = Duration.ofSeconds(2);

        cacheManager.put(key, data, shortTtl);
        assertTrue(cacheManager.exists(key), "Klucz powinien istnieć na początku");

        Thread.sleep(3000);

        assertFalse(cacheManager.exists(key), "Klucz powinien wygasnąć po TTL");
        assertNull(cacheManager.get(key, TestObject.class), "Dane powinny być null po wygaśnięciu");
    }

    @Test
    @DisplayName("Test wydajności dodawania danych")
    void shouldMeasureWritePerformance() {
        int iterations = 1000;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            String key = "test:perf-write:" + i;
            TestObject data = new TestObject("performance", i, LocalDateTime.now());
            cacheManager.put(key, data, Duration.ofMinutes(5));
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        double avgTimePerOp = (double) durationMs / iterations;

        System.out.printf("Wydajność zapisu: %d operacji w %d ms (%.2f ms/op)%n",
                iterations, durationMs, avgTimePerOp);

        // Sprawdź czy wszystkie dane zostały zapisane
        for (int i = 0; i < Math.min(10, iterations); i++) {
            String key = "test:perf-write:" + i;
            assertTrue(cacheManager.exists(key), "Klucz " + key + " powinien istnieć");
        }

        // Podstawowa asercja wydajności (powinno być szybciej niż 5ms na operację)
        assertTrue(avgTimePerOp < 5.0,
                String.format("Zapis powinien być szybszy niż 5ms/op, ale było %.2f ms/op", avgTimePerOp));
    }

    @Test
    @DisplayName("Test wydajności odczytywania danych")
    void shouldMeasureReadPerformance() {
        // Przygotuj dane
        int iterations = 1000;
        String baseKey = "test:perf-read:";

        for (int i = 0; i < iterations; i++) {
            String key = baseKey + i;
            TestObject data = new TestObject("readPerf", i, LocalDateTime.now());
            cacheManager.put(key, data, Duration.ofMinutes(10));
        }

        // Zmierz wydajność odczytu
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            String key = baseKey + i;
            TestObject data = cacheManager.get(key, TestObject.class);
            assertNotNull(data, "Dane powinny być dostępne");
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        double avgTimePerOp = (double) durationMs / iterations;

        System.out.printf("Wydajność odczytu: %d operacji w %d ms (%.2f ms/op)%n",
                iterations, durationMs, avgTimePerOp);

        // Podstawowa asercja wydajności (powinno być szybciej niż 3ms na operację)
        assertTrue(avgTimePerOp < 3.0,
                String.format("Odczyt powinien być szybszy niż 3ms/op, ale było %.2f ms/op", avgTimePerOp));
    }

    @Test
    @DisplayName("Test inwalidacji cache")
    void shouldInvalidateCache() {
        String key = "test:invalidate";
        TestObject data = new TestObject("toInvalidate", 777, LocalDateTime.now());
        cacheManager.put(key, data, Duration.ofMinutes(10));

        assertTrue(cacheManager.exists(key), "Dane powinny być w cache");

        cacheManager.delete(key);

        assertFalse(cacheManager.exists(key), "Dane powinny zostać usunięte");
        assertNull(cacheManager.get(key, TestObject.class), "Odczyt po inwalidacji powinien zwrócić null");
    }

    @Test
    @DisplayName("Test inwalidacji z prefiksem")
    void shouldInvalidateCacheByPrefix() {
        String prefix = "test:prefix-inv";
        for (int i = 0; i < 5; i++) {
            String key = prefix + ":" + i;
            TestObject data = new TestObject("prefix" + i, i, LocalDateTime.now());
            cacheManager.put(key, data, Duration.ofMinutes(10));
        }

        // Dodaj klucz z innym prefiksem (nie powinien zostać usunięty)
        String otherKey = "test:other:1";
        TestObject otherData = new TestObject("other", 999, LocalDateTime.now());
        cacheManager.put(otherKey, otherData, Duration.ofMinutes(10));
        cacheManager.deleteByPrefix(prefix);
        for (int i = 0; i < 5; i++) {
            String key = prefix + ":" + i;
            assertFalse(cacheManager.exists(key), "Klucz " + key + " powinien zostać usunięty");
        }

        assertTrue(cacheManager.exists(otherKey), "Klucz z innym prefiksem powinien zostać");
    }

    /**
     * Klasa testowa do serializacji
     */
    public static class TestObject {

        public String name;
        public int value;
        public LocalDateTime timestamp;

        public TestObject() {
            // Konstruktor bezargumentowy
        }

        public TestObject(String name, int value, LocalDateTime timestamp) {
            this.name = name;
            this.value = value;
            this.timestamp = timestamp;
        }

        // Gettery i settery
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("TestObject{name='%s', value=%d, timestamp=%s}", name, value, timestamp);
        }
    }
}
