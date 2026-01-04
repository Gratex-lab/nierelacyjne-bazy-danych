package wypozyczalnia.managers;

import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;

import wypozyczalnia.objects.Najemca;
import wypozyczalnia.repositories.NajemcaRepozytorium;

/**
 * Manager obsługujący logikę biznesową zarządzania najemcami wypożyczalni.
 */
public class NajemcaManager {

    private final NajemcaRepozytorium najemcaRepozytorium;

    public NajemcaManager(CqlSession session) {
        this.najemcaRepozytorium = new NajemcaRepozytorium(session);
    }

    /**
     * Dodaje nowego najemcę do systemu. Sprawdza unikalność loginu.
     */
    public void dodajNajemce(Najemca najemca) {
        try {
            najemcaRepozytorium.dodaj(najemca);
            System.out.println("Dodano najemcę: " + najemca.getLogin());
        } catch (IllegalArgumentException e) {
            System.err.println("Błąd podczas dodawania najemcy: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Nieoczekiwany błąd podczas dodawania najemcy: " + e.getMessage());
            throw new RuntimeException("Nie udało się dodać najemcy", e);
        }
    }

    /**
     * Znajduje najemcę po jego UUID.
     */
    public Najemca znajdzNajemce(UUID najemcaUuid) {
        try {
            Najemca najemca = najemcaRepozytorium.znajdz(najemcaUuid);
            if (najemca == null) {
                throw new IllegalArgumentException("Najemca o ID '" + najemcaUuid + "' nie istnieje.");
            }
            return najemca;
        } catch (IllegalArgumentException e) {
            System.err.println("Błąd podczas wyszukiwania najemcy: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Nieoczekiwany błąd podczas wyszukiwania najemcy: " + e.getMessage());
            throw new RuntimeException("Nie udało się wyszukać najemcy", e);
        }
    }

    /**
     * Znajduje najemcę po loginie.
     */
    public Najemca znajdzNajemcePoLoginie(String login) {
        try {
            return najemcaRepozytorium.znajdzLogin(login);
        } catch (Exception e) {
            System.err.println("Nieoczekiwany błąd podczas wyszukiwania najemcy po loginie: " + e.getMessage());
            throw new RuntimeException("Nie udało się wyszukać najemcy po loginie", e);
        }
    }

    /**
     * Deaktywuje najemcę.
     */
    public void deaktywujNajemce(Najemca najemca) {
        try {
            najemca.setAktywny(false);
            najemcaRepozytorium.aktualizuj(najemca);
            System.out.println("Deaktywowano najemcę: " + najemca.getLogin());
        } catch (Exception e) {
            System.err.println("Błąd podczas deaktywacji najemcy: " + e.getMessage());
            throw new RuntimeException("Nie udało się deaktywować najemcy", e);
        }
    }

    /**
     * Aktywuje najemcę.
     */
    public void aktywujNajemce(Najemca najemca) {
        try {
            najemca.setAktywny(true);
            najemcaRepozytorium.aktualizuj(najemca);
            System.out.println("Aktywowano najemcę: " + najemca.getLogin());
        } catch (Exception e) {
            System.err.println("Błąd podczas aktywacji najemcy: " + e.getMessage());
            throw new RuntimeException("Nie udało się aktywować najemcy", e);
        }
    }

    /**
     * Usuwa najemcę.
     */
    public void usunNajemce(Najemca najemca) {
        try {
            najemcaRepozytorium.usun(najemca);
            System.out.println("Usunięto najemcę: " + najemca.getLogin());
        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania najemcy: " + e.getMessage());
            throw new RuntimeException("Nie udało się usunąć najemcy", e);
        }
    }
}
