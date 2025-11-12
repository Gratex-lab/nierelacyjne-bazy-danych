package wypozyczalnia.repositories;

import jakarta.persistence.EntityManager;
import wypozyczalnia.objects.Najem;

import java.util.List;
import java.util.UUID;

/**
 * Repozytorium dla encji Najem. Obsługuje operacje CRUD oraz zapytania
 * biznesowe.
 */
public class NajemRepozytorium implements Repozytorium<Najem> {

    private final EntityManager entityManager;

    public NajemRepozytorium(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dodaj(Najem najem) {
        entityManager.persist(najem);
    }

    @Override
    public void usun(Najem najem) {
        entityManager.remove(najem);
    }

    @Override
    public Najem znajdz(UUID najemUuid) {
        List<Najem> najmy = entityManager.createQuery(
                "SELECT n FROM Najem n WHERE n.id = :id", Najem.class)
                .setParameter("id", najemUuid)
                .getResultList();
        return najmy.isEmpty() ? null : najmy.getFirst();
    }

    /**
     * Znajduje najmy dla danego najemcy i nieruchomości.
     */
    public List<Najem> znajdzNajemcowi(UUID nieruchomoscId, UUID najemcaUuid) {
        return entityManager.createQuery(
                "SELECT n FROM Najem n WHERE n.najemca.id = :najemcaId AND n.nieruchomosc.id = :nieruchomoscId",
                Najem.class)
                .setParameter("najemcaId", najemcaUuid)
                .setParameter("nieruchomoscId", nieruchomoscId)
                .getResultList();
    }
}
