package library.managers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import library.objects.auto.Auto;
import library.objects.auto.SamochodOsobowy;
import library.objects.auto.SamochodCiezarowy;
import library.repositories.AutoRepozytorium;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Manager obsługujący logikę biznesową zarządzania autami. Obsługuje różne typy
 * samochodów z wykorzystaniem polimorfizmu.
 */
public class AutoManager {

    private final EntityManager entityManager;
    private final AutoRepozytorium autoRepozytorium;

    public AutoManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.autoRepozytorium = new AutoRepozytorium(entityManager);
    }

    /**
     * Dodaje nowe auto do wypożyczalni.
     */
    public void dodajAuto(Auto auto) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            Auto a = autoRepozytorium.znajdz(auto.getId());
            if (a != null) {
                throw new IllegalArgumentException("Auto " + auto.getPelnaNazwa() + " o ID '" + auto.getId() + "' już istnieje.");
            }
            autoRepozytorium.dodaj(auto);
            tx.commit();
        } catch (IllegalArgumentException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        String typ = auto instanceof SamochodOsobowy ? "Samochód osobowy"
                : (auto instanceof SamochodCiezarowy ? "Samochód ciężarowy" : "Auto");
        System.out.println("Dodano auto: " + auto.getPelnaNazwa() + " (" + typ + ")");
    }

    /**
     * Znajduje auto po jego UUID.
     */
    public Auto znajdzAuto(UUID autoUuid) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            Auto a = autoRepozytorium.znajdz(autoUuid);
            tx.commit();
            return a;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Usuwa auto z wypożyczalni.
     */
    public void usunAuto(Auto auto) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            autoRepozytorium.usun(auto);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println("Usunięto auto: " + auto.getPelnaNazwa() + ".");
    }

    /**
     * Sprawdza czy auto jest zajęte w podanym okresie. Używa izolacji
     * transakcji dla zapewnienia spójności danych.
     */
    public boolean czyJestZajete(Auto auto, LocalDateTime start, LocalDateTime koniec) {
        if (start.isAfter(koniec) || start.isEqual(koniec)) {
            throw new IllegalArgumentException("Data rozpoczęcia musi być przed datą zakończenia.");
        }
        return autoRepozytorium.czyJestZajety(auto, start, koniec);
    }
}
