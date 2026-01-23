package wypozyczalnia.managers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import wypozyczalnia.kafka.WypozyczenieProducer;
import wypozyczalnia.objects.Najem;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;
import wypozyczalnia.repositories.NajemRepozytorium;

/**
 * Manager obsługujący logikę biznesową najmu nieruchomości w MongoDB. Zapewnia
 * spójność, sprawdzanie limitów i dostępności.
 */
public class NajemManager {

    private final MongoDatabase database;
    private final NajemRepozytorium najemRepozytorium;
    private final NajemcaManager najemcaManager;
    private final NieruchomoscManager nieruchomoscManager;
    private final WypozyczenieProducer wypozyczenieProducer;
    private static final int MAKS_LICZBA_NAJMOW = 3;

    public NajemManager(MongoDatabase database, NajemcaManager najemcaManager, NieruchomoscManager nieruchomoscManager) {
        this.database = database;
        this.najemRepozytorium = new NajemRepozytorium(database);
        this.najemcaManager = najemcaManager;
        this.nieruchomoscManager = nieruchomoscManager;
        this.wypozyczenieProducer = new WypozyczenieProducer();
    }

    /**
     * Dokonuje najmu nieruchomości przez najemcę. Sprawdza aktywność najemcy,
     * dostępność nieruchomości i limit najmów z użyciem blokady optymistycznej.
     */
    public void dokonajNajmu(Najemca najemca, Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Najemca zarzadzanyNajemca = najemcaManager.znajdzNajemce(najemca.getId());
                Nieruchomosc zarzadzanaNieruchomosc = nieruchomoscManager.znajdzNieruchomosc(nieruchomosc.getId());

                if (zarzadzanyNajemca == null || zarzadzanaNieruchomosc == null) {
                    throw new IllegalArgumentException("Najemca lub nieruchomość nie istnieje w bazie.");
                }

                if (!zarzadzanyNajemca.isActive()) {
                    throw new IllegalStateException("Najemca jest nieaktywny.");
                }

                long aktualnaLiczbaNajmow = liczAktywneNajmyNajemcy(zarzadzanyNajemca);
                if (aktualnaLiczbaNajmow >= MAKS_LICZBA_NAJMOW) {
                    throw new IllegalStateException("Najemca '" + zarzadzanyNajemca.getLogin() + "' osiągnął limit najmów.");
                }

                // Blokada optymistyczna przy rezerwacji
                if (!nieruchomoscManager.sprobujZarezerwowac(zarzadzanaNieruchomosc, start, koniec)) {
                    throw new IllegalArgumentException("Nieruchomość jest niedostępna w podanym przedziale czasowym.");
                }

                Najem nowyNajem = new Najem(zarzadzanyNajemca, zarzadzanaNieruchomosc, start, koniec);
                najemRepozytorium.dodaj(nowyNajem);

                // Wysyłanie do kafki
                wypozyczenieProducer.sendWypozyczenie(
                        nowyNajem.getId().toString(),
                        nowyNajem.getNajemcaId().toString(),
                        nowyNajem.getNieruchomoscId().toString(),
                        nowyNajem.getDataRozpoczecia().toString(),
                        nowyNajem.getDataZakonczenia().toString()
                );

                System.out.println("Dokonano najmu nieruchomości: " + zarzadzanaNieruchomosc.getPelnyAdres());
                return;

            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new RuntimeException("Błąd podczas wykonywania najmu: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("Konflikt blokady optymistycznej") && retryCount < maxRetries - 1) {
                    retryCount++;
                    System.out.println("Konflikt blokady optymistycznej, próba " + retryCount + "/" + maxRetries);
                    try {
                        Thread.sleep(50 + (retryCount * 25)); // Backoff z eksponencjalnym wzrostem
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Przerwano podczas oczekiwania na ponowną próbę", ie);
                    }
                    continue;
                }
                throw new RuntimeException("Błąd podczas wykonywania najmu: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("Nie udało się dokonać najmu po " + maxRetries + " próbach z powodu konfliktów blokady optymistycznej");
    }

    /**
     * Zwraca wynajętą nieruchomość.
     */
    public void zwrocNieruchomosc(Najemca najemca, Nieruchomosc nieruchomosc) {
        try {
            List<Najem> najmy = najemRepozytorium.znajdzNajemcowi(nieruchomosc.getId(), najemca.getId());
            if (najmy.isEmpty()) {
                throw new RuntimeException("Najemca nie ma aktywnego najmu dla nieruchomości: " + nieruchomosc.getPelnyAdres());
            }

            Najem najemDoZwrotu = najmy.get(0);
            najemRepozytorium.usun(najemDoZwrotu);

            System.out.println("Zwrócono nieruchomość '" + nieruchomosc.getPelnyAdres() + "'. Limit zwolniony.");
        } catch (RuntimeException e) {
            throw new RuntimeException("Błąd podczas zwrotu nieruchomości: " + e.getMessage(), e);
        }
    }

    /**
     * Liczy aktywne najmy najemcy używając MongoDB.
     */
    private long liczAktywneNajmyNajemcy(Najemca najemca) {
        LocalDateTime teraz = LocalDateTime.now();
        Date terazDate = Date.from(teraz.atZone(ZoneId.systemDefault()).toInstant());

        MongoCollection<Document> collection = database.getCollection("najmy");

        Document query = new Document("najemcaId", najemca.getId())
                .append("dataZakonczenia", new Document("$gt", terazDate));

        return collection.countDocuments(query);
    }

    public void close() {
        if (wypozyczenieProducer != null) {
            wypozyczenieProducer.close();
        }
    }
}
