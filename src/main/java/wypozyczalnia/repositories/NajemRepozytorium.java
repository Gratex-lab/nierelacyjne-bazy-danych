package wypozyczalnia.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.Najem;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

/**
 * Repozytorium dla encji Najem w MongoDB.
 * Obsługuje operacje CRUD oraz zapytania biznesowe.
 */
public class NajemRepozytorium implements Repozytorium<Najem> {

    private final MongoCollection<Document> collection;

    public NajemRepozytorium(MongoDatabase database) {
        this.collection = database.getCollection("najmy");
    }

    @Override
    public void dodaj(Najem najem) {
        Document document = najem.toDocument();
        collection.insertOne(document);
        // Set the generated ID back to the object
        najem.setId(document.getObjectId("_id"));
    }

    @Override
    public void usun(Najem najem) {
        if (najem.getId() != null) {
            collection.deleteOne(eq("_id", najem.getId()));
        }
    }

    @Override
    public Najem znajdz(ObjectId najemId) {
        Document document = collection.find(eq("_id", najemId)).first();
        return Najem.fromDocument(document);
    }

    /**
     * Znajduje najmy dla danego najemcy i nieruchomości.
     */
    public List<Najem> znajdzNajemcowi(ObjectId nieruchomoscId, ObjectId najemcaId) {
        List<Najem> najmy = new ArrayList<>();
        collection.find(and(
                eq("najemcaId", najemcaId),
                eq("nieruchomoscId", nieruchomoscId)
        )).forEach(document -> {
            Najem najem = Najem.fromDocument(document);
            if (najem != null) {
                najmy.add(najem);
            }
        });
        return najmy;
    }

    /**
     * Aktualizuje najem w bazie danych.
     */
    @Override
    public void aktualizuj(Najem najem) {
        if (najem.getId() != null) {
            collection.replaceOne(eq("_id", najem.getId()), najem.toDocument());
        }
    }
}
