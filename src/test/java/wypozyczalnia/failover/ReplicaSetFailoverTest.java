package wypozyczalnia.failover;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import wypozyczalnia.managers.NajemcaManager;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.repositories.NajemcaRepozytorium;

import java.io.IOException;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

class ReplicaSetFailoverTest {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private NajemcaManager najemcaManager;
    private NajemcaRepozytorium najemcaRepozytorium;

    @BeforeAll
    static void setUpConnection() {
        String connectionString = "mongodb://localhost:27017,localhost:27018,localhost:27019/test_failover_db?replicaSet=rs0&retryWrites=true&w=majority";
        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase("test_failover_db");
    }

    @BeforeEach
    void setUp() {
        najemcaManager = new NajemcaManager(database);
        najemcaRepozytorium = new NajemcaRepozytorium(database);
    }

    @AfterEach
    void tearDown() {
        try {
            database.getCollection("najemcy").drop();
        } catch (Exception e) {
        }
    }

    @AfterAll
    static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    private void stopDockerContainer(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "stop", containerName);
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Błąd podczas zatrzymywania kontenera " + containerName + ": " + e.getMessage());
        }
    }

    private void startDockerContainer(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "start", containerName);
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Błąd podczas uruchamiania kontenera " + containerName + ": " + e.getMessage());
        }
    }

    private void waitForClusterStabilization() throws InterruptedException {
        Thread.sleep(15000);
    }

    @Test
    @Order(1)
    void testCrudPrzyPelnymKlastrze() {
        Najemca najemca = new Najemca("test_pelny_klaster");
        najemcaManager.dodajNajemce(najemca);
        assertNotNull(najemca.getId());

        Najemca znaleziony = najemcaRepozytorium.znajdz(najemca.getId());
        assertNotNull(znaleziony);
        assertEquals("test_pelny_klaster", znaleziony.getLogin());

        znaleziony.setActive(false);
        najemcaManager.aktualizujNajemce(znaleziony);
        Najemca zaktualizowany = najemcaRepozytorium.znajdz(najemca.getId());
        assertFalse(zaktualizowany.isActive());

        najemcaRepozytorium.usun(zaktualizowany);
        Najemca usuniety = najemcaRepozytorium.znajdz(najemca.getId());
        assertNull(usuniety);
    }

    @Test
    @Order(2)
    void testCrudPoWylaczeniuPrimary() throws InterruptedException {
        Najemca przed = new Najemca("przed_failover");
        najemcaManager.dodajNajemce(przed);
        ObjectId idPrzed = przed.getId();

        stopDockerContainer("mongo1");
        waitForClusterStabilization();

        Najemca po = new Najemca("po_failover");
        assertDoesNotThrow(() -> najemcaManager.dodajNajemce(po));
        assertNotNull(po.getId());

        Najemca odczytanyPrzed = najemcaRepozytorium.znajdz(idPrzed);
        assertNotNull(odczytanyPrzed);
        assertEquals("przed_failover", odczytanyPrzed.getLogin());

        po.setActive(false);
        assertDoesNotThrow(() -> najemcaManager.aktualizujNajemce(po));
        assertDoesNotThrow(() -> najemcaRepozytorium.usun(po));

        startDockerContainer("mongo1");
        waitForClusterStabilization();
    }

    @Test
    @Order(3)
    void testSpojnoscDanychPoFailover() throws InterruptedException {
        Najemca n1 = new Najemca("spojnosc_1");
        Najemca n2 = new Najemca("spojnosc_2");
        Najemca n3 = new Najemca("spojnosc_3");

        najemcaManager.dodajNajemce(n1);
        najemcaManager.dodajNajemce(n2);
        najemcaManager.dodajNajemce(n3);

        ObjectId id1 = n1.getId();
        ObjectId id2 = n2.getId();
        ObjectId id3 = n3.getId();

        stopDockerContainer("mongo2");
        waitForClusterStabilization();

        Najemca check1 = najemcaRepozytorium.znajdz(id1);
        Najemca check2 = najemcaRepozytorium.znajdz(id2);
        Najemca check3 = najemcaRepozytorium.znajdz(id3);

        assertNotNull(check1);
        assertNotNull(check2);
        assertNotNull(check3);
        assertEquals("spojnosc_1", check1.getLogin());
        assertEquals("spojnosc_2", check2.getLogin());
        assertEquals("spojnosc_3", check3.getLogin());

        startDockerContainer("mongo2");
        waitForClusterStabilization();

        check1 = najemcaRepozytorium.znajdz(id1);
        check2 = najemcaRepozytorium.znajdz(id2);
        check3 = najemcaRepozytorium.znajdz(id3);

        assertNotNull(check1);
        assertNotNull(check2);
        assertNotNull(check3);
    }

    @Test
    @Order(4)
    void testWeryfikacjaKoncowa() {
        Najemca finalny = new Najemca("test_finalny");
        najemcaManager.dodajNajemce(finalny);

        Najemca sprawdzenie = najemcaRepozytorium.znajdz(finalny.getId());
        assertNotNull(sprawdzenie);
        assertEquals("test_finalny", sprawdzenie.getLogin());
    }
}
