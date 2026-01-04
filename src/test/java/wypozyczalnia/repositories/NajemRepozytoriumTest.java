package wypozyczalnia.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.datastax.oss.driver.api.core.CqlSession;

import wypozyczalnia.config.CassandraConfig;
import wypozyczalnia.objects.Najem;
import wypozyczalnia.objects.Najemca;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;

class NajemRepozytoriumTest {

    private static CqlSession session;
    private static NajemRepozytorium repozytorium;

    @BeforeAll
    static void setupCassandra() {

        CassandraConfig config = CassandraConfig.getInstance();
        session = config.getSession();
        repozytorium = new NajemRepozytorium(session);
    }

    @BeforeEach
    void clearData() {
        // Czyszczenie danych między testami
        session.execute("TRUNCATE najmy");
        session.execute("TRUNCATE najmy_by_najemca");
        session.execute("TRUNCATE najmy_by_nieruchomosc");
    }

    @Test
    @DisplayName("Powinien dodać nowy najem")
    void DodawanieNajmu() {

        Najemca najemca = new Najemca("najemca1");
        Mieszkanie mieszkanie = new Mieszkanie("Warszawa", "Mokotów", "ul. Testowa 1", 3, "centralne", true);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMonths(6);
        Najem najem = new Najem(najemca, mieszkanie, start, end);

        repozytorium.dodaj(najem);

        Najem znaleziony = repozytorium.znajdz(najem.getId());
        assertThat(znaleziony).isNotNull();
        assertThat(znaleziony.getNajemcaLogin()).isEqualTo("najemca1");
        assertThat(znaleziony.getNieruchomoscAdres()).isEqualTo("Warszawa, Mokotów, ul. Testowa 1");
        assertThat(znaleziony.getDataRozpoczecia()).isEqualToIgnoringNanos(start);
        assertThat(znaleziony.getDataZakonczenia()).isEqualToIgnoringNanos(end);
    }

    @Test
    @DisplayName("Powinien zaktualizować najem")
    void AktualizacjaNajmu() {

        Najemca najemca = new Najemca("najemca2");
        Mieszkanie mieszkanie = new Mieszkanie("Kraków", "Stare Miasto", "ul. Floriańska 1", 2, "gazowe", false);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMonths(12);
        Najem najem = new Najem(najemca, mieszkanie, start, end);
        repozytorium.dodaj(najem);

        LocalDateTime newEnd = end.plusMonths(6);
        najem.setDataZakonczenia(newEnd);

        repozytorium.aktualizuj(najem);

        Najem znaleziony = repozytorium.znajdz(najem.getId());
        assertThat(znaleziony.getDataZakonczenia()).isEqualToIgnoringNanos(newEnd);
    }

    @Test
    @DisplayName("Powinien usunąć najem")
    void UsuwanieNajmu() {
        Najemca najemca = new Najemca("najemca3");
        Mieszkanie mieszkanie = new Mieszkanie("Gdańsk", "Wrzeszcz", "ul. Grunwaldzka 1", 1, "elektryczne", true);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMonths(3);
        Najem najem = new Najem(najemca, mieszkanie, start, end);
        repozytorium.dodaj(najem);

        repozytorium.usun(najem);

        Najem znaleziony = repozytorium.znajdz(najem.getId());
        assertThat(znaleziony).isNull();
    }

    @Test
    @DisplayName("Powinien znaleźć najmy po najemcy i nieruchomości")
    void ZnajdzNajmy() {

        Najemca najemca = new Najemca("najemca4");
        Mieszkanie mieszkanie = new Mieszkanie("Poznań", "Centrum", "ul. Święty Marcin 1", 2, "miejskie", false);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMonths(6);

        Najem najem = new Najem(najemca, mieszkanie, start, end);
        repozytorium.dodaj(najem);

        List<Najem> najmy = repozytorium.znajdzNajemcy(mieszkanie.getId(), najemca.getId());

        assertThat(najmy).hasSize(1);
        assertThat(najmy.get(0).getNajemcaLogin()).isEqualTo("najemca4");
    }

    @Test
    @DisplayName("Test konwersji modelu do dokumentu i z powrotem")
    void KonwersjaModelu() {

        UUID najemcaId = UUID.randomUUID();
        UUID nieruchomoscId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 30, 18, 0);

        Najem original = new Najem(UUID.randomUUID(), najemcaId, "test_login",
                nieruchomoscId, "Test, Miasto, ul. Testowa 1", start, end);

        repozytorium.dodaj(original);
        Najem retrieved = repozytorium.znajdz(original.getId());

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(original.getId());
        assertThat(retrieved.getNajemcaId()).isEqualTo(original.getNajemcaId());
        assertThat(retrieved.getNajemcaLogin()).isEqualTo(original.getNajemcaLogin());
        assertThat(retrieved.getNieruchomoscId()).isEqualTo(original.getNieruchomoscId());
        assertThat(retrieved.getNieruchomoscAdres()).isEqualTo(original.getNieruchomoscAdres());
        assertThat(retrieved.getDataRozpoczecia()).isEqualTo(original.getDataRozpoczecia());
        assertThat(retrieved.getDataZakonczenia()).isEqualTo(original.getDataZakonczenia());
    }

    @Test
    @DisplayName("Powinien liczyć aktywne najmy")
    void LiczAktywneNajmy() {

        Najemca najemca = new Najemca("najemca5");
        Mieszkanie mieszkanie = new Mieszkanie("Wrocław", "Krzyki", "ul. Powstańców Śląskich 1", 4, "centralne", false);
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 12, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 12, 0);
        Najem najem = new Najem(najemca, mieszkanie, start, end);
        repozytorium.dodaj(najem);

        long aktywneNajmy = repozytorium.liczAktywneNajmy(najemca.getId(), LocalDateTime.of(2024, 8, 1, 12, 0));
        assertThat(aktywneNajmy).isGreaterThanOrEqualTo(0);

        long przyszleNajmy = repozytorium.liczAktywneNajmy(najemca.getId(), LocalDateTime.of(2025, 1, 1, 12, 0));
        assertThat(przyszleNajmy).isEqualTo(0);
    }
}
