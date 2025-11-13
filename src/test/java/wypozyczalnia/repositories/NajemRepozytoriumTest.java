package wypozyczalnia.repositories;

import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import wypozyczalnia.objects.Najem;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class NajemRepozytoriumTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7");

    private MongoDatabase database;
    private NajemRepozytorium najemRepository;
    private NajemcaRepozytorium najemcaRepository;
    private NieruchomoscRepozytorium nieruchomoscRepository;

    @BeforeEach
    void setUp() {
        String connectionString = mongoContainer.getReplicaSetUrl();
        database = com.mongodb.client.MongoClients.create(connectionString)
                .getDatabase("test_db");
        najemRepository = new NajemRepozytorium(database);
        najemcaRepository = new NajemcaRepozytorium(database);
        nieruchomoscRepository = new NieruchomoscRepozytorium(database);
    }

    @AfterEach
    void tearDown() {
        database.drop();
    }

    @Test
    void testDodajNajem() {
        Najemca najemca = new Najemca("testNajemca");
        najemcaRepository.dodaj(najemca);

        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. Testowa 1", 2, "gazowe", false);
        nieruchomoscRepository.dodaj(mieszkanie);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime koniec = start.plusDays(7).withNano(0);

        Najem najem = new Najem(najemca, mieszkanie, start, koniec);

        najemRepository.dodaj(najem);

        assertNotNull(najem.getId());

        Najem znaleziony = najemRepository.znajdz(najem.getId());
        assertNotNull(znaleziony);
        assertEquals(najemca.getId(), znaleziony.getNajemcaId());
        assertEquals(mieszkanie.getId(), znaleziony.getNieruchomoscId());
        assertEquals(start, znaleziony.getDataRozpoczecia());
        assertEquals(koniec, znaleziony.getDataZakonczenia());
    }

    @Test
    void testZnajdzNieistniejacy() {
        ObjectId nieistniejeId = new ObjectId();

        Najem wynik = najemRepository.znajdz(nieistniejeId);

        assertNull(wynik);
    }

    @Test
    void testZnajdzNajemcowi() {
        Najemca najemca = new Najemca("testNajemca");
        najemcaRepository.dodaj(najemca);

        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. Testowa 1", 2, "gazowe", false);
        nieruchomoscRepository.dodaj(mieszkanie);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime koniec = start.plusDays(7).withNano(0);

        Najem najem1 = new Najem(najemca, mieszkanie, start, koniec);
        Najem najem2 = new Najem(najemca, mieszkanie, koniec.plusDays(1).withNano(0), koniec.plusDays(8).withNano(0));

        najemRepository.dodaj(najem1);
        najemRepository.dodaj(najem2);

        List<Najem> najmy = najemRepository.znajdzNajemcowi(mieszkanie.getId(), najemca.getId());

        assertEquals(2, najmy.size());
    }

    @Test
    void testZnajdzNajemcowiPuste() {
        ObjectId randomNieruchomoscId = new ObjectId();
        ObjectId randomNajemcaId = new ObjectId();

        List<Najem> najmy = najemRepository.znajdzNajemcowi(randomNieruchomoscId, randomNajemcaId);

        assertTrue(najmy.isEmpty());
    }

    @Test
    void testUsunNajem() {
        Najemca najemca = new Najemca("testNajemca");
        najemcaRepository.dodaj(najemca);

        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. Testowa 1", 2, "gazowe", false);
        nieruchomoscRepository.dodaj(mieszkanie);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime koniec = start.plusDays(7).withNano(0);

        Najem najem = new Najem(najemca, mieszkanie, start, koniec);
        najemRepository.dodaj(najem);

        ObjectId id = najem.getId();

        najemRepository.usun(najem);

        Najem usuniety = najemRepository.znajdz(id);
        assertNull(usuniety);
    }

    @Test
    void testAktualizujNajem() {
        Najemca najemca = new Najemca("testNajemca");
        najemcaRepository.dodaj(najemca);

        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. Testowa 1", 2, "gazowe", false);
        nieruchomoscRepository.dodaj(mieszkanie);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime koniec = start.plusDays(7).withNano(0);

        Najem najem = new Najem(najemca, mieszkanie, start, koniec);
        najemRepository.dodaj(najem);

        LocalDateTime nowyKoniec = koniec.plusDays(3).withNano(0);
        najem.setDataZakonczenia(nowyKoniec);
        najemRepository.aktualizuj(najem);

        Najem zaktualizowany = najemRepository.znajdz(najem.getId());
        assertNotNull(zaktualizowany);
        assertEquals(nowyKoniec, zaktualizowany.getDataZakonczenia());
    }
}
