package wypozyczalnia.repositories;

import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;
import wypozyczalnia.objects.nieruchomosc.Dom;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class NieruchomoscRepozytoriumTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7");

    private MongoDatabase database;
    private NieruchomoscRepozytorium repository;

    @BeforeEach
    void setUp() {
        String connectionString = mongoContainer.getReplicaSetUrl();
        database = com.mongodb.client.MongoClients.create(connectionString)
                .getDatabase("test_db");
        repository = new NieruchomoscRepozytorium(database);
    }

    @AfterEach
    void tearDown() {
        database.drop();
    }

    @Test
    void testDodajMieszkanie() {
        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Centrum", "ul. Testowa 1", 3, "gazowe", true);

        repository.dodaj(mieszkanie);

        assertNotNull(mieszkanie.getId());

        Nieruchomosc znaleziona = repository.znajdz(mieszkanie.getId());
        assertNotNull(znaleziona);
        assertTrue(znaleziona instanceof Mieszkanie);

        Mieszkanie znalezione = (Mieszkanie) znaleziona;
        assertEquals("Kraków", znalezione.getMiasto());
        assertEquals("Centrum", znalezione.getDzielnica());
        assertEquals("ul. Testowa 1", znalezione.getAdres());
        assertEquals(3, znalezione.getLiczbaPokoi());
        assertEquals("gazowe", znalezione.getTypOgrzewania());
        assertTrue(znalezione.isCzyUmeblowane());
    }

    @Test
    void testDodajDom() {
        Dom dom = new Dom("Warszawa", "Mokotów", "ul. Testowa Dom 1", 500, "jednorodzinny", true);

        repository.dodaj(dom);

        assertNotNull(dom.getId());

        Nieruchomosc znaleziona = repository.znajdz(dom.getId());
        assertNotNull(znaleziona);
        assertTrue(znaleziona instanceof Dom);

        Dom znaleziony = (Dom) znaleziona;
        assertEquals("Warszawa", znaleziony.getMiasto());
        assertEquals("Mokotów", znaleziony.getDzielnica());
        assertEquals("ul. Testowa Dom 1", znaleziony.getAdres());
        assertEquals(500, znaleziony.getPowierzchniaDzialki());
        assertEquals("jednorodzinny", znaleziony.getTypBudynku());
        assertTrue(znaleziony.isCzyZOgrodem());
    }

    @Test
    void testZnajdzNieistniejacy() {
        ObjectId nieistniejeId = new ObjectId();

        Nieruchomosc wynik = repository.znajdz(nieistniejeId);

        assertNull(wynik);
    }

    @Test
    void testCzyJestZajetaWolna() {
        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. Wolna 1", 2, "elektryczne", false);
        repository.dodaj(mieszkanie);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime koniec = start.plusDays(7);

        boolean zajeta = repository.czyJestZajeta(mieszkanie, start, koniec);

        assertFalse(zajeta);
    }

    @Test
    void testUsunNieruchomosc() {
        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. DoUsuniecia 1", 1, "gazowe", false);
        repository.dodaj(mieszkanie);
        ObjectId id = mieszkanie.getId();

        repository.usun(mieszkanie);

        Nieruchomosc usunieta = repository.znajdz(id);
        assertNull(usunieta);
    }

    @Test
    void testAktualizujNieruchomosc() {
        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Test", "ul. DoAktualizacji 1", 2, "elektryczne", false);
        repository.dodaj(mieszkanie);

        mieszkanie.setCzyUmeblowane(true);
        mieszkanie.setTypOgrzewania("gazowe");
        repository.aktualizuj(mieszkanie);

        Nieruchomosc zaktualizowana = repository.znajdz(mieszkanie.getId());
        assertNotNull(zaktualizowana);
        assertTrue(zaktualizowana instanceof Mieszkanie);

        Mieszkanie zaktualizowane = (Mieszkanie) zaktualizowana;
        assertTrue(zaktualizowane.isCzyUmeblowane());
        assertEquals("gazowe", zaktualizowane.getTypOgrzewania());
    }
}
