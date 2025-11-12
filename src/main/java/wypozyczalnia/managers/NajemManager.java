package wypozyczalnia.managers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import wypozyczalnia.objects.Najem;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;
import wypozyczalnia.repositories.NajemRepozytorium;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manager obsługujący logikę biznesową najmu nieruchomości. Zapewnia
 * transakcyjność, sprawdzanie limitów i dostępności.
 */
public class NajemManager {

    private final EntityManager entityManager;
    private final NajemRepozytorium najemRepozytorium;
    private final NajemcaManager najemcaManager;
    private final NieruchomoscManager nieruchomoscManager;
    private static final int MAKS_LICZBA_NAJMOW = 3;

    public NajemManager(EntityManager entityManager, NajemcaManager najemcaManager, NieruchomoscManager nieruchomoscManager) {
        this.entityManager = entityManager;
        this.najemRepozytorium = new NajemRepozytorium(entityManager);
        this.najemcaManager = najemcaManager;
        this.nieruchomoscManager = nieruchomoscManager;
    }

    /**
     * Dokonuje najmu nieruchomości przez najemcę. Sprawdza aktywność najemcy,
     * dostępność nieruchomości i limit najmów.
     */
    public void dokonajNajmu(Najemca najemca, Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        Najemca zarzadzanyNajemca = najemcaManager.znajdzNajemce(najemca.getId());
        Nieruchomosc zarzadzanaNieruchomosc = nieruchomoscManager.znajdzNieruchomosc(nieruchomosc.getId());

        if (zarzadzanyNajemca == null || zarzadzanaNieruchomosc == null) {
            throw new IllegalArgumentException("Najemca lub nieruchomość nie istnieje w bazie.");
        }

        if (!zarzadzanyNajemca.czyAktywny()) {
            throw new IllegalStateException("Najemca jest nieaktywny.");
        }

        if (nieruchomoscManager.czyJestZajeta(zarzadzanaNieruchomosc, start, koniec)) {
            throw new IllegalArgumentException("Nieruchomość jest niedostępna w podanym przedziale czasowym.");
        }

        long aktualnaLiczbaNajmow = liczAktywneNajmyNajemcy(zarzadzanyNajemca);
        if (aktualnaLiczbaNajmow >= MAKS_LICZBA_NAJMOW) {
            throw new IllegalStateException("Najemca '" + zarzadzanyNajemca.getLogin() + "' osiągnął limit najmów.");
        }

        Najem nowyNajem = new Najem(zarzadzanyNajemca, zarzadzanaNieruchomosc, start, koniec);

        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            najemRepozytorium.dodaj(nowyNajem);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println("Dokonano najmu nieruchomości: " + zarzadzanaNieruchomosc.getPelnyAdres());
    }

    /**
     * Zwraca wynajętą nieruchomość.
     */
    public void zwrocNieruchomosc(Najemca najemca, Nieruchomosc nieruchomosc) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            List<Najem> najmy = najemRepozytorium.znajdzNajemcowi(nieruchomosc.getId(), najemca.getId());
            if (najmy.isEmpty()) {
                throw new RuntimeException("Najemca nie ma aktywnego najmu dla nieruchomości: " + nieruchomosc.getPelnyAdres());
            }
            Najem najemDoZwrotu = najmy.getFirst();
            najemRepozytorium.usun(najemDoZwrotu);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println("Zwrócono nieruchomość '" + nieruchomosc.getPelnyAdres() + "'. Limit zwolniony.");
    }

    /**
     * Liczy aktywne najmy najemcy.
     */
    private long liczAktywneNajmyNajemcy(Najemca najemca) {
        LocalDateTime teraz = LocalDateTime.now();
        return entityManager.createQuery(
                "SELECT COUNT(n) FROM Najem n WHERE n.najemca = :najemca AND n.dataZakonczenia > :teraz", Long.class)
                .setParameter("najemca", najemca)
                .setParameter("teraz", teraz)
                .getSingleResult();
    }
}
