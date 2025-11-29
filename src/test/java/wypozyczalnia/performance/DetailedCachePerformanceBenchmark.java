package wypozyczalnia.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import wypozyczalnia.cache.RedisCacheManager;
import wypozyczalnia.config.MongoConfig;
import wypozyczalnia.objects.nieruchomosc.Dom;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;
import wypozyczalnia.repositories.NieruchomoscRepozytorium;
import wypozyczalnia.repositories.cache.CachedNieruchomoscRepozytorium;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Szczegółowe testy wydajnościowe dla różnych scenariuszy cache'owania.
 * Porównuje wydajność operacji z cache vs bez cache.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 2)
public class DetailedCachePerformanceBenchmark {

    private NieruchomoscRepozytorium nieruchomoscRepo;
    private RedisCacheManager cacheManager;
    private List<Nieruchomosc> testNieruchomosci;
    private LocalDateTime testStart, testEnd;

    @Setup(Level.Trial)
    public void setupTrial() {
        try {
            cacheManager = new RedisCacheManager();
            if (!cacheManager.isAvailable()) {
                throw new RuntimeException("Redis niedostępny! Uruchom: docker-compose up redis-stack");
            }

            // Tworzenie repozytorium z cache bezpośrednio przez dekorator
            NieruchomoscRepozytorium pureMongoRepo = new NieruchomoscRepozytorium(MongoConfig.getDatabase());
            nieruchomoscRepo = new CachedNieruchomoscRepozytorium(pureMongoRepo, cacheManager, MongoConfig.getDatabase());

            // Przygotuj testowe dane
            setupTestData();

            // Okresy testowe dla sprawdzania dostępności
            testStart = LocalDateTime.now().plusDays(10);
            testEnd = LocalDateTime.now().plusDays(17);
            System.out.println("Liczba testowych nieruchomości: " + testNieruchomosci.size());
            System.out.println("Cache dostępny: " + cacheManager.isAvailable());

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
        // Wyczyść cache przed każdą iteracją
        clearAllCache();
    }

    /**
     * Pojedynczy odczyt nieruchomości z MongoDB
     */
    @Benchmark
    public Nieruchomosc singleReadMongoDB() {
        // Usuń z cache, żeby wymusić odczyt z MongoDB
        Nieruchomosc first = testNieruchomosci.get(0);
        String cacheKey = "nieruchomosc:" + first.getId().toHexString();
        cacheManager.delete(cacheKey);

        return nieruchomoscRepo.znajdz(first.getId());
    }

    /**
     * odczyt nieruchomości z Redis cache
     */
    @Benchmark
    public Nieruchomosc singleReadCache() {
        Nieruchomosc first = testNieruchomosci.get(0);
        String cacheKey = "nieruchomosc:" + first.getId().toHexString();

        // Upewnij się, że dane są w cache
        if (!cacheManager.exists(cacheKey)) {
            cacheManager.put(cacheKey, first, Duration.ofMinutes(10));
        }

        return nieruchomoscRepo.znajdz(first.getId());
    }

    /**
     * Sprawdzenie dostępności z MongoDB
     */
    @Benchmark
    public boolean checkAvailabilityMongoDB() {
        Nieruchomosc nieruchomosc = testNieruchomosci.get(0);

        // Wyczyść cache dostępności
        String period = testStart.toString() + "_" + testEnd.toString();
        String cacheKey = "dostepnosc:" + nieruchomosc.getId().toHexString() + ":" + period;
        cacheManager.delete(cacheKey);

        return nieruchomoscRepo.czyJestZajeta(nieruchomosc, testStart, testEnd);
    }

    /**
     * Sprawdzenie dostępności z Redis cache
     */
    @Benchmark
    public boolean checkAvailabilityCache() {
        Nieruchomosc nieruchomosc = testNieruchomosci.get(0);
        String period = testStart.toString() + "_" + testEnd.toString();
        String cacheKey = "dostepnosc:" + nieruchomosc.getId().toHexString() + ":" + period;

        // Upewnij się, że wynik jest w cache
        if (!cacheManager.exists(cacheKey)) {
            cacheManager.put(cacheKey, false, Duration.ofMinutes(2));
        }

        return nieruchomoscRepo.czyJestZajeta(nieruchomosc, testStart, testEnd);
    }

    /**
     * Wsadowy odczyt z MongoDB (5 nieruchomości)
     */
    @Benchmark
    public List<Nieruchomosc> batchReadMongoDB() {
        List<Nieruchomosc> results = new ArrayList<>();

        // Wyczyść cache dla wszystkich
        for (int i = 0; i < Math.min(5, testNieruchomosci.size()); i++) {
            Nieruchomosc n = testNieruchomosci.get(i);
            String cacheKey = "nieruchomosc:" + n.getId().toHexString();
            cacheManager.delete(cacheKey);
        }

        // Odczytaj z MongoDB
        for (int i = 0; i < Math.min(5, testNieruchomosci.size()); i++) {
            Nieruchomosc n = testNieruchomosci.get(i);
            results.add(nieruchomoscRepo.znajdz(n.getId()));
        }

        return results;
    }

    /**
     * Wsadowy odczyt z cache (5 nieruchomości)
     */
    @Benchmark
    public List<Nieruchomosc> batchReadCache() {
        List<Nieruchomosc> results = new ArrayList<>();

        // Upewnij się, że wszystkie są w cache
        for (int i = 0; i < Math.min(5, testNieruchomosci.size()); i++) {
            Nieruchomosc n = testNieruchomosci.get(i);
            String cacheKey = "nieruchomosc:" + n.getId().toHexString();
            if (!cacheManager.exists(cacheKey)) {
                cacheManager.put(cacheKey, n, Duration.ofMinutes(10));
            }
        }

        // Odczytaj z cache
        for (int i = 0; i < Math.min(5, testNieruchomosci.size()); i++) {
            Nieruchomosc n = testNieruchomosci.get(i);
            results.add(nieruchomoscRepo.znajdz(n.getId()));
        }

        return results;
    }

    /**
     * Operacja write do Redis cache
     */
    @Benchmark
    public void cacheWrite() {
        Nieruchomosc nieruchomosc = testNieruchomosci.get(0);
        String cacheKey = "nieruchomosc:write-test:" + System.nanoTime();

        cacheManager.put(cacheKey, nieruchomosc, Duration.ofMinutes(10));
    }

    /**
     * Inwalidacja cache (pojedynczy klucz)
     */
    @Benchmark
    public void singleInvalidation() {
        String cacheKey = "nieruchomosc:invalidate:" + System.nanoTime();

        // Dodaj do cache
        cacheManager.put(cacheKey, testNieruchomosci.get(0), Duration.ofMinutes(10));

        // Inwaliduj
        cacheManager.delete(cacheKey);
    }

    /**
     * Inwalidacja cache (prefix - wiele kluczy)
     */
    @Benchmark
    public void prefixInvalidation() {
        String prefix = "nieruchomosc:prefix-test:" + System.nanoTime();

        // Dodaj kilka kluczy z tym prefiksem
        for (int i = 0; i < 5; i++) {
            String key = prefix + ":" + i;
            cacheManager.put(key, testNieruchomosci.get(0), Duration.ofMinutes(10));
        }

        // Inwaliduj wszystkie z prefiksem
        cacheManager.deleteByPrefix(prefix);
    }

    private void setupTestData() {
        testNieruchomosci = new ArrayList<>();

        // Usuń ewentualne poprzednie dane
        cleanup();

        // Utwórz 10 testowych nieruchomości
        for (int i = 0; i < 10; i++) {
            Dom dom = new Dom(
                    "TestMiasto",
                    "TestDzielnica",
                    "ul. Testowa " + i,
                    100 + i * 10,
                    "testowy",
                    i % 2 == 0,
                    1L
            );

            nieruchomoscRepo.dodaj(dom);
            testNieruchomosci.add(dom);
        }

        System.out.println("Utworzono " + testNieruchomosci.size() + " testowych nieruchomości");
    }

    private void cleanup() {
        try {
            if (testNieruchomosci != null) {
                for (Nieruchomosc n : testNieruchomosci) {
                    try {
                        nieruchomoscRepo.usun(n);
                    } catch (Exception e) {
                    }
                }
            }
            clearAllCache();
        } catch (Exception e) {
            System.err.println("Błąd cleanup: " + e.getMessage());
        }
    }

    private void clearAllCache() {
        if (cacheManager != null) {
            cacheManager.deleteByPrefix("nieruchomosc");
            cacheManager.deleteByPrefix("dostepnosc");
        }
    }

    /**
     * Uruchomienie szczegółowego benchmarku.
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DetailedCachePerformanceBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.TEXT)
                .result("detailed-cache-performance-results.txt")
                .build();

        new Runner(opt).run();
    }
}
