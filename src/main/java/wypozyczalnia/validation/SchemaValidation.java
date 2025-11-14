package wypozyczalnia.validation;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;

/**
 * Klasa odpowiedzialna za konfigurację walidacji schematów MongoDB. Narzuca
 * ograniczenia biznesowe na poziomie bazy danych.
 */
public class SchemaValidation {

    /**
     * Inicjalizuje walidację schematów dla wszystkich kolekcji.
     */
    public static void initializeSchemas(MongoDatabase database) {
        createNajemcySchema(database);
        createNieruchomosciSchema(database);
        createNajmySchema(database);
    }

    /**
     * Tworzy schemat walidacji dla kolekcji najemcy.
     */
    private static void createNajemcySchema(MongoDatabase database) {
        Document schema = new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", java.util.Arrays.asList("login", "aktywny"))
                .append("properties", new Document()
                        .append("_id", new Document("bsonType", "objectId"))
                        .append("login", new Document()
                                .append("bsonType", "string")
                                .append("minLength", 1)
                                .append("maxLength", 50)
                                .append("pattern", "^[a-zA-Z0-9_-]+$")
                                .append("description", "Login musi być unikalny i zawierać tylko litery, cyfry, _ i -"))
                        .append("aktywny", new Document()
                                .append("bsonType", "bool")
                                .append("description", "Określa czy konto najemcy jest aktywne"))));

        ValidationOptions validationOptions = new ValidationOptions()
                .validator(schema)
                .validationLevel(ValidationLevel.STRICT)
                .validationAction(ValidationAction.ERROR);

        CreateCollectionOptions options = new CreateCollectionOptions()
                .validationOptions(validationOptions);

        try {
            database.createCollection("najemcy", options);
        } catch (Exception e) {
            // Kolekcja już istnieje, aktualizujemy walidację
            try {
                database.runCommand(new Document("collMod", "najemcy")
                        .append("validator", schema)
                        .append("validationLevel", "strict")
                        .append("validationAction", "error"));
            } catch (Exception modException) {
                // Ignoruj błąd jeśli kolekcja nie istnieje
                System.out.println("Nie można zmodyfikować walidacji dla kolekcji najemcy: " + modException.getMessage());
            }
        }

