package wypozyczalnia.integration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import wypozyczalnia.managers.NajemManager;
import wypozyczalnia.managers.NajemcaManager;
import wypozyczalnia.managers.NieruchomoscManager;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class IntegrationTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0.1");

    private MongoDatabase database;
    private NajemcaManager najemcaManager;
    private NieruchomoscManager nieruchomoscManager;
    private NajemManager najemManager;

    @BeforeEach
    void setUp() {
        String connectionString = mongoContainer.getReplicaSetUrl();

        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .build();

        MongoClient mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase("integration_test_db");

        najemcaManager = new NajemcaManager(database);
        nieruchomoscManager = new NieruchomoscManager(database);
        najemManager = new NajemManager(database, najemcaManager, nieruchomoscManager);
    }

    @AfterEach
    void tearDown() {
        database.drop();
    }

    @Test
    void testPelnyScenariusz() {
        Najemca najemca = new Najemca("integrationTest");
        najemcaManager.dodajNajemce(najemca);

        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. Integration 1", 3, "gazowe", true, 0L);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime koniec = start.plusDays(7).withNano(0);

        assertDoesNotThrow(() -> {
            najemManager.dokonajNajmu(najemca, mieszkanie, start, koniec);
        });

        boolean zajeta = nieruchomoscManager.czyJestZajeta(mieszkanie, start.plusDays(1), koniec.minusDays(1));
        assertTrue(zajeta);

        assertThrows(Exception.class, () -> {
            najemManager.dokonajNajmu(najemca, mieszkanie, start.plusDays(1), koniec.plusDays(1));
        });

        assertDoesNotThrow(() -> {
            najemManager.zwrocNieruchomosc(najemca, mieszkanie);
        });

        boolean zajetaPoZwrocie = nieruchomoscManager.czyJestZajeta(mieszkanie, start.plusDays(1), koniec.minusDays(1));
        assertFalse(zajetaPoZwrocie);
    }

    @Test
    void testLimitNajmow() {
        Najemca najemca = new Najemca("limitTest");
        najemcaManager.dodajNajemce(najemca);

        Mieszkanie mieszkanie1 = new Mieszkanie("Kraków", "Test", "ul. Limit 1", 2, "gazowe", false, 0L);
        Mieszkanie mieszkanie2 = new Mieszkanie("Kraków", "Test", "ul. Limit 2", 2, "elektryczne", false, 0L);
        Mieszkanie mieszkanie3 = new Mieszkanie("Kraków", "Test", "ul. Limit 3", 2, "centralne", false, 0L);
        Mieszkanie mieszkanie4 = new Mieszkanie("Kraków", "Test", "ul. Limit 4", 2, "gazowe", false, 0L);

        nieruchomoscManager.dodajNieruchomosc(mieszkanie1);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie2);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie3);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie4);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime koniec = start.plusDays(7).withNano(0);

        assertDoesNotThrow(() -> {
            najemManager.dokonajNajmu(najemca, mieszkanie1, start, koniec);
            najemManager.dokonajNajmu(najemca, mieszkanie2, start, koniec);
            najemManager.dokonajNajmu(najemca, mieszkanie3, start, koniec);
        });

        assertThrows(Exception.class, () -> {
            najemManager.dokonajNajmu(najemca, mieszkanie4, start, koniec);
        });

        assertDoesNotThrow(() -> {
            najemManager.zwrocNieruchomosc(najemca, mieszkanie1);
        });
        assertDoesNotThrow(() -> {
            najemManager.dokonajNajmu(najemca, mieszkanie4, start, koniec);
        });
    }

    @Test
    void testNieaktywnyNajemca() {
        Najemca najemca = new Najemca("nieaktywnyTest");
        najemca.setActive(false);
        najemcaManager.dodajNajemce(najemca);

        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. Nieaktywny 1", 2, "gazowe", false, 0L);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime koniec = start.plusDays(7).withNano(0);

        assertThrows(Exception.class, () -> {
            najemManager.dokonajNajmu(najemca, mieszkanie, start, koniec);
        });
    }

    @Test
    void testValidacjaTerminow() {
        Najemca najemca = new Najemca("terminTest");
        najemcaManager.dodajNajemce(najemca);

        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. Termin 1", 2, "gazowe", false, 0L);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie);

        LocalDateTime start = LocalDateTime.now().plusDays(7).withNano(0);
        LocalDateTime koniec = LocalDateTime.now().plusDays(1).withNano(0);

        assertThrows(Exception.class, () -> {
            najemManager.dokonajNajmu(najemca, mieszkanie, start, koniec);
        });
    }
}
