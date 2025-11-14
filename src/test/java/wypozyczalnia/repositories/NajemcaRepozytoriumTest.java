package wypozyczalnia.repositories;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import wypozyczalnia.objects.Najemca;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class NajemcaRepozytoriumTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0.1");

    private MongoDatabase database;
    private NajemcaRepozytorium repository;

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
        database = mongoClient.getDatabase("test_db");
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
        assertTrue(znaleziony.isActive());
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

        najemca.setActive(false);
        repository.aktualizuj(najemca);

        Najemca zaktualizowany = repository.znajdz(najemca.getId());
        assertNotNull(zaktualizowany);
        assertFalse(zaktualizowany.isActive());
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
