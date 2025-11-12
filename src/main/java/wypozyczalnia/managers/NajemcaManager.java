package wypozyczalnia.managers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.repositories.NajemcaRepozytorium;

import java.util.UUID;

/**
 * Manager obsługujący logikę biznesową zarządzania najemcami wypożyczalni.
 * Zapewnia unikalność loginów i transakcyjność operacji.
 */
public class NajemcaManager {

    private final EntityManager entityManager;
    private final NajemcaRepozytorium najemcaRepozytorium;

    public NajemcaManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.najemcaRepozytorium = new NajemcaRepozytorium(entityManager);
    }

    /**
     * Dodaje nowego najemcę do systemu. Sprawdza unikalność loginu.
     */
    public void dodajNajemce(Najemca najemca) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            Najemca n = najemcaRepozytorium.znajdzLogin(najemca.getLogin());
            if (n != null) {
                throw new IllegalArgumentException("Najemca z loginem '" + najemca.getLogin() + "' już istnieje.");
            }
            najemcaRepozytorium.dodaj(najemca);
            tx.commit();
        } catch (IllegalArgumentException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println("Dodano najemcę: " + najemca.getLogin());
    }

    /**
     * Znajduje najemcę po jego UUID.
     */
    public Najemca znajdzNajemce(UUID najemcaUuid) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            Najemca n = najemcaRepozytorium.znajdz(najemcaUuid);
            if (n == null) {
                throw new IllegalArgumentException("Najemca o ID '" + najemcaUuid + "' nie istnieje.");
            }
            tx.commit();
            return n;
        } catch (IllegalArgumentException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
