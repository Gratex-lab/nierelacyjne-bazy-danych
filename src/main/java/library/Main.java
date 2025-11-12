package library;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import library.managers.WypozyczenieManager;
import library.managers.KlientManager;
import library.managers.AutoManager;
import library.objects.Klient;
import library.objects.auto.SamochodOsobowy;
import library.objects.auto.SamochodCiezarowy;

import java.time.LocalDateTime;

/**
 * Główna klasa demonstrująca funkcjonalności wypożyczalni aut. Testuje
 * transakcje ACID, izolację, optymistyczne blokady i logikę biznesową.
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
        AutoManager autoManager = new AutoManager(em);
        KlientManager klientManager = new KlientManager(em);
        WypozyczenieManager wypozyczenieManager = new WypozyczenieManager(em, klientManager, autoManager);

        // Przygotowanie dat testowych
        LocalDateTime dzisiaj = LocalDateTime.now().withNano(0).plusMinutes(1);
        LocalDateTime zaTydzien = dzisiaj.plusWeeks(1);
        LocalDateTime zaDwaTygodnie = dzisiaj.plusWeeks(2);

        // Dodawanie klientów
        Klient klientAktywny = new Klient("jan_a");
        Klient klientZablokowany = new Klient("anna_z");
        klientZablokowany.setAktywny(false);

        klientManager.dodajKlienta(klientAktywny);
        klientManager.dodajKlienta(klientZablokowany);

        // Dodawanie samochodów
        SamochodOsobowy auto1_Toyota = new SamochodOsobowy("Toyota", "Corolla", "KR12345", 5, "benzyna", false);
        SamochodOsobowy auto2_BMW = new SamochodOsobowy("BMW", "320i", "KR54321", 5, "benzyna", true);
        SamochodOsobowy auto3_Audi = new SamochodOsobowy("Audi", "A4", "KR67890", 5, "diesel", true);
        SamochodOsobowy auto4_Limit = new SamochodOsobowy("Ford", "Focus (Limit)", "KR99999", 5, "benzyna", false);

        SamochodCiezarowy ciezarowka1 = new SamochodCiezarowy("Mercedes", "Sprinter", "KT11111", 3500, "C", false);

        autoManager.dodajAuto(auto1_Toyota);
        autoManager.dodajAuto(auto2_BMW);
        autoManager.dodajAuto(auto3_Audi);
        autoManager.dodajAuto(auto4_Limit);
        autoManager.dodajAuto(ciezarowka1);

        System.out.println("\nTEST A: Aktywność Klienta");
        try {
            wypozyczenieManager.dokonajWypozyczenia(klientZablokowany, auto1_Toyota, dzisiaj, zaTydzien);
        } catch (IllegalStateException e) {
            System.out.println("BLOKADA KONTA: " + e.getMessage());
        }

        System.out.println("\nTEST B: Limit Wypożyczeń (3/3)");

        wypozyczenieManager.dokonajWypozyczenia(klientAktywny, auto1_Toyota, dzisiaj.plusMinutes(1), zaTydzien);

        wypozyczenieManager.dokonajWypozyczenia(klientAktywny, ciezarowka1, dzisiaj.plusMinutes(2), zaTydzien);

        wypozyczenieManager.dokonajWypozyczenia(klientAktywny, auto2_BMW, dzisiaj.plusMinutes(3), zaTydzien);

        try {
            System.out.println("\n-> Próba wypożyczenia 4. auta (Ford Focus)");
            wypozyczenieManager.dokonajWypozyczenia(klientAktywny, auto4_Limit, dzisiaj.plusMinutes(4), zaTydzien);
        } catch (IllegalStateException e) {
            System.out.println("LIMIT Osiągnięty: " + e.getMessage());
        }

        System.out.println("\n-> Zwracamy Toyotę Corollę, aby zwolnić limit");
        wypozyczenieManager.zwrocAuto(klientAktywny, auto1_Toyota);

        System.out.println("-> Ponowna próba wypożyczenia Forda Focus");
        wypozyczenieManager.dokonajWypozyczenia(klientAktywny, auto4_Limit, dzisiaj.plusMinutes(5), zaTydzien);

        System.out.println("\n-> Zwrot wszystkich wypożyczonych aut");
        wypozyczenieManager.zwrocAuto(klientAktywny, auto4_Limit);
        wypozyczenieManager.zwrocAuto(klientAktywny, auto2_BMW);
        wypozyczenieManager.zwrocAuto(klientAktywny, ciezarowka1);

        System.out.println("\nTEST C: Kolizja Czasowa Auta");

        System.out.println("\n-> Wypożyczenie bazowe Audi A4: " + dzisiaj + " - " + zaTydzien);
        wypozyczenieManager.dokonajWypozyczenia(klientAktywny, auto3_Audi, dzisiaj, zaTydzien);

        LocalDateTime kolizjaStart = dzisiaj.plusDays(1);
        LocalDateTime kolizjaKoniec = zaTydzien.minusDays(1);

        try {
            System.out.println("\n-> Próba kolidującego wypożyczenia Audi A4: " + kolizjaStart + " - " + kolizjaKoniec);
            wypozyczenieManager.dokonajWypozyczenia(klientAktywny, auto3_Audi, kolizjaStart, kolizjaKoniec);
        } catch (Exception e) {
            System.out.println("KOLIZJA: " + e.getMessage());
        }

        LocalDateTime bezKolizjiStart = zaTydzien.plusDays(1);
        LocalDateTime bezKolizjiKoniec = zaDwaTygodnie;

        try {
            System.out.println("\n-> Wypożyczenie niekolidujące Audi A4: " + bezKolizjiStart + " - " + bezKolizjiKoniec);
            wypozyczenieManager.dokonajWypozyczenia(klientAktywny, auto3_Audi, bezKolizjiStart, bezKolizjiKoniec);
            System.out.println("Zakończono pomyślnie");
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }

        em.close();
        emf.close();
    }
}
