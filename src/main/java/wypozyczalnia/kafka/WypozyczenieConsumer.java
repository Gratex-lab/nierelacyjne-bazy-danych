package wypozyczalnia.kafka;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoDatabase;

import wypozyczalnia.objects.Najem;
import wypozyczalnia.repositories.NajemRepozytorium;

public class WypozyczenieConsumer implements Runnable {

    private static final String TOPIC_NAME = "wypozyczenia";
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final NajemRepozytorium najemRepozytorium;
    private volatile boolean running = true;

    public WypozyczenieConsumer(MongoDatabase database) {
        Properties props = new Properties();

        try (InputStream input = WypozyczenieConsumer.class.getClassLoader().getResourceAsStream("kafka.properties")) {
            if (input == null) {
                throw new RuntimeException("Nie znaleziono kafka.properties");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Błąd ładowania kafka.properties: " + e.getMessage());
        }

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty("bootstrap.servers"));
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, props.getProperty("group.id"));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        this.consumer = new KafkaConsumer<>(consumerProps);

        // Rejestracja listenera do monitorowania przypisań partycji
        this.consumer.subscribe(Collections.singletonList(TOPIC_NAME), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                System.out.println("Partycje odebrane: " + partitions);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.println("Konsument przypisany do partycji: " + partitions);
            }
        });

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.najemRepozytorium = new NajemRepozytorium(database);

        // oczekiwanie na przypisanie partycji
        for (int i = 0; i < 5; i++) {
            consumer.poll(Duration.ofMillis(200));
            if (!consumer.assignment().isEmpty()) {
                break;
            }
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        System.out.println("Odebrano wiadomość z partycji: " + record.partition()
                                + ", offset: " + record.offset());

                        Map<String, String> message = objectMapper.readValue(record.value(), Map.class);

                        ObjectId najemId = new ObjectId(message.get("najemId"));
                        ObjectId najemcaId = new ObjectId(message.get("najemcaId"));
                        ObjectId nieruchomoscId = new ObjectId(message.get("nieruchomoscId"));
                        LocalDateTime dataRozpoczecia = LocalDateTime.parse(message.get("dataRozpoczecia"));
                        LocalDateTime dataZakonczenia = LocalDateTime.parse(message.get("dataZakonczenia"));

                        // Sprawdzanie czy najem już istnieje
                        if (najemRepozytorium.znajdz(najemId) == null) {
                            Najem najem = new Najem(najemcaId, nieruchomoscId, dataRozpoczecia, dataZakonczenia);
                            najem.setId(najemId);
                            najemRepozytorium.dodaj(najem);
                            System.out.println("Zapisano najem do bazy danych: " + najemId);
                        } else {
                            System.out.println("Najem już istnieje w bazie: " + najemId);
                        }

                    } catch (Exception e) {
                        System.err.println("Błąd przetwarzania wiadomości: " + e.getMessage());
                    }
                }

                consumer.commitSync();
            }
        } catch (Exception e) {
            System.err.println("Błąd konsumenta: " + e.getMessage());
        } finally {
            consumer.close();
        }
    }

    public void shutdown() {
        running = false;
    }
}
