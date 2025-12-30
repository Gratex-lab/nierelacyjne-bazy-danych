package wypozyczalnia.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;

/**
 * Klasa konfiguracyjna dla połączenia. Zarządza sesjami, poziomami spójności i
 * replikacji.
 */
public class CassandraConfig {

    private static CassandraConfig instance;
    private CqlSession session;
    private Properties properties;

    // Poziomy spójności dla różnych operacji
    private DefaultConsistencyLevel readConsistency;
    private DefaultConsistencyLevel writeConsistency;

    private CassandraConfig() {
        loadProperties();
        initializeSession();
    }

    public static synchronized CassandraConfig getInstance() {
        if (instance == null) {
            instance = new CassandraConfig();
        }
        return instance;
    }

    private void loadProperties() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("cassandra.conf")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Nie można załadować konfiguracji Cassandra", e);
        }

        // Ustawienie poziomów spójności
        readConsistency = DefaultConsistencyLevel.valueOf(
                properties.getProperty("cassandra.consistency.read", "LOCAL_QUORUM"));
        writeConsistency = DefaultConsistencyLevel.valueOf(
                properties.getProperty("cassandra.consistency.write", "LOCAL_QUORUM"));
    }

    private void initializeSession() {
        String[] contactPoints = properties.getProperty("cassandra.contact-points", "localhost:9042")
                .split(",");
        String datacenter = properties.getProperty("cassandra.local-datacenter", "dc1");
        String keyspace = properties.getProperty("cassandra.keyspace", "wypozyczalnia");

        CqlSessionBuilder builder = CqlSession.builder();

        // Dodanie punktów kontaktowych
        for (String contactPoint : contactPoints) {
            String[] parts = contactPoint.trim().split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9042;
            builder.addContactPoint(new InetSocketAddress(host, port));
        }

        session = builder
                .withLocalDatacenter(datacenter)
                .build();

        // Inicjalizacja schematu bazy danych
        initializeSchema(keyspace);
    }

    private void initializeSchema(String keyspaceName) {
        // Utworzenie keyspace z replikacją
        String replicationStrategy = properties.getProperty("cassandra.replication.strategy",
                "NetworkTopologyStrategy");
        String dc1Replication = properties.getProperty("cassandra.replication.dc1", "2");

        session.execute(String.format(
                "CREATE KEYSPACE IF NOT EXISTS %s "
                + "WITH REPLICATION = { "
                + "'class': '%s', "
                + "'dc1': %s "
                + "}",
                keyspaceName, replicationStrategy, dc1Replication));

        // Użycie keyspace
        session.execute("USE " + keyspaceName);

        // Tworzenie tabel
        createTables();
    }

    private void createTables() {
        // Tabela najemców
        session.execute(
                "CREATE TABLE IF NOT EXISTS najemcy ("
                + "id UUID PRIMARY KEY, "
                + "login TEXT, "
                + "aktywny BOOLEAN"
                + ")");

        // Tabela najemców wg loginu (denormalizacja dla szybkich wyszukiwań)
        session.execute(
                "CREATE TABLE IF NOT EXISTS najemcy_by_login ("
                + "login TEXT PRIMARY KEY, "
                + "id UUID, "
                + "aktywny BOOLEAN"
                + ")");

        // Tabela nieruchomości
        session.execute(
                "CREATE TABLE IF NOT EXISTS nieruchomosci ("
                + "id UUID PRIMARY KEY, "
                + "miasto TEXT, "
                + "dzielnica TEXT, "
                + "adres TEXT, "
                + "typ_nieruchomosci TEXT, "
                + "liczba_pokoi INT, "
                + "typ_ogrzewania TEXT, "
                + "czy_umeblowane BOOLEAN, "
                + "powierzchnia_dzialki INT, "
                + "typ_budynku TEXT, "
                + "czy_z_ogrodem BOOLEAN"
                + ")");

        // Tabela najmów
        session.execute(
                "CREATE TABLE IF NOT EXISTS najmy ("
                + "id UUID PRIMARY KEY, "
                + "najemca_id UUID, "
                + "najemca_login TEXT, "
                + "nieruchomosc_id UUID, "
                + "nieruchomosc_adres TEXT, "
                + "data_rozpoczecia TIMESTAMP, "
                + "data_zakonczenia TIMESTAMP"
                + ")");

        // Tabela najmów wg najemcy (denormalizacja)
        session.execute(
                "CREATE TABLE IF NOT EXISTS najmy_by_najemca ("
                + "najemca_id UUID, "
                + "nieruchomosc_id UUID, "
                + "id UUID, "
                + "najemca_login TEXT, "
                + "nieruchomosc_adres TEXT, "
                + "data_rozpoczecia TIMESTAMP, "
                + "data_zakonczenia TIMESTAMP, "
                + "PRIMARY KEY (najemca_id, nieruchomosc_id)"
                + ")");

        // Tabela najmów wg nieruchomości (denormalizacja dla sprawdzania dostępności)
        session.execute(
                "CREATE TABLE IF NOT EXISTS najmy_by_nieruchomosc ("
                + "nieruchomosc_id UUID, "
                + "data_rozpoczecia TIMESTAMP, "
                + "data_zakonczenia TIMESTAMP, "
                + "id UUID, "
                + "najemca_id UUID, "
                + "najemca_login TEXT, "
                + "nieruchomosc_adres TEXT, "
                + "PRIMARY KEY (nieruchomosc_id, data_rozpoczecia)"
                + ")");
    }

    public CqlSession getSession() {
        return session;
    }

    public DefaultConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public DefaultConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    public void close() {
        if (session != null && !session.isClosed()) {
            session.close();
        }
    }
}
