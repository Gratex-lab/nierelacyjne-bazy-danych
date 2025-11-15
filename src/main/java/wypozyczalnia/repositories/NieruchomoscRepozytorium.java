package wypozyczalnia.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;
import wypozyczalnia.objects.Najem;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static com.mongodb.client.model.Filters.*;

/**
 * Repozytorium dla encji Nieruchomosc w MongoDB. Obsługuje operacje CRUD oraz
 * sprawdzanie dostępności nieruchomości.
 */
public class NieruchomoscRepozytorium implements Repozytorium<Nieruchomosc> {

    private final MongoCollection<Nieruchomosc> collection;
    private final MongoCollection<Najem> najmyCollection;

    public NieruchomoscRepozytorium(MongoDatabase database) {
        this.collection = database.getCollection("nieruchomosci", Nieruchomosc.class);
        this.najmyCollection = database.getCollection("najmy", Najem.class);
    }

    @Override
    public void dodaj(Nieruchomosc nieruchomosc) {
        collection.insertOne(nieruchomosc);
    }

    @Override
    public void usun(Nieruchomosc nieruchomosc) {
        if (nieruchomosc.getId() != null) {
            collection.deleteOne(eq("_id", nieruchomosc.getId()));
        }
    }

    @Override
    public Nieruchomosc znajdz(ObjectId nieruchomoscId) {
        return collection.find(eq("_id", nieruchomoscId)).first();
    }

    /**
     * Sprawdza czy nieruchomość jest zajęta w podanym okresie. Wykorzystuje
     * transakcje MongoDB dla zapewnienia spójności.
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
     * Sprawdza dostępność i zwiększa wersję nieruchomości. Rzuca
     * wyjątek jeśli nieruchomość jest niedostępna lub wersja się nie zgadza.
     */
    public boolean sprobujZarezerwowac(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        Date startDate = Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(koniec.atZone(ZoneId.systemDefault()).toInstant());

        // Sprawdź czy są konflikty czasowe
        Document conflictQuery = new Document("nieruchomoscId", nieruchomosc.getId())
                .append("$or", java.util.Arrays.asList(
                        new Document("$and", java.util.Arrays.asList(
                                new Document("dataRozpoczecia", new Document("$lt", endDate)),
                                new Document("dataZakonczenia", new Document("$gt", startDate))
                        ))
                ));

        if (najmyCollection.countDocuments(conflictQuery) > 0) {
            return false; // Nieruchomość zajęta
        }

        // Zwiększ wersję (blokada optymistyczna)
        Document filter = new Document("_id", nieruchomosc.getId())
                .append("version", nieruchomosc.getVersion());
        Document update = new Document("$inc", new Document("version", 1));

        long updatedCount = collection.updateOne(filter, update).getModifiedCount();

        if (updatedCount == 0) {
            throw new RuntimeException("Konflikt blokady optymistycznej - spróbuj ponownie");
        }

        // Zaktualizuj wersję w obiekcie
        nieruchomosc.setVersion(nieruchomosc.getVersion() + 1);
        return true;
    }

    /**
     * Aktualizuje nieruchomość w bazie danych.
     */
    @Override
    public void aktualizuj(Nieruchomosc nieruchomosc) {
        if (nieruchomosc.getId() != null) {
            collection.replaceOne(eq("_id", nieruchomosc.getId()), nieruchomosc);
        }
    }
}
