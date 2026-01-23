package wypozyczalnia.kafka;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class WypozyczenieProducer {

    private static final String TOPIC_NAME = "wypozyczenia";
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final String nazwaWypozyczalni;

    public WypozyczenieProducer() {
        Properties props = new Properties();

        try (InputStream input = WypozyczenieProducer.class.getClassLoader().getResourceAsStream("kafka.properties")) {
            if (input == null) {
                throw new RuntimeException("Nie znaleziono kafka.properties");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Błąd ładowania kafka.properties: " + e.getMessage());
        }

        nazwaWypozyczalni = props.getProperty("nazwa.wypozyczalni", "Wypozyczalnia");

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty("bootstrap.servers"));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        this.producer = new KafkaProducer<>(producerProps);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void sendWypozyczenie(String najemId, String najemcaId, String nieruchomoscId,
            String dataRozpoczecia, String dataZakonczenia) {
        try {
            Map<String, String> message = new HashMap<>();
            message.put("najemId", najemId);
            message.put("najemcaId", najemcaId);
            message.put("nieruchomoscId", nieruchomoscId);
            message.put("dataRozpoczecia", dataRozpoczecia);
            message.put("dataZakonczenia", dataZakonczenia);
            message.put("nazwaWypozyczalni", nazwaWypozyczalni);

            String json = objectMapper.writeValueAsString(message);

            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, najemId, json);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Błąd wysyłania wiadomości: " + exception.getMessage());
                } else {
                    System.out.println("Wysłano wypożyczenie do partycji: " + metadata.partition());
                }
            });
        } catch (Exception e) {
            System.err.println("Błąd podczas wysyłania wypożyczenia: " + e.getMessage());
        }
    }

    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}
