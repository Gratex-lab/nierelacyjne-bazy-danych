package wypozyczalnia.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import wypozyczalnia.cache.RedisCacheManager;
import wypozyczalnia.config.MongoConfig;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.repositories.NajemcaRepozytorium;
import wypozyczalnia.repositories.cache.CachedNajemcaRepozytorium;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Testy wydajnościowe JMH porównujące wydajność: - Odczyt z MongoDB - Odczyt z
 * Redis cache - Inwalidacja cache
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class CachePerformanceBenchmark {

    private NajemcaRepozytorium najemcaRepo;
    private RedisCacheManager cacheManager;
    private Najemca testNajemca;

    private static final String TEST_LOGIN = "benchmark-user";

    @Setup(Level.Trial)
    public void setupTrial() {
        try {
            // Inicjalizuj Redis cache
            cacheManager = new RedisCacheManager();
            if (!cacheManager.isAvailable()) {
                throw new RuntimeException("Redis niedostępny! Uruchom: docker-compose up redis-stack");
            }

            // Inicjalizuj repozytorium z cache (dekorator)
            NajemcaRepozytorium pureMongoRepo = new NajemcaRepozytorium(MongoConfig.getDatabase());
            najemcaRepo = new CachedNajemcaRepozytorium(pureMongoRepo, cacheManager, MongoConfig.getDatabase());

            // Przygotuj testowe dane
            testNajemca = new Najemca(TEST_LOGIN);

            // Usuń ewentualne poprzednie dane
            cleanup();

            // Dodaj do bazy danych
            pureMongoRepo.dodaj(testNajemca);
            System.out.println("Cache dostępny: " + cacheManager.isAvailable());
            System.out.println("Test najemca ID: " + testNajemca.getId());

        } catch (RuntimeException e) {
            throw new RuntimeException("Błąd setup benchmarku: " + e.getMessage(), e);
        }
    }

    @TearDown(Level.Trial)
    public void teardownTrial() {
        cleanup();
        if (cacheManager != null) {
            cacheManager.close();
        }
        MongoConfig.closeConnection();
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        // Wyczyść cache przed każdą iteracją, żeby zapewnić kontrolowane warunki
        clearAllCache();
    }

    /**
     * Odczyt z MongoDB (cache miss) Cache jest pusty, więc dane muszą być
     * pobrane z MongoDB.
     */
    @Benchmark
    public Najemca readFromMongoDB_CacheMiss() {
        // Upewnij się, że cache jest pusty (symulacja cache miss)
        if (cacheManager.isAvailable()) {
            cacheManager.delete("najemca_login:" + TEST_LOGIN);
            cacheManager.delete("najemca:" + testNajemca.getId().toHexString());
        }
        // Odczyt z bazy (będzie cache miss, więc pobierze z MongoDB)
        return najemcaRepo.znajdzLogin(TEST_LOGIN);
    }

    /**
     * Odczyt z Redis cache (cache hit) Dane są już w cache, więc pobierze z
     * Redis.
     */
    @Benchmark
    public Najemca readFromRedis_CacheHit() {
        // Upewnij się, że dane są w cache (załaduj jeśli nie ma)
        String cacheKey = "najemca_login:" + TEST_LOGIN;
        if (!cacheManager.exists(cacheKey)) {
            cacheManager.put(cacheKey, testNajemca, Duration.ofMinutes(15));
        }
        // Odczyt z cache (będzie cache hit)
        return najemcaRepo.znajdzLogin(TEST_LOGIN);
    }

    /**
     * Inwalidacja cache Mierzy czas usunięcia danych z cache.
     */
    @Benchmark
    public void invalidateCache() {
        // Upewnij się, że dane są w cache
        String cacheKey = "najemca_login:" + TEST_LOGIN;
        if (!cacheManager.exists(cacheKey)) {
            cacheManager.put(cacheKey, testNajemca, Duration.ofMinutes(15));
        }
        // Inwalidacja cache
        cacheManager.delete(cacheKey);
    }

    /**
     * Pełny cykl write-read Zapisuje do cache i od razu odczytuje.
     */
    @Benchmark
    public Najemca writeAndReadCache() {
        String cacheKey = "najemca_login:write-read-" + System.nanoTime();
        // Write
        cacheManager.put(cacheKey, testNajemca, Duration.ofMinutes(15));
        // Read
        return cacheManager.get(cacheKey, Najemca.class);
    }

    /**
     * MongoDB bez cache Mierzy czystą wydajność MongoDB (cache wyłączony).
     */
    @Benchmark
    public Najemca readPureMongoDb() {
        // Utwórz repozytorium bez cache
        NajemcaRepozytorium pureMongoRepo = new NajemcaRepozytorium(MongoConfig.getDatabase());
        return pureMongoRepo.znajdzLogin(TEST_LOGIN);
    }

    private void cleanup() {
        try {
            if (najemcaRepo != null && testNajemca != null) {
                // Usuń testowe dane
                Najemca existing = najemcaRepo.znajdzLogin(TEST_LOGIN);
                if (existing != null) {
                    najemcaRepo.usun(existing);
                }
            }
            clearAllCache();
        } catch (Exception e) {
            System.err.println("Błąd cleanup: " + e.getMessage());
        }
    }

    private void clearAllCache() {
        if (cacheManager != null) {
            cacheManager.deleteByPrefix("najemca");
            cacheManager.deleteByPrefix("najemca_login");
        }
    }

    /**
     * Uruchomienie benchmarku z szczegółowymi opcjami.
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CachePerformanceBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.TEXT)
                .result("cache-performance-results.txt")
                .build();

        new Runner(opt).run();
    }
}
