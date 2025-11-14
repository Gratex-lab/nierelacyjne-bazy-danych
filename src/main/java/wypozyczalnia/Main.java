package wypozyczalnia;

import com.mongodb.client.MongoDatabase;
import wypozyczalnia.config.MongoConfig;
import wypozyczalnia.managers.NajemcaManager;
import wypozyczalnia.managers.NieruchomoscManager;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;
import wypozyczalnia.objects.nieruchomosc.Dom;

/**
 * Główna klasa inicjalizująca dane w wypożyczalni nieruchomości. Dodaje
 * przykładowych najemców i nieruchomości do bazy MongoDB.
 */
public class Main {

    public static void main(String[] args) {
        MongoDatabase database;
        try {
            database = MongoConfig.getDatabase();
            System.out.println("Połączono z MongoDB.");
        } catch (Exception e) {
            System.err.println("Błąd połączenia z MongoDB: " + e.getMessage());
            return;
        }

        NieruchomoscManager nieruchomoscManager = new NieruchomoscManager(database);
        NajemcaManager najemcaManager = new NajemcaManager(database);

        System.out.println("Dodawanie najemców...");

        // Dodawanie najemców
        Najemca najemca1 = new Najemca("jan_kowalski");
        Najemca najemca2 = new Najemca("anna_nowak");
        Najemca najemca3 = new Najemca("piotr_wisniewski");

        // Jeden nieaktywny najemca
        Najemca najemcaNieaktywny = new Najemca("maria_zablokowana");
        najemcaNieaktywny.setActive(false);

        najemcaManager.dodajNajemce(najemca1);
        najemcaManager.dodajNajemce(najemca2);
        najemcaManager.dodajNajemce(najemca3);
        najemcaManager.dodajNajemce(najemcaNieaktywny);

        System.out.println("Dodano 4 najemców (3 aktywnych, 1 nieaktywny).");

        System.out.println("Dodawanie nieruchomości...");

        // Dodawanie nieruchomości - mieszkania
        Mieszkanie mieszkanie1 = new Mieszkanie("Kraków", "Stare Miasto", "ul. Floriańska 12", 3, "gazowe", true);
        Mieszkanie mieszkanie2 = new Mieszkanie("Kraków", "Kazimierz", "ul. Szeroka 5", 2, "elektryczne", false);
        Mieszkanie mieszkanie3 = new Mieszkanie("Kraków", "Podgórze", "ul. Kalwaryjska 20", 4, "centralne", true);
        Mieszkanie mieszkanie4 = new Mieszkanie("Warszawa", "Mokotów", "ul. Puławska 100", 2, "gazowe", false);
        Mieszkanie mieszkanie5 = new Mieszkanie("Gdańsk", "Śródmieście", "ul. Długa 15", 3, "centralne", true);

        // Dodawanie nieruchomości - domy
        Dom dom1 = new Dom("Wieliczka", "Centrum", "ul. Kościuszki 15", 500, "jednorodzinny", true);
        Dom dom2 = new Dom("Zakopane", "Centrum", "ul. Krupówki 30", 300, "letniskowy", false);
        Dom dom3 = new Dom("Krynica", "Uzdrowisko", "ul. Zdrojowa 8", 250, "letniskowy", true);

        nieruchomoscManager.dodajNieruchomosc(mieszkanie1);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie2);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie3);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie4);
        nieruchomoscManager.dodajNieruchomosc(mieszkanie5);
        nieruchomoscManager.dodajNieruchomosc(dom1);
        nieruchomoscManager.dodajNieruchomosc(dom2);
        nieruchomoscManager.dodajNieruchomosc(dom3);

        System.out.println("Dodano 8 nieruchomości (5 mieszkań, 3 domy).");
        System.out.println("Inicjalizacja danych zakończona pomyślnie!");

        // Zamknięcie połączenia
        try {
            MongoConfig.closeConnection();
            System.out.println("Połączenie z MongoDB zostało zamknięte.");
        } catch (Exception e) {
            System.err.println("Błąd podczas zamykania połączenia: " + e.getMessage());
        }
    }
}
