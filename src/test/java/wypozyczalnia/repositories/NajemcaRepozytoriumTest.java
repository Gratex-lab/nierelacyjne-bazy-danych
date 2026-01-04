package wypozyczalnia.repositories;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.datastax.oss.driver.api.core.CqlSession;

import wypozyczalnia.config.CassandraConfig;
import wypozyczalnia.objects.Najemca;

class NajemcaRepozytoriumTest {

    private static CqlSession session;
    private static NajemcaRepozytorium repozytorium;

    @BeforeAll
    static void setupCassandra() {
        CassandraConfig config = CassandraConfig.getInstance();
        session = config.getSession();
        repozytorium = new NajemcaRepozytorium(session);
    }

    @BeforeEach
    void clearData() {
        // Czyszczenie danych między testami
        session.execute("TRUNCATE najemcy");
        session.execute("TRUNCATE najemcy_by_login");
    }

    @Test
    @DisplayName("Powinien dodać nowego najemcę")
    void DodajNajemce() {
        Najemca najemca = new Najemca("testuser");

        repozytorium.dodaj(najemca);

        Najemca znaleziony = repozytorium.znajdz(najemca.getId());
        assertThat(znaleziony).isNotNull();
        assertThat(znaleziony.getLogin()).isEqualTo("testuser");
        assertThat(znaleziony.czyAktywny()).isTrue();
    }

    @Test
    @DisplayName("Powinien rzucić wyjątek przy dodawaniu najemcy z istniejącym loginem")
    void TestRzuceniaWyjatku() {

        Najemca najemca1 = new Najemca("duplicate");
        Najemca najemca2 = new Najemca("duplicate");
        repozytorium.dodaj(najemca1);

        assertThatThrownBy(() -> repozytorium.dodaj(najemca2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("już istnieje");
    }

    @Test
    @DisplayName("Powinien zaktualizować najemcę")
    void AktualizacjaNajemcy() {
        Najemca najemca = new Najemca("updatetest");
        repozytorium.dodaj(najemca);
        najemca.setAktywny(false);

        repozytorium.aktualizuj(najemca);

        Najemca znaleziony = repozytorium.znajdz(najemca.getId());
        assertThat(znaleziony.czyAktywny()).isFalse();
    }

    @Test
    @DisplayName("Powinien usunąć najemcę")
    void UsuwanieNajemcy() {

        Najemca najemca = new Najemca("deletetest");
        repozytorium.dodaj(najemca);

        repozytorium.usun(najemca);

        Najemca znaleziony = repozytorium.znajdz(najemca.getId());
        assertThat(znaleziony).isNull();
    }

    @Test
    @DisplayName("Powinien znaleźć najemcę po loginie")
    void ZnajdzLogin() {

        Najemca najemca = new Najemca("logintest");
        repozytorium.dodaj(najemca);

        Najemca znaleziony = repozytorium.znajdzLogin("logintest");

        assertThat(znaleziony).isNotNull();
        assertThat(znaleziony.getId()).isEqualTo(najemca.getId());
    }

    @Test
    @DisplayName("Powinien zwrócić null dla nieistniejącego UUID")
    void TestZwracaniaNulla() {

        Najemca znaleziony = repozytorium.znajdz(UUID.randomUUID());

        assertThat(znaleziony).isNull();
    }

    @Test
    @DisplayName("Powinien zwrócić null dla nieistniejącego loginu")
    void TestZwracaniaNulla2() {

        Najemca znaleziony = repozytorium.znajdzLogin("nonexistent");

        assertThat(znaleziony).isNull();
    }

    @Test
    @DisplayName("Test konwersji modelu do dokumentu i z powrotem")
    void KonwersjaModelu() {

        UUID testId = UUID.randomUUID();
        Najemca original = new Najemca(testId, "datatest", false);

        repozytorium.dodaj(original);
        Najemca retrieved = repozytorium.znajdz(testId);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(original.getId());
        assertThat(retrieved.getLogin()).isEqualTo(original.getLogin());
        assertThat(retrieved.czyAktywny()).isEqualTo(original.czyAktywny());
    }
}
