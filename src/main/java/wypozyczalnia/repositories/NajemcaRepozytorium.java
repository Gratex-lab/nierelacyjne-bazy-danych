package wypozyczalnia.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.Najemca;

import static com.mongodb.client.model.Filters.*;

/**
 * Repozytorium dla encji Najemca w MongoDB. Obsługuje operacje CRUD oraz
 * wyszukiwanie po loginie.
 */
public class NajemcaRepozytorium implements Repozytorium<Najemca> {

    private final MongoCollection<Najemca> collection;

    public NajemcaRepozytorium(MongoDatabase database) {
        this.collection = database.getCollection("najemcy", Najemca.class);
    }

    @Override
    public void dodaj(Najemca najemca) {
        collection.insertOne(najemca);
    }

    @Override
    public void usun(Najemca najemca) {
        if (najemca.getId() != null) {
            collection.deleteOne(eq("_id", najemca.getId()));
        }
    }

    @Override
    public Najemca znajdz(ObjectId najemcaId) {
        return collection.find(eq("_id", najemcaId)).first();
    }

    /**
     * Wyszukuje najemcę po unikalnym loginie.
     */
    public Najemca znajdzLogin(String login) {
        return collection.find(eq("login", login)).first();
    }

    /**
     * Aktualizuje najemcę w bazie danych.
     */
    @Override
    public void aktualizuj(Najemca najemca) {
        if (najemca.getId() != null) {
            collection.replaceOne(eq("_id", najemca.getId()), najemca);
        }
    }
}
