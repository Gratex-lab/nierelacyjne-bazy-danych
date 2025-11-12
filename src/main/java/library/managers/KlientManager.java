package library.managers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import library.objects.Klient;
import library.repositories.KlientRepozytorium;

import java.util.UUID;

/**
 * Manager obsługujący logikę biznesową zarządzania klientami wypożyczalni.
 * Zapewnia unikalność loginów i transakcyjność operacji.
 */
public class KlientManager {

    private final EntityManager entityManager;
    private final KlientRepozytorium klientRepozytorium;

    public KlientManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.klientRepozytorium = new KlientRepozytorium(entityManager);
    }

    /**
     * Dodaje nowego klienta do systemu. Sprawdza unikalność loginu.
     */
    public void dodajKlienta(Klient klient) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            Klient k = klientRepozytorium.znajdzLogin(klient.getLogin());
            if (k != null) {
                throw new IllegalArgumentException("Klient z loginem '" + klient.getLogin() + "' już istnieje.");
            }
            klientRepozytorium.dodaj(klient);
            tx.commit();
        } catch (IllegalArgumentException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println("Dodano klienta: " + klient.getLogin());
    }

    /**
     * Znajduje klienta po jego UUID.
     */
    public Klient znajdzKlienta(UUID klientUuid) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            Klient k = klientRepozytorium.znajdz(klientUuid);
            if (k == null) {
                throw new IllegalArgumentException("Klient o ID '" + klientUuid + "' nie istnieje.");
            }
            tx.commit();
            return k;
        } catch (IllegalArgumentException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
