package wypozyczalnia;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import wypozyczalnia.managers.NajemManager;
import wypozyczalnia.managers.NajemcaManager;
import wypozyczalnia.managers.NieruchomoscManager;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;
import wypozyczalnia.objects.nieruchomosc.Dom;

import java.time.LocalDateTime;

/**
 * Główna klasa demonstrująca funkcjonalności wypożyczalni nieruchomości.
 * Testuje transakcje ACID, izolację, optymistyczne blokady i logikę biznesową.
 */
public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf;
        EntityManager em;
        try {
            emf = Persistence.createEntityManagerFactory("WypozyczalniaAut");
        } catch (Exception e) {
            System.out.println("Błąd inicjalizacji JPA/Hibernate: " + e.getMessage());
            throw new RuntimeException("Nie można połączyć się z bazą danych", e);
        }

        em = emf.createEntityManager();
        NieruchomoscManager nieruchomoscManager = new NieruchomoscManager(em);
        NajemcaManager najemcaManager = new NajemcaManager(em);
        NajemManager najemManager = new NajemManager(em, najemcaManager, nieruchomoscManager);

        // Przygotowanie dat testowych
        LocalDateTime dzisiaj = LocalDateTime.now().withNano(0).plusMinutes(1);
        LocalDateTime zaTydzien = dzisiaj.plusWeeks(1);
        LocalDateTime zaDwaTygodnie = dzisiaj.plusWeeks(2);

        // Dodawanie najemców
        Najemca najemcaAktywny = new Najemca("jan_a");
        Najemca najemcaZablokowany = new Najemca("anna_z");
        najemcaZablokowany.setAktywny(false);

        najemcaManager.dodajNajemce(najemcaAktywny);
        najemcaManager.dodajNajemce(najemcaZablokowany);

        // Dodawanie nieruchomości
        Mieszkanie mieszkanie1_Centrum = new Mieszkanie("Kraków", "Stare Miasto", "ul. Floriańska 12", 3, "gazowe", true);
        Mieszkanie mieszkanie2_Kazimierz = new Mieszkanie("Kraków", "Kazimierz", "ul. Szeroka 5", 2, "elektryczne", false);
        Mieszkanie mieszkanie3_Podgorze = new Mieszkanie("Kraków", "Podgórze", "ul. Kalwaryjska 20", 4, "centralne", true);
        Mieszkanie mieszkanie4_Limit = new Mieszkanie("Warszawa", "Mokotów", "ul. Puławska 100 (Limit)", 2, "gazowe", false);

        Dom dom1_Wieliczka = new Dom("Wieliczka", "Centrum", "ul. Kościuszki 15", 500, "jednorodzinny", true);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie1_Centrum);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie2_Kazimierz);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie3_Podgorze);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie4_Limit);
        nieruchomoscManager.dodajNieruchomosc(dom1_Wieliczka);

        System.out.println("\nTEST A: Aktywność Najemcy");
        try {
            najemManager.dokonajNajmu(najemcaZablokowany, mieszkanie1_Centrum, dzisiaj, zaTydzien);
        } catch (IllegalStateException e) {
            System.out.println("BLOKADA KONTA: " + e.getMessage());
        }

        System.out.println("\nTEST B: Limit Najmów (3/3)");

        najemManager.dokonajNajmu(najemcaAktywny, mieszkanie1_Centrum, dzisiaj.plusMinutes(1), zaTydzien);

        najemManager.dokonajNajmu(najemcaAktywny, dom1_Wieliczka, dzisiaj.plusMinutes(2), zaTydzien);

        najemManager.dokonajNajmu(najemcaAktywny, mieszkanie2_Kazimierz, dzisiaj.plusMinutes(3), zaTydzien);

        try {
            System.out.println("\n-> Próba wynajęcia 4. nieruchomości (mieszkanie w Warszawie)");
            najemManager.dokonajNajmu(najemcaAktywny, mieszkanie4_Limit, dzisiaj.plusMinutes(4), zaTydzien);
        } catch (IllegalStateException e) {
            System.out.println("LIMIT Osiągnięty: " + e.getMessage());
        }

        System.out.println("\n-> Zwracamy mieszkanie w centrum, aby zwolnić limit");
        najemManager.zwrocNieruchomosc(najemcaAktywny, mieszkanie1_Centrum);

        System.out.println("-> Ponowna próba wynajęcia mieszkania w Warszawie");
        najemManager.dokonajNajmu(najemcaAktywny, mieszkanie4_Limit, dzisiaj.plusMinutes(5), zaTydzien);

        System.out.println("\n-> Zwrot wszystkich wynajętych nieruchomości");
        najemManager.zwrocNieruchomosc(najemcaAktywny, mieszkanie4_Limit);
        najemManager.zwrocNieruchomosc(najemcaAktywny, mieszkanie2_Kazimierz);
        najemManager.zwrocNieruchomosc(najemcaAktywny, dom1_Wieliczka);

        System.out.println("\nTEST C: Kolizja Czasowa Nieruchomości");

        System.out.println("\n-> Najem bazowy mieszkania w Podgórzu: " + dzisiaj + " - " + zaTydzien);
        najemManager.dokonajNajmu(najemcaAktywny, mieszkanie3_Podgorze, dzisiaj, zaTydzien);

        LocalDateTime kolizjaStart = dzisiaj.plusDays(1);
        LocalDateTime kolizjaKoniec = zaTydzien.minusDays(1);

        try {
            System.out.println("\n-> Próba kolidującego najmu mieszkania w Podgórzu: " + kolizjaStart + " - " + kolizjaKoniec);
            najemManager.dokonajNajmu(najemcaAktywny, mieszkanie3_Podgorze, kolizjaStart, kolizjaKoniec);
        } catch (Exception e) {
            System.out.println("KOLIZJA: " + e.getMessage());
        }

        LocalDateTime bezKolizjiStart = zaTydzien.plusDays(1);
        LocalDateTime bezKolizjiKoniec = zaDwaTygodnie;

        try {
            System.out.println("\n-> Najem niekolidujący mieszkania w Podgórzu: " + bezKolizjiStart + " - " + bezKolizjiKoniec);
            najemManager.dokonajNajmu(najemcaAktywny, mieszkanie3_Podgorze, bezKolizjiStart, bezKolizjiKoniec);
            System.out.println("Zakończono pomyślnie");
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }

        em.close();
        emf.close();
    }
}
