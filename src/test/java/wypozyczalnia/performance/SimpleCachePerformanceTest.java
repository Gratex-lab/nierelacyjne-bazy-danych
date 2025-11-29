package wypozyczalnia.performance;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import wypozyczalnia.cache.RedisCacheManager;
import wypozyczalnia.config.MongoConfig;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.repositories.NajemcaRepozytorium;
import wypozyczalnia.repositories.cache.CachedNajemcaRepozytorium;

import java.time.Duration;

/**
 * Proste testy wydajnościowe bez JMH - do szybkiego sprawdzenia różnic.
 * Porównuje wydajność cache hit vs cache miss oraz inwalidacji.
 */
public class SimpleCachePerformanceTest {

    private NajemcaRepozytorium cachedRepo;
    private NajemcaRepozytorium pureMongoRepo;
    private RedisCacheManager cacheManager;
    private Najemca testNajemca;

    private static final String TEST_LOGIN = "perf-test-user";
    private static final int ITERATIONS = 100;

    @BeforeEach
    void setUp() {
        try {
            // Inicjalizuj Redis cache
            cacheManager = new RedisCacheManager();
            Assumptions.assumeTrue(cacheManager.isAvailable(),
                    "Redis niedostępny. Uruchom: docker-compose up redis-stack");

            // Inicjalizuj repozytoria
            pureMongoRepo = new NajemcaRepozytorium(MongoConfig.getDatabase()); // Bez cache
            cachedRepo = new CachedNajemcaRepozytorium(pureMongoRepo, cacheManager, MongoConfig.getDatabase()); // Z cache

            // Przygotuj testowe dane
            cleanup();
            testNajemca = new Najemca(TEST_LOGIN);
            pureMongoRepo.dodaj(testNajemca);
            System.out.println("Test najemca ID: " + testNajemca.getId());
            System.out.println("Cache dostępny: " + cacheManager.isAvailable());

        } catch (Exception e) {
            throw new RuntimeException("Błąd setup: " + e.getMessage(), e);
        }
    }

    @AfterEach
    void tearDown() {
        cleanup();
        if (cacheManager != null) {
            cacheManager.close();
        }
    }

    @Test
    @DisplayName("Porównanie wydajności: MongoDB vs Redis Cache")
    void compareMongoDbVsCachePerformance() {

        // Test 1: Cache Miss (pierwsze wywołanie - dane z MongoDB)
        clearAllCache();

        long mongoStartTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            clearAllCache(); // Każde wywołanie - cache miss
            Najemca result = cachedRepo.znajdzLogin(TEST_LOGIN);
            assertNotNull(result);
        }
        long mongoTotalTime = System.nanoTime() - mongoStartTime;
        double mongoAvgTime = (double) mongoTotalTime / ITERATIONS / 1_000_000; // ms

        // Test 2: Cache Hit (dane z Redis)
        // Pierwsze wywołanie załaduje do cache
        cachedRepo.znajdzLogin(TEST_LOGIN);

