package library.repositories;

import jakarta.persistence.EntityManager;
import library.objects.auto.Auto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repozytorium dla encji Auto. Obsługuje operacje CRUD oraz sprawdzanie
 * dostępności aut.
 */
public class AutoRepozytorium implements Repozytorium<Auto> {

    private final EntityManager entityManager;

    public AutoRepozytorium(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dodaj(Auto auto) {
        entityManager.persist(auto);
    }

    @Override
    public void usun(Auto auto) {
        entityManager.remove(auto);
    }

    @Override
    public Auto znajdz(UUID autoUuid) {
        List<Auto> auta = entityManager.createQuery("SELECT a FROM Auto a WHERE a.id = :id", Auto.class)
                .setParameter("id", autoUuid)
                .getResultList();
        return auta.isEmpty() ? null : auta.getFirst();
    }

    /**
     * Sprawdza czy auto jest zajęte w podanym okresie. Wykorzystuje izolację
     * transakcji dla zapewnienia spójności.
     */
    public boolean czyJestZajety(Auto auto, LocalDateTime start, LocalDateTime koniec) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(w) FROM Wypozyczenie w "
                + "WHERE w.auto = :auto AND ("
                + "    (w.dataRozpoczecia < :koniec AND w.dataZakonczenia > :start)"
                + ")", Long.class)
                .setParameter("auto", auto)
                .setParameter("start", start)
                .setParameter("koniec", koniec)
                .getSingleResult();
        return count > 0;
    }
}
