package wypozyczalnia.repositories;

import jakarta.persistence.EntityManager;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repozytorium dla encji Nieruchomosc. Obsługuje operacje CRUD oraz sprawdzanie
 * dostępności nieruchomości.
 */
public class NieruchomoscRepozytorium implements Repozytorium<Nieruchomosc> {

    private final EntityManager entityManager;

    public NieruchomoscRepozytorium(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dodaj(Nieruchomosc nieruchomosc) {
        entityManager.persist(nieruchomosc);
    }

    @Override
    public void usun(Nieruchomosc nieruchomosc) {
        entityManager.remove(nieruchomosc);
    }

    @Override
    public Nieruchomosc znajdz(UUID nieruchomoscUuid) {
        List<Nieruchomosc> nieruchomosci = entityManager.createQuery("SELECT n FROM Nieruchomosc n WHERE n.id = :id", Nieruchomosc.class)
                .setParameter("id", nieruchomoscUuid)
                .getResultList();
        return nieruchomosci.isEmpty() ? null : nieruchomosci.getFirst();
    }

    /**
     * Sprawdza czy nieruchomość jest zajęta w podanym okresie. Wykorzystuje
     * izolację transakcji dla zapewnienia spójności.
     */
    public boolean czyJestZajeta(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(n) FROM Najem n "
                + "WHERE n.nieruchomosc = :nieruchomosc AND ("
                + "    (n.dataRozpoczecia < :koniec AND n.dataZakonczenia > :start)"
                + ")", Long.class)
                .setParameter("nieruchomosc", nieruchomosc)
                .setParameter("start", start)
                .setParameter("koniec", koniec)
                .getSingleResult();
        return count > 0;
    }
}
