package wypozyczalnia.managers;

import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;
import wypozyczalnia.objects.nieruchomosc.Dom;
import wypozyczalnia.repositories.NieruchomoscRepozytorium;

import java.time.LocalDateTime;

/**
 * Manager obsługujący logikę biznesową zarządzania nieruchomościami. Obsługuje
 * różne typy nieruchomości z wykorzystaniem polimorfizmu w MongoDB.
 */
public class NieruchomoscManager {

    private final NieruchomoscRepozytorium nieruchomoscRepozytorium;

    public NieruchomoscManager(MongoDatabase database) {
        this.nieruchomoscRepozytorium = new NieruchomoscRepozytorium(database);
    }

    /**
     * Dodaje nową nieruchomość do wypożyczalni.
     */
    public void dodajNieruchomosc(Nieruchomosc nieruchomosc) {
        if (nieruchomosc == null) {
            throw new IllegalArgumentException("Nieruchomość nie może być null");
        }
        try {
            nieruchomoscRepozytorium.dodaj(nieruchomosc);
            String typ = nieruchomosc instanceof Mieszkanie ? "Mieszkanie"
                    : (nieruchomosc instanceof Dom ? "Dom" : "Nieruchomość");
            System.out.println("Dodano nieruchomość: " + nieruchomosc.getPelnyAdres() + " (" + typ + ")");
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas dodawania nieruchomości: " + e.getMessage(), e);
        }
    }

    /**
     * Znajduje nieruchomość po jej ObjectId.
     */
    public Nieruchomosc znajdzNieruchomosc(ObjectId nieruchomoscId) {
        try {
            return nieruchomoscRepozytorium.znajdz(nieruchomoscId);
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas wyszukiwania nieruchomości: " + e.getMessage(), e);
        }
    }

    /**
     * Usuwa nieruchomość z wypożyczalni.
     */
    public void usunNieruchomosc(Nieruchomosc nieruchomosc) {
        try {
            nieruchomoscRepozytorium.usun(nieruchomosc);
            System.out.println("Usunięto nieruchomość: " + nieruchomosc.getPelnyAdres() + ".");
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas usuwania nieruchomości: " + e.getMessage(), e);
        }
    }

    /**
     * Sprawdza czy nieruchomość jest zajęta w podanym okresie. Używa zapytań
     * MongoDB dla zapewnienia spójności danych.
     */
    public boolean czyJestZajeta(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        if (start.isAfter(koniec) || start.isEqual(koniec)) {
            throw new IllegalArgumentException("Data rozpoczęcia musi być przed datą zakończenia.");
        }
        try {
            return nieruchomoscRepozytorium.czyJestZajeta(nieruchomosc, start, koniec);
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas sprawdzania dostępności nieruchomości: " + e.getMessage(), e);
        }
    }

    /**
     * Sprawdza dostępność i rezerwuje nieruchomość z użyciem blokady
     * optymistycznej. Zapobiega równoczesnym wypożyczeniom tej samej
     * nieruchomości.
     */
    public boolean sprobujZarezerwowac(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        if (start.isAfter(koniec) || start.isEqual(koniec)) {
            throw new IllegalArgumentException("Data rozpoczęcia musi być przed datą zakończenia.");
        }
        try {
            return nieruchomoscRepozytorium.sprobujZarezerwowac(nieruchomosc, start, koniec);
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas rezerwacji nieruchomości: " + e.getMessage(), e);
        }
    }

    /**
     * Aktualizuje nieruchomość w systemie.
     */
    public void aktualizujNieruchomosc(Nieruchomosc nieruchomosc) {
        try {
            nieruchomoscRepozytorium.aktualizuj(nieruchomosc);
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas aktualizacji nieruchomości: " + e.getMessage(), e);
        }
    }
}
