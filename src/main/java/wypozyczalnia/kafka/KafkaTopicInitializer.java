package wypozyczalnia.kafka;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;

public class KafkaTopicInitializer {

    private static final String TOPIC_NAME = "wypozyczenia";
    private static final int NUM_PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 3;

    public static void initializeTopic() {
        Properties props = new Properties();

        try (InputStream input = KafkaTopicInitializer.class.getClassLoader().getResourceAsStream("kafka.properties")) {
            if (input == null) {
                System.out.println("Nie znaleziono kafka.properties");
                return;
            }
            props.load(input);
        } catch (IOException e) {
            System.err.println("Błąd ładowania kafka.properties: " + e.getMessage());
            return;
        }

        try (AdminClient adminClient = AdminClient.create(props)) {
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> topicNames = topics.names().get();

            if (!topicNames.contains(TOPIC_NAME)) {
                NewTopic newTopic = new NewTopic(TOPIC_NAME, NUM_PARTITIONS, REPLICATION_FACTOR);
                adminClient.createTopics(Collections.singleton(newTopic)).all().get();
                System.out.println("Utworzono temat: " + TOPIC_NAME + " z " + NUM_PARTITIONS + " partycjami");
            } else {
                System.out.println("Temat " + TOPIC_NAME + " już istnieje");
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas inicjalizacji tematu: " + e.getMessage());
        }
    }
}
