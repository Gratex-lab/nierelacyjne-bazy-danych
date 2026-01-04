package wypozyczalnia.managers;

import java.time.LocalDateTime;
import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;

import wypozyczalnia.objects.nieruchomosc.Dom;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;
import wypozyczalnia.repositories.NieruchomoscRepozytorium;

/**
 * Manager obsługujący logikę biznesową zarządzania nieruchomościami.
 */
public class NieruchomoscManager {

    private final NieruchomoscRepozytorium nieruchomoscRepozytorium;

    public NieruchomoscManager(CqlSession session) {
        this.nieruchomoscRepozytorium = new NieruchomoscRepozytorium(session);
    }

    /**
     * Dodaje nową nieruchomość do wypożyczalni.
     */
    public void dodajNieruchomosc(Nieruchomosc nieruchomosc) {
        try {
            nieruchomoscRepozytorium.dodaj(nieruchomosc);

            String typ = nieruchomosc instanceof Mieszkanie ? "Mieszkanie"
                    : (nieruchomosc instanceof Dom ? "Dom" : "Nieruchomość");
            System.out.println("Dodano nieruchomość: " + nieruchomosc.getPelnyAdres() + " (" + typ + ")");
        } catch (IllegalArgumentException e) {
            System.err.println("Błąd podczas dodawania nieruchomości: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Nieoczekiwany błąd podczas dodawania nieruchomości: " + e.getMessage());
            throw new RuntimeException("Nie udało się dodać nieruchomości", e);
        }
    }

    /**
     * Znajduje nieruchomość po jej UUID.
     */
    public Nieruchomosc znajdzNieruchomosc(UUID nieruchomoscUuid) {
        try {
            return nieruchomoscRepozytorium.znajdz(nieruchomoscUuid);
        } catch (Exception e) {
            System.err.println("Błąd podczas wyszukiwania nieruchomości: " + e.getMessage());
            throw new RuntimeException("Nie udało się wyszukać nieruchomości", e);
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
            System.err.println("Błąd podczas usuwania nieruchomości: " + e.getMessage());
            throw new RuntimeException("Nie udało się usunąć nieruchomości", e);
        }
    }

    /**
     * Sprawdza czy nieruchomość jest zajęta w podanym okresie. Używa
     * denormalizowanej tabeli dla szybkich sprawdzeń dostępności.
     */
    public boolean czyJestZajeta(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        if (start.isAfter(koniec) || start.isEqual(koniec)) {
            throw new IllegalArgumentException("Data rozpoczęcia musi być przed datą zakończenia.");
        }

        try {
            return nieruchomoscRepozytorium.czyJestZajeta(nieruchomosc, start, koniec);
        } catch (Exception e) {
            System.err.println("Błąd podczas sprawdzania dostępności nieruchomości: " + e.getMessage());
            throw new RuntimeException("Nie udało się sprawdzić dostępności nieruchomości", e);
        }
    }

    /**
     * Aktualizuje nieruchomość.
     */
    public void aktualizujNieruchomosc(Nieruchomosc nieruchomosc) {
        try {
            nieruchomoscRepozytorium.aktualizuj(nieruchomosc);
            System.out.println("Zaktualizowano nieruchomość: " + nieruchomosc.getPelnyAdres());
        } catch (Exception e) {
            System.err.println("Błąd podczas aktualizacji nieruchomości: " + e.getMessage());
            throw new RuntimeException("Nie udało się zaktualizować nieruchomości", e);
        }
    }
}
