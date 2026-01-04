package wypozyczalnia.managers;

import java.time.LocalDateTime;
import java.util.List;

import com.datastax.oss.driver.api.core.CqlSession;

import wypozyczalnia.objects.Najem;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;
import wypozyczalnia.repositories.NajemRepozytorium;

/**
 * Manager obsługujący logikę biznesową najmu nieruchomości.
 */
public class NajemManager {

    private final NajemRepozytorium najemRepozytorium;
    private final NajemcaManager najemcaManager;
    private final NieruchomoscManager nieruchomoscManager;
    private static final int MAKS_LICZBA_NAJMOW = 3;

    public NajemManager(CqlSession session, NajemcaManager najemcaManager, NieruchomoscManager nieruchomoscManager) {
        this.najemRepozytorium = new NajemRepozytorium(session);
        this.najemcaManager = najemcaManager;
        this.nieruchomoscManager = nieruchomoscManager;
    }

    /**
     * Dokonuje najmu nieruchomości przez najemcę. Sprawdza aktywność najemcy,
     * dostępność nieruchomości i limit najmów.
     */
    public void dokonajNajmu(Najemca najemca, Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        try {
            // Pobranie aktualnych danych z bazy
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

            long aktualnaLiczbaNajmow = liczAktywneNajmy(zarzadzanyNajemca);
            if (aktualnaLiczbaNajmow >= MAKS_LICZBA_NAJMOW) {
                throw new IllegalStateException("Najemca '" + zarzadzanyNajemca.getLogin() + "' osiągnął limit najmów.");
            }

            Najem nowyNajem = new Najem(zarzadzanyNajemca, zarzadzanaNieruchomosc, start, koniec);
            najemRepozytorium.dodaj(nowyNajem);

            System.out.println("Dokonano najmu nieruchomości: " + zarzadzanaNieruchomosc.getPelnyAdres());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Błąd podczas najmu: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Nieoczekiwany błąd podczas najmu: " + e.getMessage());
            throw new RuntimeException("Nie udało się dokonać najmu", e);
        }
    }

    /**
     * Zwraca wynajętą nieruchomość.
     */
    public void zwrocNieruchomosc(Najemca najemca, Nieruchomosc nieruchomosc) {
        try {
            List<Najem> najmy = najemRepozytorium.znajdzNajemcy(nieruchomosc.getId(), najemca.getId());
            if (najmy.isEmpty()) {
                throw new RuntimeException("Najemca nie ma aktywnego najmu dla nieruchomości: " + nieruchomosc.getPelnyAdres());
            }

            Najem najemDoZwrotu = najmy.getFirst();
            najemRepozytorium.usun(najemDoZwrotu);

            System.out.println("Zwrócono nieruchomość '" + nieruchomosc.getPelnyAdres() + "'. Limit zwolniony.");
        } catch (RuntimeException e) {
            System.err.println("Błąd podczas zwrotu: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Nieoczekiwany błąd podczas zwrotu: " + e.getMessage());
            throw new RuntimeException("Nie udało się zwrócić nieruchomości", e);
        }
    }

    /**
     * Liczy aktywne najmy.
     */
    private long liczAktywneNajmy(Najemca najemca) {
        try {
            LocalDateTime teraz = LocalDateTime.now();
            return najemRepozytorium.liczAktywneNajmy(najemca.getId(), teraz);
        } catch (Exception e) {
            System.err.println("Błąd podczas liczenia aktywnych najmów: " + e.getMessage());
            throw new RuntimeException("Nie udało się policzyć aktywnych najmów", e);
        }
    }

    /**
     * Znajduje najem po jego UUID.
     */
    public Najem znajdzNajem(java.util.UUID najemUuid) {
        try {
            return najemRepozytorium.znajdz(najemUuid);
        } catch (Exception e) {
            System.err.println("Błąd podczas wyszukiwania najmu: " + e.getMessage());
            throw new RuntimeException("Nie udało się wyszukać najmu", e);
        }
    }

    /**
     * Znajduje najmy dla najemcy i nieruchomości.
     */
    public List<Najem> znajdzNajmy(java.util.UUID najemcaId, java.util.UUID nieruchomoscId) {
        try {
            return najemRepozytorium.znajdzNajemcy(nieruchomoscId, najemcaId);
        } catch (Exception e) {
            System.err.println("Błąd podczas wyszukiwania najmów najemcy: " + e.getMessage());
            throw new RuntimeException("Nie udało się wyszukać najmów najemcy", e);
        }
    }

    /**
     * Aktualizuje najem.
     */
    public void aktualizujNajem(Najem najem) {
        try {
            najemRepozytorium.aktualizuj(najem);
            System.out.println("Zaktualizowano najem: " + najem.getId());
        } catch (Exception e) {
            System.err.println("Błąd podczas aktualizacji najmu: " + e.getMessage());
            throw new RuntimeException("Nie udało się zaktualizować najmu", e);
        }
    }
}