        // Tworzenie unikalnego indeksu na login
        MongoCollection<Document> collection = database.getCollection("najemcy");
        collection.createIndex(new Document("login", 1),
                new com.mongodb.client.model.IndexOptions().unique(true));
    }

    /**
     * Tworzy schemat walidacji dla kolekcji nieruchomości.
     */
    private static void createNieruchomosciSchema(MongoDatabase database) {
        Document schema = new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", java.util.Arrays.asList("miasto", "dzielnica", "adres", "typ"))
                .append("properties", new Document()
                        .append("_id", new Document("bsonType", "objectId"))
                        .append("miasto", new Document()
                                .append("bsonType", "string")
                                .append("minLength", 1)
                                .append("maxLength", 100))
                        .append("dzielnica", new Document()
                                .append("bsonType", "string")
                                .append("minLength", 1)
                                .append("maxLength", 100))
                        .append("adres", new Document()
                                .append("bsonType", "string")
                                .append("minLength", 1)
                                .append("maxLength", 200))
                        .append("typ", new Document()
                                .append("enum", java.util.Arrays.asList("mieszkanie", "dom"))
                                .append("description", "Typ nieruchomości: mieszkanie lub dom"))
                        // Pola specyficzne dla mieszkania
                        .append("liczbaPokoi", new Document()
                                .append("bsonType", "int")
                                .append("minimum", 1)
                                .append("maximum", 20))
                        .append("typOgrzewania", new Document()
                                .append("bsonType", "string")
                                .append("enum", java.util.Arrays.asList("gazowe", "elektryczne", "centralne")))
                        .append("czyUmeblowane", new Document("bsonType", "bool"))
                        // Pola specyficzne dla domu
                        .append("powierzchniaDzialki", new Document()
                                .append("bsonType", "int")
                                .append("minimum", 1))
                        .append("typBudynku", new Document()
                                .append("bsonType", "string")
                                .append("enum", java.util.Arrays.asList("jednorodzinny", "bliźniak", "szeregowy", "letniskowy")))
                        .append("czyZOgrodem", new Document("bsonType", "bool")))
                .append("oneOf", java.util.Arrays.asList(
                        // Walidacja dla mieszkania
                        new Document("properties", new Document()
                                .append("typ", new Document("const", "mieszkanie"))
                                .append("liczbaPokoi", new Document("bsonType", "int"))
                                .append("typOgrzewania", new Document("bsonType", "string"))
                                .append("czyUmeblowane", new Document("bsonType", "bool")))
                                .append("required", java.util.Arrays.asList("liczbaPokoi", "typOgrzewania", "czyUmeblowane")),
                        // Walidacja dla domu
                        new Document("properties", new Document()
                                .append("typ", new Document("const", "dom"))
                                .append("powierzchniaDzialki", new Document("bsonType", "int"))
                                .append("typBudynku", new Document("bsonType", "string"))
                                .append("czyZOgrodem", new Document("bsonType", "bool")))
                                .append("required", java.util.Arrays.asList("powierzchniaDzialki", "typBudynku", "czyZOgrodem")))));

        ValidationOptions validationOptions = new ValidationOptions()
                .validator(schema)
                .validationLevel(ValidationLevel.STRICT)
                .validationAction(ValidationAction.ERROR);

        CreateCollectionOptions options = new CreateCollectionOptions()
                .validationOptions(validationOptions);

        try {
            database.createCollection("nieruchomosci", options);
        } catch (Exception e) {
            try {
                database.runCommand(new Document("collMod", "nieruchomosci")
                        .append("validator", schema)
                        .append("validationLevel", "strict")
                        .append("validationAction", "error"));
            } catch (Exception modException) {
                // Ignoruj błąd jeśli kolekcja nie istnieje
                System.out.println("Nie można zmodyfikować walidacji dla kolekcji nieruchomosci: " + modException.getMessage());
            }
        }

        // Tworzenie unikalnego indeksu na adres
        MongoCollection<Document> collection = database.getCollection("nieruchomosci");
        collection.createIndex(new Document("adres", 1),
                new com.mongodb.client.model.IndexOptions().unique(true));
    }

    /**
     * Tworzy schemat walidacji dla kolekcji najmów.
     */
    private static void createNajmySchema(MongoDatabase database) {
        Document schema = new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", java.util.Arrays.asList("najemcaId", "nieruchomoscId", "dataRozpoczecia", "dataZakonczenia"))
                .append("properties", new Document()
                        .append("_id", new Document("bsonType", "objectId"))
                        .append("najemcaId", new Document()
                                .append("bsonType", "objectId")
                                .append("description", "Referencja do najemcy"))
                        .append("nieruchomoscId", new Document()
                                .append("bsonType", "objectId")
                                .append("description", "Referencja do nieruchomości"))
                        .append("dataRozpoczecia", new Document()
                                .append("bsonType", "date")
                                .append("description", "Data rozpoczęcia najmu"))
                        .append("dataZakonczenia", new Document()
                                .append("bsonType", "date")
                                .append("description", "Data zakończenia najmu"))));

        ValidationOptions validationOptions = new ValidationOptions()
                .validator(schema)
                .validationLevel(ValidationLevel.STRICT)
                .validationAction(ValidationAction.ERROR);

        CreateCollectionOptions options = new CreateCollectionOptions()
                .validationOptions(validationOptions);

        try {
            database.createCollection("najmy", options);
        } catch (Exception e) {
            try {
                database.runCommand(new Document("collMod", "najmy")
                        .append("validator", schema)
                        .append("validationLevel", "strict")
                        .append("validationAction", "error"));
            } catch (Exception modException) {
                // Ignoruj błąd jeśli kolekcja nie istnieje
                System.out.println("Nie można zmodyfikować walidacji dla kolekcji najmy: " + modException.getMessage());
            }
        }

        // Tworzenie indeksów dla optymalizacji zapytań
        MongoCollection<Document> collection = database.getCollection("najmy");
        collection.createIndex(new Document("najemcaId", 1));
        collection.createIndex(new Document("nieruchomoscId", 1));
        collection.createIndex(new Document("dataRozpoczecia", 1).append("dataZakonczenia", 1));

        // Kompozytowy indeks dla sprawdzania konfliktów dat
        collection.createIndex(new Document("nieruchomoscId", 1)
                .append("dataRozpoczecia", 1)
                .append("dataZakonczenia", 1));
    }
}
