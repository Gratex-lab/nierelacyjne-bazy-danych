package wypozyczalnia.managers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;
import wypozyczalnia.objects.nieruchomosc.Dom;
import wypozyczalnia.repositories.NieruchomoscRepozytorium;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Manager obsługujący logikę biznesową zarządzania nieruchomościami. Obsługuje
 * różne typy nieruchomości z wykorzystaniem polimorfizmu.
 */
public class NieruchomoscManager {

    private final EntityManager entityManager;
    private final NieruchomoscRepozytorium nieruchomoscRepozytorium;

    public NieruchomoscManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.nieruchomoscRepozytorium = new NieruchomoscRepozytorium(entityManager);
    }

    /**
     * Dodaje nową nieruchomość do wypożyczalni.
     */
    public void dodajNieruchomosc(Nieruchomosc nieruchomosc) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            Nieruchomosc n = nieruchomoscRepozytorium.znajdz(nieruchomosc.getId());
            if (n != null) {
                throw new IllegalArgumentException("Nieruchomość " + nieruchomosc.getPelnyAdres() + " o ID '" + nieruchomosc.getId() + "' już istnieje.");
            }
            nieruchomoscRepozytorium.dodaj(nieruchomosc);
            tx.commit();
        } catch (IllegalArgumentException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        String typ = nieruchomosc instanceof Mieszkanie ? "Mieszkanie"
                : (nieruchomosc instanceof Dom ? "Dom" : "Nieruchomość");
        System.out.println("Dodano nieruchomość: " + nieruchomosc.getPelnyAdres() + " (" + typ + ")");
    }

    /**
     * Znajduje nieruchomość po jej UUID.
     */
    public Nieruchomosc znajdzNieruchomosc(UUID nieruchomoscUuid) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            Nieruchomosc n = nieruchomoscRepozytorium.znajdz(nieruchomoscUuid);
            tx.commit();
            return n;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Usuwa nieruchomość z wypożyczalni.
     */
    public void usunNieruchomosc(Nieruchomosc nieruchomosc) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            nieruchomoscRepozytorium.usun(nieruchomosc);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println("Usunięto nieruchomość: " + nieruchomosc.getPelnyAdres() + ".");
    }

    /**
     * Sprawdza czy nieruchomość jest zajęta w podanym okresie. Używa izolacji
     * transakcji dla zapewnienia spójności danych.
     */
    public boolean czyJestZajeta(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        if (start.isAfter(koniec) || start.isEqual(koniec)) {
            throw new IllegalArgumentException("Data rozpoczęcia musi być przed datą zakończenia.");
        }
        return nieruchomoscRepozytorium.czyJestZajeta(nieruchomosc, start, koniec);
    }
}
