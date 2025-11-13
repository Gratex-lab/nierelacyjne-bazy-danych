package wypozyczalnia.repositories;

import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import wypozyczalnia.objects.Najemca;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class NajemcaRepozytoriumTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7");

    private MongoDatabase database;
    private NajemcaRepozytorium repository;

    @BeforeEach
    void setUp() {
        String connectionString = mongoContainer.getReplicaSetUrl();
        database = com.mongodb.client.MongoClients.create(connectionString)
                .getDatabase("test_db");
        repository = new NajemcaRepozytorium(database);
    }

    @AfterEach
    void tearDown() {
        database.drop();
    }

    @Test
    void testDodajNajemce() {
        Najemca najemca = new Najemca("testLogin");

        repository.dodaj(najemca);

        assertNotNull(najemca.getId());

        Najemca znaleziony = repository.znajdz(najemca.getId());
        assertNotNull(znaleziony);
        assertEquals("testLogin", znaleziony.getLogin());
        assertTrue(znaleziony.czyAktywny());
    }

    @Test
    void testZnajdzNieistniejacy() {
        ObjectId nieistniejeId = new ObjectId();

        Najemca wynik = repository.znajdz(nieistniejeId);

        assertNull(wynik);
    }

    @Test
    void testZnajdzPoLoginie() {
        Najemca najemca = new Najemca("unikatowy_login");
        repository.dodaj(najemca);

        Najemca znaleziony = repository.znajdzLogin("unikatowy_login");

        assertNotNull(znaleziony);
        assertEquals("unikatowy_login", znaleziony.getLogin());
        assertEquals(najemca.getId(), znaleziony.getId());
    }

    @Test
    void testZnajdzPoLogininieistniejacym() {
        Najemca wynik = repository.znajdzLogin("nieistniejacy_login");

        assertNull(wynik);
    }

    @Test
    void testAktualizujNajemce() {
        Najemca najemca = new Najemca("login_do_aktualizacji");
        repository.dodaj(najemca);

        najemca.setAktywny(false);
        repository.aktualizuj(najemca);

        Najemca zaktualizowany = repository.znajdz(najemca.getId());
        assertNotNull(zaktualizowany);
        assertFalse(zaktualizowany.czyAktywny());
    }

    @Test
    void testUsunNajemce() {

        Najemca najemca = new Najemca("login_do_usuniecia");
        repository.dodaj(najemca);
        ObjectId id = najemca.getId();

        repository.usun(najemca);

        Najemca usuniety = repository.znajdz(id);
        assertNull(usuniety);
    }
}