        long cacheStartTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Najemca result = cachedRepo.znajdzLogin(TEST_LOGIN);
            assertNotNull(result);
        }
        long cacheTotalTime = System.nanoTime() - cacheStartTime;
        double cacheAvgTime = (double) cacheTotalTime / ITERATIONS / 1_000_000;

        // Test 3: Czysty MongoDB (bez cache w ogóle)
        long pureMongoStartTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Najemca result = pureMongoRepo.znajdzLogin(TEST_LOGIN);
            assertNotNull(result);
        }
        long pureMongoTotalTime = System.nanoTime() - pureMongoStartTime;
        double pureMongoAvgTime = (double) pureMongoTotalTime / ITERATIONS / 1_000_000;

        // Wyniki
        System.out.println("\nWYNIKI TESTÓW WYDAJNOŚCIOWYCH:");
        System.out.printf("MongoDB + Cache Miss:  %.3f ms/op (całkowity: %d ms)%n", mongoAvgTime, mongoTotalTime / 1_000_000);
        System.out.printf("Redis Cache Hit:       %.3f ms/op (całkowity: %d ms)%n", cacheAvgTime, cacheTotalTime / 1_000_000);
        System.out.printf("Czysty MongoDB:        %.3f ms/op (całkowity: %d ms)%n", pureMongoAvgTime, pureMongoTotalTime / 1_000_000);

        double speedupVsMiss = mongoAvgTime / cacheAvgTime;
        double speedupVsPureMongo = pureMongoAvgTime / cacheAvgTime;

        System.out.printf("\nPRZYSPIESZENIE CACHE:%n");
        System.out.printf("Cache vs Cache Miss:   %.1fx szybciej%n", speedupVsMiss);
        System.out.printf("Cache vs Czysty Mongo: %.1fx szybciej%n", speedupVsPureMongo);

        // Dodatkowe opóźnienia
        double cacheOverhead = mongoAvgTime - pureMongoAvgTime;
        if (cacheOverhead > 0) {
            System.out.printf("Overhead cache miss:   +%.3f ms (%.1f%% więcej)%n",
                    cacheOverhead, (cacheOverhead / pureMongoAvgTime) * 100);
        }

        // Asercje
        assertTrue(cacheAvgTime < mongoAvgTime,
                String.format("Cache powinien być szybszy niż cache miss (%.3f vs %.3f ms)",
                        cacheAvgTime, mongoAvgTime));

        assertTrue(speedupVsMiss >= 2.0,
                String.format("Cache powinien być przynajmniej 2x szybszy (było %.1fx)", speedupVsMiss));
    }

    @Test
    @DisplayName("Test wydajności inwalidacji cache")
    void testCacheInvalidationPerformance() {

        // Załaduj dane do cache
        cachedRepo.znajdzLogin(TEST_LOGIN);

        // Test inwalidacji pojedynczego klucza
        long singleInvalidationStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            // Dodaj do cache
            String key = "najemca_login:" + TEST_LOGIN;
            cacheManager.put(key, testNajemca, Duration.ofMinutes(10));

            // Inwaliduj
            cacheManager.delete(key);
        }
        long singleInvalidationTime = System.nanoTime() - singleInvalidationStart;
        double avgSingleInvalidation = (double) singleInvalidationTime / ITERATIONS / 1_000_000;

        // Test inwalidacji z prefiksem (wielu kluczy)
        long prefixInvalidationStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            // Dodaj kilka kluczy z prefiksem
            String prefix = "test-invalidation:" + i;
            for (int j = 0; j < 5; j++) {
                String key = prefix + ":" + j;
                cacheManager.put(key, testNajemca, Duration.ofMinutes(10));
            }

            // Inwaliduj wszystkie z prefiksem
            cacheManager.deleteByPrefix(prefix);
        }
        long prefixInvalidationTime = System.nanoTime() - prefixInvalidationStart;
        double avgPrefixInvalidation = (double) prefixInvalidationTime / ITERATIONS / 1_000_000;

        System.out.println("\nWYNIKI INWALIDACJI:");
        System.out.printf("Pojedynczy klucz:   %.3f ms/op%n", avgSingleInvalidation);
        System.out.printf("Prefix (5 kluczy): %.3f ms/op%n", avgPrefixInvalidation);
        System.out.printf("Overhead prefixu:   %.3f ms (%.1fx więcej)%n",
                avgPrefixInvalidation - avgSingleInvalidation,
                avgPrefixInvalidation / avgSingleInvalidation);

        // Podstawowe asercje wydajności
        assertTrue(avgSingleInvalidation < 6.0,
                String.format("Inwalidacja powinna być szybsza niż 6ms (było %.3f ms)", avgSingleInvalidation));

        assertTrue(avgPrefixInvalidation < 20.0,
                String.format("Inwalidacja prefiksu powinna być szybsza niż 20ms (było %.3f ms)", avgPrefixInvalidation));
    }

    @Test
    @DisplayName("Test overhead'u cache przy różnych obciążeniach")
    void testCacheOverheadUnderLoad() {
        int[] loadSizes = {1, 10, 50, 100};

        for (int loadSize : loadSizes) {
            System.out.printf("\nTest dla %d równoczesnych operacji:%n", loadSize);

            // Cache miss
            clearAllCache();
            long cacheMissStart = System.nanoTime();
            for (int i = 0; i < loadSize; i++) {
                clearAllCache();
                cachedRepo.znajdzLogin(TEST_LOGIN);
            }
            long cacheMissTime = System.nanoTime() - cacheMissStart;

            // Cache hit
            cachedRepo.znajdzLogin(TEST_LOGIN);
            long cacheHitStart = System.nanoTime();
            for (int i = 0; i < loadSize; i++) {
                cachedRepo.znajdzLogin(TEST_LOGIN);
            }
            long cacheHitTime = System.nanoTime() - cacheHitStart;

            long pureMongoStart = System.nanoTime();
            for (int i = 0; i < loadSize; i++) {
                pureMongoRepo.znajdzLogin(TEST_LOGIN);
            }
            long pureMongoTime = System.nanoTime() - pureMongoStart;

            double cacheMissAvg = (double) cacheMissTime / loadSize / 1_000_000;
            double cacheHitAvg = (double) cacheHitTime / loadSize / 1_000_000;
            double pureMongoAvg = (double) pureMongoTime / loadSize / 1_000_000;

            System.out.printf("  Cache Miss: %.3f ms/op%n", cacheMissAvg);
            System.out.printf("  Cache Hit:  %.3f ms/op%n", cacheHitAvg);
            System.out.printf("  Pure Mongo: %.3f ms/op%n", pureMongoAvg);
            System.out.printf("  Speedup:    %.1fx%n", cacheMissAvg / cacheHitAvg);
            System.out.printf("  Overhead:   %.3f ms (%.1f%%)%n",
                    cacheMissAvg - pureMongoAvg,
                    ((cacheMissAvg - pureMongoAvg) / pureMongoAvg) * 100);
        }
    }

    private void cleanup() {
        try {
            if (pureMongoRepo != null && testNajemca != null) {
                Najemca existing = pureMongoRepo.znajdzLogin(TEST_LOGIN);
                if (existing != null) {
                    pureMongoRepo.usun(existing);
                }
            }
            clearAllCache();
        } catch (Exception e) {
            // Ignoruj błędy cleanup
        }
    }

    private void clearAllCache() {
        if (cacheManager != null) {
            cacheManager.deleteByPrefix("najemca");
            cacheManager.deleteByPrefix("najemca_login");
        }
    }
}
