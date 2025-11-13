package wypozyczalnia.managers;

import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.repositories.NajemcaRepozytorium;

/**
 * Manager obsługujący logikę biznesową zarządzania najemcami wypożyczalni.
 * Zapewnia unikalność loginów i spójność operacji w MongoDB.
 */
public class NajemcaManager {

    private final NajemcaRepozytorium najemcaRepozytorium;

    public NajemcaManager(MongoDatabase database) {
        this.najemcaRepozytorium = new NajemcaRepozytorium(database);
    }

    /**
     * Dodaje nowego najemcę do systemu. Sprawdza unikalność loginu.
     */
    public void dodajNajemce(Najemca najemca) {
        try {
            Najemca istniejacy = najemcaRepozytorium.znajdzLogin(najemca.getLogin());
            if (istniejacy != null) {
                throw new IllegalArgumentException("Najemca z loginem '" + najemca.getLogin() + "' już istnieje.");
            }
            najemcaRepozytorium.dodaj(najemca);
            System.out.println("Dodano najemcę: " + najemca.getLogin());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas dodawania najemcy: " + e.getMessage(), e);
        }
    }

    /**
     * Znajduje najemcę po jego ObjectId.
     */
    public Najemca znajdzNajemce(ObjectId najemcaId) {
        try {
            Najemca najemca = najemcaRepozytorium.znajdz(najemcaId);
            if (najemca == null) {
                throw new IllegalArgumentException("Najemca o ID '" + najemcaId + "' nie istnieje.");
            }
            return najemca;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas wyszukiwania najemcy: " + e.getMessage(), e);
        }
    }

    /**
     * Aktualizuje najemcę w systemie.
     */
    public void aktualizujNajemce(Najemca najemca) {
        try {
            najemcaRepozytorium.aktualizuj(najemca);
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas aktualizacji najemcy: " + e.getMessage(), e);
        }
    }
}
