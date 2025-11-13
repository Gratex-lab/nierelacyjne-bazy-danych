package wypozyczalnia.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import wypozyczalnia.validation.SchemaValidation;

/**
 * Konfiguracja połączenia z MongoDB replica set.
 */
public class MongoConfig {

    private static final String DATABASE_NAME = "wypozyczalnia_db";
    private static final String CONNECTION_STRING = "mongodb://mongo1:27017,mongo2:27018,mongo3:27019/wypozyczalnia_db?replicaSet=rs0";

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static boolean schemaInitialized = false;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            initializeDatabase();
        }
        return database;
    }

    public static MongoClient getMongoClient() {
        if (mongoClient == null) {
            initializeDatabase();
        }
        return mongoClient;
    }

    private static void initializeDatabase() {
        ConnectionString connectionString = new ConnectionString(CONNECTION_STRING);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase(DATABASE_NAME);

        // Inicjalizuj walidację schematów tylko raz
        if (!schemaInitialized) {
            try {
                SchemaValidation.initializeSchemas(database);
                schemaInitialized = true;
                System.out.println("Schemat walidacji MongoDB został zainicjalizowany.");
            } catch (Exception e) {
                System.err.println("Błąd podczas inicjalizacji schematów: " + e.getMessage());
            }
        }
    }

    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            schemaInitialized = false;
        }
    }
}
