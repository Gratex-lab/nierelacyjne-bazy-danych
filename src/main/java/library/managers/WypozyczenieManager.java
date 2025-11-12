package library.managers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import library.objects.Wypozyczenie;
import library.objects.Klient;
import library.objects.auto.Auto;
import library.repositories.WypozyczenieRepozytorium;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manager obsługujący logikę biznesową wypożyczeń aut. Zapewnia transakcyjność,
 * sprawdzanie limitów i dostępności.
 */
public class WypozyczenieManager {

    private final EntityManager entityManager;
    private final WypozyczenieRepozytorium wypozyczenieRepozytorium;
    private final KlientManager klientManager;
    private final AutoManager autoManager;
    private static final int MAKS_LICZBA_WYPOZYCZEN = 3;

    public WypozyczenieManager(EntityManager entityManager, KlientManager klientManager, AutoManager autoManager) {
        this.entityManager = entityManager;
        this.wypozyczenieRepozytorium = new WypozyczenieRepozytorium(entityManager);
        this.klientManager = klientManager;
        this.autoManager = autoManager;
    }

    /**
     * Dokonuje wypożyczenia auta przez klienta. Sprawdza aktywność klienta,
     * dostępność auta i limit wypożyczeń.
     */
    public void dokonajWypozyczenia(Klient klient, Auto auto, LocalDateTime start, LocalDateTime koniec) {
        Klient zarzadzanyKlient = klientManager.znajdzKlienta(klient.getId());
        Auto zarzadzaneAuto = autoManager.znajdzAuto(auto.getId());

        if (zarzadzanyKlient == null || zarzadzaneAuto == null) {
            throw new IllegalArgumentException("Klient lub auto nie istnieje w bazie.");
        }

        if (!zarzadzanyKlient.czyAktywny()) {
            throw new IllegalStateException("Klient jest nieaktywny.");
        }

        if (autoManager.czyJestZajete(zarzadzaneAuto, start, koniec)) {
            throw new IllegalArgumentException("Auto jest niedostępne w podanym przedziale czasowym.");
        }

        long aktualnaLiczbaWypozyczen = liczAktywneWypozyczenieKlienta(zarzadzanyKlient);
        if (aktualnaLiczbaWypozyczen >= MAKS_LICZBA_WYPOZYCZEN) {
            throw new IllegalStateException("Klient '" + zarzadzanyKlient.getLogin() + "' osiągnął limit wypożyczeń.");
        }

        Wypozyczenie noweWypozyczenie = new Wypozyczenie(zarzadzanyKlient, zarzadzaneAuto, start, koniec);

        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            wypozyczenieRepozytorium.dodaj(noweWypozyczenie);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println("Dokonano wypożyczenia auta: " + zarzadzaneAuto.getPelnaNazwa());
    }

    /**
     * Zwraca wypożyczone auto.
     */
    public void zwrocAuto(Klient klient, Auto auto) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            List<Wypozyczenie> wypozyczenia = wypozyczenieRepozytorium.znajdzKlientowi(auto.getId(), klient.getId());
            if (wypozyczenia.isEmpty()) {
                throw new RuntimeException("Klient nie ma aktywnego wypożyczenia dla auta: " + auto.getPelnaNazwa());
            }
            Wypozyczenie wypozyczenieDoZwrotu = wypozyczenia.getFirst();
            wypozyczenieRepozytorium.usun(wypozyczenieDoZwrotu);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println("Zwrócono auto '" + auto.getPelnaNazwa() + "'. Limit zwolniony.");
    }

    /**
     * Liczy aktywne wypożyczenia klienta.
     */
    private long liczAktywneWypozyczenieKlienta(Klient klient) {
        LocalDateTime teraz = LocalDateTime.now();
        return entityManager.createQuery(
                "SELECT COUNT(w) FROM Wypozyczenie w WHERE w.klient = :klient AND w.dataZakonczenia > :teraz", Long.class)
                .setParameter("klient", klient)
                .setParameter("teraz", teraz)
                .getSingleResult();
    }
}
