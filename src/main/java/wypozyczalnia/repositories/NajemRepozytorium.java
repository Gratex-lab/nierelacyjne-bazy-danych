package wypozyczalnia.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.Najem;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

/**
 * Repozytorium dla encji Najem w MongoDB. Obsługuje operacje CRUD oraz
 * zapytania biznesowe.
 */
public class NajemRepozytorium implements Repozytorium<Najem> {

    private final MongoCollection<Najem> collection;

    public NajemRepozytorium(MongoDatabase database) {
        this.collection = database.getCollection("najmy", Najem.class);
    }

    @Override
    public void dodaj(Najem najem) {
        collection.insertOne(najem);
    }

    @Override
    public void usun(Najem najem) {
        if (najem.getId() != null) {
            collection.deleteOne(eq("_id", najem.getId()));
        }
    }

    @Override
    public Najem znajdz(ObjectId najemId) {
        return collection.find(eq("_id", najemId)).first();
    }

    /**
     * Znajduje najmy dla danego najemcy i nieruchomości.
     */
    public List<Najem> znajdzNajemcowi(ObjectId nieruchomoscId, ObjectId najemcaId) {
        List<Najem> najmy = new ArrayList<>();
        collection.find(and(
                eq("najemcaId", najemcaId),
                eq("nieruchomoscId", nieruchomoscId)
        )).forEach(najmy::add);
        return najmy;
    }

    /**
     * Aktualizuje najem w bazie danych.
     */
    @Override
    public void aktualizuj(Najem najem) {
        if (najem.getId() != null) {
            collection.replaceOne(eq("_id", najem.getId()), najem);
        }
    }
}
