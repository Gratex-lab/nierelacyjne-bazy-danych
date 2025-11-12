package wypozyczalnia.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.Najemca;

import static com.mongodb.client.model.Filters.*;

/**
 * Repozytorium dla encji Najemca w MongoDB.
 * Obsługuje operacje CRUD oraz wyszukiwanie po loginie.
 */
public class NajemcaRepozytorium implements Repozytorium<Najemca> {

    private final MongoCollection<Document> collection;

    public NajemcaRepozytorium(MongoDatabase database) {
        this.collection = database.getCollection("najemcy");
    }

    @Override
    public void dodaj(Najemca najemca) {
        Document document = najemca.toDocument();
        collection.insertOne(document);
        // Set the generated ID back to the object
        najemca.setId(document.getObjectId("_id"));
    }

    @Override
    public void usun(Najemca najemca) {
        if (najemca.getId() != null) {
            collection.deleteOne(eq("_id", najemca.getId()));
        }
    }

    @Override
    public Najemca znajdz(ObjectId najemcaId) {
        Document document = collection.find(eq("_id", najemcaId)).first();
        return Najemca.fromDocument(document);
    }

    /**
     * Wyszukuje najemcę po unikalnym loginie.
     */
    public Najemca znajdzLogin(String login) {
        Document document = collection.find(eq("login", login)).first();
        return Najemca.fromDocument(document);
    }

    /**
     * Aktualizuje najemcę w bazie danych.
     */
    @Override
    public void aktualizuj(Najemca najemca) {
        if (najemca.getId() != null) {
            collection.replaceOne(eq("_id", najemca.getId()), najemca.toDocument());
        }
    }
}
