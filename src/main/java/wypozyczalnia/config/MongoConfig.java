package wypozyczalnia.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import wypozyczalnia.validation.SchemaValidation;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Konfiguracja połączenia z MongoDB replica set.
 */
public class MongoConfig {

    private static final String DATABASE_NAME = "wypozyczalnia_db";
    private static final String CONNECTION_STRING = "mongodb://localhost:27017,localhost:27018,localhost:27019/?replicaSet=replica_set_single";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "adminpassword";
    private static final String AUTH_DATABASE = "admin";

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

        // Konfiguracja kodeku POJO dla automatycznego mapowania
        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        // Konfiguracja uwierzytelnienia
        MongoCredential credential = MongoCredential.createCredential(USERNAME, AUTH_DATABASE, PASSWORD.toCharArray());

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .credential(credential)
                .codecRegistry(codecRegistry)
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
