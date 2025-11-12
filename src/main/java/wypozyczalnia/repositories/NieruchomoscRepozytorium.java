package wypozyczalnia.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static com.mongodb.client.model.Filters.*;

/**
 * Repozytorium dla encji Nieruchomosc w MongoDB.
 * Obsługuje operacje CRUD oraz sprawdzanie dostępności nieruchomości.
 */
public class NieruchomoscRepozytorium implements Repozytorium<Nieruchomosc> {

    private final MongoCollection<Document> collection;
    private final MongoCollection<Document> najmyCollection;

    public NieruchomoscRepozytorium(MongoDatabase database) {
        this.collection = database.getCollection("nieruchomosci");
        this.najmyCollection = database.getCollection("najmy");
    }

    @Override
    public void dodaj(Nieruchomosc nieruchomosc) {
        Document document = nieruchomosc.toDocument();
        collection.insertOne(document);
        // Set the generated ID back to the object
        nieruchomosc.setId(document.getObjectId("_id"));
    }

    @Override
    public void usun(Nieruchomosc nieruchomosc) {
        if (nieruchomosc.getId() != null) {
            collection.deleteOne(eq("_id", nieruchomosc.getId()));
        }
    }

    @Override
    public Nieruchomosc znajdz(ObjectId nieruchomoscId) {
        Document document = collection.find(eq("_id", nieruchomoscId)).first();
        return Nieruchomosc.fromDocument(document);
    }

    /**
     * Sprawdza czy nieruchomość jest zajęta w podanym okresie.
     * Wykorzystuje transakcje MongoDB dla zapewnienia spójności.
     */
    public boolean czyJestZajeta(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        Date startDate = Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(koniec.atZone(ZoneId.systemDefault()).toInstant());

        Document query = new Document("nieruchomoscId", nieruchomosc.getId())
                .append("$or", java.util.Arrays.asList(
                    new Document("$and", java.util.Arrays.asList(
                        new Document("dataRozpoczecia", new Document("$lt", endDate)),
                        new Document("dataZakonczenia", new Document("$gt", startDate))
                    ))
                ));

        long count = najmyCollection.countDocuments(query);
        return count > 0;
    }

    /**
     * Aktualizuje nieruchomość w bazie danych.
     */
    @Override
    public void aktualizuj(Nieruchomosc nieruchomosc) {
        if (nieruchomosc.getId() != null) {
            collection.replaceOne(eq("_id", nieruchomosc.getId()), nieruchomosc.toDocument());
        }
    }
}
