package library.repositories;

import jakarta.persistence.EntityManager;
import library.objects.Wypozyczenie;

import java.util.List;
import java.util.UUID;

/**
 * Repozytorium dla encji Wypozyczenie. Obsługuje operacje CRUD oraz zapytania
 * biznesowe.
 */
public class WypozyczenieRepozytorium implements Repozytorium<Wypozyczenie> {

    private final EntityManager entityManager;

    public WypozyczenieRepozytorium(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dodaj(Wypozyczenie wypozyczenie) {
        entityManager.persist(wypozyczenie);
    }

    @Override
    public void usun(Wypozyczenie wypozyczenie) {
        entityManager.remove(wypozyczenie);
    }

    @Override
    public Wypozyczenie znajdz(UUID wypozyczenieUuid) {
        List<Wypozyczenie> wypozyczenia = entityManager.createQuery(
                "SELECT w FROM Wypozyczenie w WHERE w.id = :id", Wypozyczenie.class)
                .setParameter("id", wypozyczenieUuid)
                .getResultList();
        return wypozyczenia.isEmpty() ? null : wypozyczenia.getFirst();
    }

    /**
     * Znajduje wypożyczenia dla danego klienta i auta.
     */
    public List<Wypozyczenie> znajdzKlientowi(UUID autoId, UUID klientUuid) {
        return entityManager.createQuery(
                "SELECT w FROM Wypozyczenie w WHERE w.klient.id = :klientId AND w.auto.id = :autoId",
                Wypozyczenie.class)
                .setParameter("klientId", klientUuid)
                .setParameter("autoId", autoId)
                .getResultList();
    }
}
