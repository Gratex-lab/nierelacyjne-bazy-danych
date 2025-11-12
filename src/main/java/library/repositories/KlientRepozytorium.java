package library.repositories;

import jakarta.persistence.EntityManager;
import library.objects.Klient;

import java.util.List;
import java.util.UUID;

/**
 * Repozytorium dla encji Klient. Obsługuje operacje CRUD oraz wyszukiwanie po
 * loginie.
 */
public class KlientRepozytorium implements Repozytorium<Klient> {

    private final EntityManager entityManager;

    public KlientRepozytorium(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dodaj(Klient klient) {
        entityManager.persist(klient);
    }

    @Override
    public void usun(Klient klient) {
        entityManager.remove(klient);
    }

    @Override
    public Klient znajdz(UUID klientId) {
        List<Klient> klienci = entityManager.createQuery("SELECT k FROM Klient k WHERE k.id = :id", Klient.class)
                .setParameter("id", klientId)
                .getResultList();
        return klienci.isEmpty() ? null : klienci.getFirst();
    }

    /**
     * Wyszukuje klienta po unikalnym loginie.
     */
    public Klient znajdzLogin(String login) {
        List<Klient> klienci = entityManager.createQuery("SELECT k FROM Klient k WHERE k.login = :login", Klient.class)
                .setParameter("login", login)
                .getResultList();
        return klienci.isEmpty() ? null : klienci.getFirst();
    }
}
