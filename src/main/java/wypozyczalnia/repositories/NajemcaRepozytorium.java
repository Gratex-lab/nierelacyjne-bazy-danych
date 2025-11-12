package wypozyczalnia.repositories;

import jakarta.persistence.EntityManager;
import wypozyczalnia.objects.Najemca;

import java.util.List;
import java.util.UUID;

/**
 * Repozytorium dla encji Najemca. Obsługuje operacje CRUD oraz wyszukiwanie po
 * loginie.
 */
public class NajemcaRepozytorium implements Repozytorium<Najemca> {

    private final EntityManager entityManager;

    public NajemcaRepozytorium(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dodaj(Najemca najemca) {
        entityManager.persist(najemca);
    }

    @Override
    public void usun(Najemca najemca) {
        entityManager.remove(najemca);
    }

    @Override
    public Najemca znajdz(UUID najemcaId) {
        List<Najemca> najemcy = entityManager.createQuery("SELECT n FROM Najemca n WHERE n.id = :id", Najemca.class)
                .setParameter("id", najemcaId)
                .getResultList();
        return najemcy.isEmpty() ? null : najemcy.getFirst();
    }

    /**
     * Wyszukuje najemcę po unikalnym loginie.
     */
    public Najemca znajdzLogin(String login) {
        List<Najemca> najemcy = entityManager.createQuery("SELECT n FROM Najemca n WHERE n.login = :login", Najemca.class)
                .setParameter("login", login)
                .getResultList();
        return najemcy.isEmpty() ? null : najemcy.getFirst();
    }
}
