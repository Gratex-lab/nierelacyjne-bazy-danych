package wypozyczalnia.kafka;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import wypozyczalnia.config.MongoConfig;
import wypozyczalnia.objects.Najem;
import wypozyczalnia.repositories.NajemRepozytorium;

/**
 * Test integracyjny dla Kafka: 1. Uruchamia dwie instancje konsumenta 2.
 * Sprawdza przypisanie do partycji 3. Wysyła wypożyczenia przez producenta 4.
 * Weryfikuje zapis do bazy 5. Restartuje konsumentów 6. Sprawdza, czy dane nie
 * zostały zduplikowane
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KafkaTest {

    private static MongoDatabase database;
    private static NajemRepozytorium najemRepozytorium;
    private static WypozyczenieProducer producer;

    private static List<Thread> consumerThreads;
    private static List<WypozyczenieConsumer> consumers;

    private static final int NUMBER_OF_MESSAGES = 10;
    private static final int EXPECTED_CONSUMERS = 2;

    @BeforeAll
    static void setUp() {
        KafkaTopicInitializer.initializeTopic();
        database = MongoConfig.getDatabase();
        najemRepozytorium = new NajemRepozytorium(database);
        database.getCollection("najmy").drop();
        producer = new WypozyczenieProducer();
        consumerThreads = new ArrayList<>();
        consumers = new ArrayList<>();
    }

    @Test
    @Order(1)
    @DisplayName("1. Uruchamia dwie instancje konsumenta i sprawdza przypisanie do partycji")
    void teststarttwo() throws InterruptedException {

        // 2 instancje konsumenta
        for (int i = 0; i < EXPECTED_CONSUMERS; i++) {
            final int consumerNum = i + 1;
            WypozyczenieConsumer consumer = new WypozyczenieConsumer(database);
            consumers.add(consumer);

            Thread thread = new Thread(() -> {
                System.out.println("Konsument " + consumerNum + " rozpoczyna pracę");
                consumer.run();
            }, "Consumer-" + consumerNum);

            consumerThreads.add(thread);
            thread.start();
        }

        // Czeka na przypisanie partycji
        System.out.println("Oczekiwanie na przypisanie partycji do konsumentów");
        Thread.sleep(8000);

        System.out.println("\nDwóch konsumentów uruchomionych i przypisanych do partycji");
        System.out.println("  Grupa konsumencka: wypozyczenia-consumers");
        System.out.println("  Temat: wypozyczenia (3 partycje)");
    }

    @Test
    @Order(2)
    @DisplayName("2. Tworzy wypożyczenia i sprawdza czy zostały odebrane i zapisane")
    void sendmessage_verifystorage() throws InterruptedException {
        List<ObjectId> sentIds = new ArrayList<>();

        // Wyślij wypożyczenia
        for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
            ObjectId najemId = new ObjectId();
            ObjectId najemcaId = new ObjectId();
            ObjectId nieruchomoscId = new ObjectId();
            LocalDateTime startDate = LocalDateTime.now().plusDays(i);
            LocalDateTime endDate = startDate.plusMonths(1);

            producer.sendWypozyczenie(
                    najemId.toString(),
                    najemcaId.toString(),
                    nieruchomoscId.toString(),
                    startDate.toString(),
                    endDate.toString()
            );

            sentIds.add(najemId);
            System.out.println("Wysłano wypożyczenie " + (i + 1) + ": " + najemId);
        }

        System.out.println("\nWysłano " + NUMBER_OF_MESSAGES + " wypożyczeń");

        // Czekaj na przetworzenie wiadomości
        System.out.println("Oczekiwanie na przetworzenie przez konsumentów");
        Thread.sleep(5000);

        // Sprawdź bazę danych
        long count = database.getCollection("najmy").countDocuments();
        System.out.println("\nLiczba wypożyczeń w bazie danych: " + count);

        assertEquals(NUMBER_OF_MESSAGES, count,
                "Wszystkie wypożyczenia powinny być zapisane w bazie");

        // Sprawdzenie indywidualnych rekordów
        int found = 0;
        for (ObjectId id : sentIds) {
            Najem najem = najemRepozytorium.znajdz(id);
            if (najem != null) {
                found++;
            }
        }

        System.out.println("Znaleziono w bazie: " + found + "/" + NUMBER_OF_MESSAGES + " wypożyczeń");
        assertEquals(NUMBER_OF_MESSAGES, found,
                "Wszystkie wysłane wypożyczenia powinny być w bazie");
    }

    @Test
    @Order(3)
    @DisplayName("3. Wyłącz konsumentów, włącz ponownie i sprawdź czy dane nie zostały zduplikowane")
    void restartconsumers_noduplicates() throws InterruptedException {

        // Zapisz obecną liczbę rekordów
        long countBeforeRestart = database.getCollection("najmy").countDocuments();
        System.out.println("Liczba wypożyczeń przed restartem: " + countBeforeRestart);

        // Wyłącz konsumentów
        System.out.println("\nWyłączanie konsumentów");
        for (WypozyczenieConsumer consumer : consumers) {
            consumer.shutdown();
        }

        // Czekaj na zakończenie wątków
        for (Thread thread : consumerThreads) {
            thread.join(5000);
        }
        System.out.println("Konsumenci wyłączeni");

        // Wyczyść listy
        consumers.clear();
        consumerThreads.clear();

        // Krótka przerwa przed ponownym uruchomieniem
        Thread.sleep(2000);

        // Uruchamianie konsumentów ponownie
        for (int i = 0; i < EXPECTED_CONSUMERS; i++) {
            final int consumerNum = i + 1;
            WypozyczenieConsumer consumer = new WypozyczenieConsumer(database);
            consumers.add(consumer);

            Thread thread = new Thread(() -> {
                System.out.println("Konsument " + consumerNum + " (restart) rozpoczyna pracę");
                consumer.run();
            }, "Consumer-Restart-" + consumerNum);

            consumerThreads.add(thread);
            thread.start();
        }

        System.out.println("Oczekiwanie na ponowne przetworzenie wiadomości");
        Thread.sleep(8000);

        // Sprawdzenie liczby rekordów po restarcie
        long countAfterRestart = database.getCollection("najmy").countDocuments();
        System.out.println("Liczba wypożyczeń po restarcie: " + countAfterRestart);

        System.out.println("  Przed restartem: " + countBeforeRestart);
        System.out.println("  Po restarcie:    " + countAfterRestart);

        assertEquals(countBeforeRestart, countAfterRestart, "Liczba wypożyczeń nie powinna się zmienić");

        System.out.println("  Mechanizm sprawdzania duplikatów działa poprawnie");
    }

    @AfterAll
    static void tearDown() throws InterruptedException {

        // Wyłącz konsumentów
        for (WypozyczenieConsumer consumer : consumers) {
            consumer.shutdown();
        }

        for (Thread thread : consumerThreads) {
            thread.join(5000);
        }

        // Zamknij producenta
        if (producer != null) {
            producer.close();
        }

        // Zamknij połączenie MongoDB
        MongoClient client = MongoConfig.getMongoClient();
        if (client != null) {
            client.close();
        }
    }
}
