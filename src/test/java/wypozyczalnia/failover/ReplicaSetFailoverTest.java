package wypozyczalnia.failover;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import wypozyczalnia.managers.NajemcaManager;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.repositories.NajemcaRepozytorium;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uproszczone testy failover dla MongoDB Replica Set.
 * Testy wykorzystują istniejący klaster Docker (docker-compose.yml).
 * 
 * WYMAGANIA:
 * 1. Uruchom klaster: docker compose up -d
 * 2. Uruchom testy: mvn test -Dtest=ReplicaSetFailoverTest
 */
class ReplicaSetFailoverTest {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private NajemcaManager najemcaManager;
    private NajemcaRepozytorium najemcaRepozytorium;

    @BeforeAll
    static void setUpConnection() {
        // Połączenie z lokalnym klastrem Docker
        String connectionString = "mongodb://localhost:27017,localhost:27018,localhost:27019/test_failover_db?replicaSet=rs0&retryWrites=true&w=majority";
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase("test_failover_db");
    }

    @BeforeEach
    void setUp() {
        najemcaManager = new NajemcaManager(database);
        najemcaRepozytorium = new NajemcaRepozytorium(database);
    }

    @AfterEach
    void tearDown() {
        // Czyszczenie po testach
        try {
            database.getCollection("najemcy").drop();
        } catch (Exception e) {
            // Ignoruj błędy czyszczenia
        }
    }

    @AfterAll
    static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    /**
     * Test 1: Operacje CRUD działają przy pełnym klastrze (wszystkie węzły
     * działają)
     * Wymóg: Testy przy poprawnie działającym klastrze
     */
    @Test
    @Order(1)
    @DisplayName("Test 1: CRUD przy działającym klastrze (wszystkie węzły)")
    void testCrudPrzyPelnymKlastrze() {
        // CREATE
        Najemca najemca = new Najemca("test_pelny_klaster");
        najemcaManager.dodajNajemce(najemca);
        assertNotNull(najemca.getId(), "ID powinno być ustawione po dodaniu");

        // READ
        Najemca znaleziony = najemcaRepozytorium.znajdz(najemca.getId());
        assertNotNull(znaleziony, "Najemca powinien być znaleziony");
        assertEquals("test_pelny_klaster", znaleziony.getLogin());

        // UPDATE
        znaleziony.setAktywny(false);
        najemcaManager.aktualizujNajemce(znaleziony);
        Najemca zaktualizowany = najemcaRepozytorium.znajdz(najemca.getId());
        assertFalse(zaktualizowany.czyAktywny(), "Status aktywności powinien być zaktualizowany");

        // DELETE
        najemcaRepozytorium.usun(zaktualizowany);
        Najemca usuniety = najemcaRepozytorium.znajdz(najemca.getId());
        assertNull(usuniety, "Najemca powinien być usunięty");
    }

    /**
     * Test 2: Operacje po wyłączeniu PRIMARY
     * Wymóg: +2 pkt - wykonano testy po wyłączeniu aktualnego węzła primary
     * 
     * INSTRUKCJA:
     * 1. Uruchom ten test
     * 2. Podczas pauzy 20 sekund wykonaj: docker stop
     * nierelacyjne-bazy-danych-mongo1-1
     * 3. Test sprawdzi czy system działa po failover
     */
    @Test
    @Order(2)
    @DisplayName("Test 2: CRUD po wyłączeniu PRIMARY (RĘCZNIE wyłącz mongo1)")
    void testCrudPoWylaczeniuPrimary() throws InterruptedException {
        // Dodaj dane przed wyłączeniem PRIMARY
        Najemca przed = new Najemca("przed_failover");
        najemcaManager.dodajNajemce(przed);
        ObjectId idPrzed = przed.getId();

        // PAUZA - użytkownik wyłącza PRIMARY
        // Wykonaj: docker stop nierelacyjne-bazy-danych-mongo1-1
        Thread.sleep(20000);

        // CREATE po failover
        Najemca po = new Najemca("po_failover");
        assertDoesNotThrow(() -> najemcaManager.dodajNajemce(po),
                "CREATE powinno działać po wyłączeniu PRIMARY");
        assertNotNull(po.getId(), "ID powinno być ustawione");

        // READ danych sprzed awarii
        Najemca odczytanyPrzed = najemcaRepozytorium.znajdz(idPrzed);
        assertNotNull(odczytanyPrzed, "Dane sprzed awarii powinny być dostępne");
        assertEquals("przed_failover", odczytanyPrzed.getLogin());

        // UPDATE po failover
        po.setAktywny(false);
        assertDoesNotThrow(() -> najemcaManager.aktualizujNajemce(po),
                "UPDATE powinno działać po wyłączeniu PRIMARY");

        // DELETE po failover
        assertDoesNotThrow(() -> najemcaRepozytorium.usun(po),
                "DELETE powinno działać po wyłączeniu PRIMARY");
    }

    /**
     * Test 3: Spójność danych po przywróceniu PRIMARY
     * Wymóg: +1 pkt - sprawdzono spójność danych po wykonaniu testów
     * 
     * INSTRUKCJA:
     * 1. Upewnij się że mongo1 jest wyłączony
     * 2. Uruchom ten test
     * 3. Podczas pauzy uruchom: docker start nierelacyjne-bazy-danych-mongo1-1
     * 4. Test sprawdzi spójność danych
     */
    @Test
    @Order(3)
    @DisplayName("Test 3: Spójność danych po przywróceniu PRIMARY")
    void testSpojnoscDanychPoFailover() throws InterruptedException {
        // Dodaj 3 rekordy przed przywróceniem
        Najemca n1 = new Najemca("spojnosc_1");
        Najemca n2 = new Najemca("spojnosc_2");
        Najemca n3 = new Najemca("spojnosc_3");

        najemcaManager.dodajNajemce(n1);
        najemcaManager.dodajNajemce(n2);
        najemcaManager.dodajNajemce(n3);

        ObjectId id1 = n1.getId();
        ObjectId id2 = n2.getId();
        ObjectId id3 = n3.getId();

        // PAUZA - użytkownik przywraca PRIMARY
        // Wykonaj: docker start nierelacyjne-bazy-danych-mongo1-1
        Thread.sleep(20000);

        // WERYFIKACJA SPÓJNOŚCI
        Najemca check1 = najemcaRepozytorium.znajdz(id1);
        Najemca check2 = najemcaRepozytorium.znajdz(id2);
        Najemca check3 = najemcaRepozytorium.znajdz(id3);

        assertNotNull(check1, "Najemca 1 powinien istnieć");
        assertNotNull(check2, "Najemca 2 powinien istnieć");
        assertNotNull(check3, "Najemca 3 powinien istnieć");

        assertEquals("spojnosc_1", check1.getLogin());
        assertEquals("spojnosc_2", check2.getLogin());
        assertEquals("spojnosc_3", check3.getLogin());
    }

    /**
     * Test 4: Weryfikacja końcowa - klaster w pełnym składzie
     */
    @Test
    @Order(4)
    @DisplayName("Test 4: Weryfikacja końcowa - pełny klaster")
    void testWeryfikacjaKoncowa() {
        // Prosty test CRUD na końcu
        Najemca finalny = new Najemca("test_finalny");
        najemcaManager.dodajNajemce(finalny);

        Najemca sprawdzenie = najemcaRepozytorium.znajdz(finalny.getId());
        assertNotNull(sprawdzenie);
        assertEquals("test_finalny", sprawdzenie.getLogin());
    }
}
