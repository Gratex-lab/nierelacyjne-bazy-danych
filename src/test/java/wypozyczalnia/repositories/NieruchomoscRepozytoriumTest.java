package wypozyczalnia.repositories;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.datastax.oss.driver.api.core.CqlSession;

import wypozyczalnia.config.CassandraConfig;
import wypozyczalnia.objects.nieruchomosc.Dom;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;

class NieruchomoscRepozytoriumTest {

    private static CqlSession session;
    private static NieruchomoscRepozytorium repozytorium;

    @BeforeAll
    static void setupCassandra() {

        CassandraConfig config = CassandraConfig.getInstance();
        session = config.getSession();
        repozytorium = new NieruchomoscRepozytorium(session);
    }

    @BeforeEach
    void clearData() {
        // Czyszczenie danych między testami
        session.execute("TRUNCATE nieruchomosci");
        session.execute("TRUNCATE najmy_by_nieruchomosc");
    }

    @Test
    @DisplayName("Powinien dodać nowe mieszkanie")
    void DodawanieMieszkania() {

        Mieszkanie mieszkanie = new Mieszkanie("Warszawa", "Śródmieście", "ul. Nowy Świat 1",
                3, "centralne", true);

        repozytorium.dodaj(mieszkanie);

        Nieruchomosc znaleziona = repozytorium.znajdz(mieszkanie.getId());
        assertThat(znaleziona).isNotNull();
        assertThat(znaleziona).isInstanceOf(Mieszkanie.class);

        Mieszkanie znalezione = (Mieszkanie) znaleziona;
        assertThat(znalezione.getMiasto()).isEqualTo("Warszawa");
        assertThat(znalezione.getDzielnica()).isEqualTo("Śródmieście");
        assertThat(znalezione.getAdres()).isEqualTo("ul. Nowy Świat 1");
        assertThat(znalezione.getLiczbaPokoi()).isEqualTo(3);
        assertThat(znalezione.getTypOgrzewania()).isEqualTo("centralne");
        assertThat(znalezione.isCzyUmeblowane()).isTrue();
        assertThat(znalezione.getTypNieruchomosci()).isEqualTo("MIESZKANIE");
    }

    @Test
    @DisplayName("Powinien dodać nowy dom")
    void DodawanieDomu() {

        Dom dom = new Dom("Kraków", "Podgórze", "ul. Kalwaryjska 10",
                500, "jednorodzinny", true);

        repozytorium.dodaj(dom);

        Nieruchomosc znaleziona = repozytorium.znajdz(dom.getId());
        assertThat(znaleziona).isNotNull();
        assertThat(znaleziona).isInstanceOf(Dom.class);

        Dom znaleziony = (Dom) znaleziona;
        assertThat(znaleziony.getMiasto()).isEqualTo("Kraków");
        assertThat(znaleziony.getDzielnica()).isEqualTo("Podgórze");
        assertThat(znaleziony.getAdres()).isEqualTo("ul. Kalwaryjska 10");
        assertThat(znaleziony.getPowierzchniaDzialki()).isEqualTo(500);
        assertThat(znaleziony.getTypBudynku()).isEqualTo("jednorodzinny");
        assertThat(znaleziony.isCzyZOgrodem()).isTrue();
        assertThat(znaleziony.getTypNieruchomosci()).isEqualTo("DOM");
    }

    @Test
    @DisplayName("Powinien zaktualizować nieruchomość")
    void AktualizacjaNieruchomosci() {

        Mieszkanie mieszkanie = new Mieszkanie("Gdańsk", "Wrzeszcz", "ul. Grunwaldzka 5",
                2, "gazowe", false);
        repozytorium.dodaj(mieszkanie);
        mieszkanie.setCzyUmeblowane(true);

        repozytorium.aktualizuj(mieszkanie);

        Nieruchomosc znaleziona = repozytorium.znajdz(mieszkanie.getId());
        Mieszkanie znalezione = (Mieszkanie) znaleziona;
        assertThat(znalezione.isCzyUmeblowane()).isTrue();
    }

    @Test
    @DisplayName("Powinien usunąć nieruchomość")
    void UsuwanieNieruchomosci() {

        Dom dom = new Dom("Poznań", "Grunwald", "ul. Głogowska 15",
                300, "szeregowy", false);
        repozytorium.dodaj(dom);

        repozytorium.usun(dom);

        Nieruchomosc znaleziona = repozytorium.znajdz(dom.getId());
        assertThat(znaleziona).isNull();
    }

    @Test
    @DisplayName("Wyjątek przy dodawaniu duplikatu")
    void RzucanieWyjatku() {

        UUID id = UUID.randomUUID();
        Mieszkanie mieszkanie1 = new Mieszkanie("Wrocław", "Krzyki", "ul. Powstańców 1",
                1, "elektryczne", false);
        mieszkanie1.setId(id);

        Mieszkanie mieszkanie2 = new Mieszkanie("Wrocław", "Fabryczna", "ul. Legnicka 1",
                2, "centralne", true);
        mieszkanie2.setId(id);

        repozytorium.dodaj(mieszkanie1);

        assertThatThrownBy(() -> repozytorium.dodaj(mieszkanie2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("już istnieje");
    }

    @Test
    @DisplayName("Powinien sprawdzić czy nieruchomość jest zajęta")
    void NieruchomoscZajeta() {

        Mieszkanie mieszkanie = new Mieszkanie("Łódź", "Bałuty", "ul. Piotrkowska 100",
                4, "miejskie", true);
        repozytorium.dodaj(mieszkanie);

        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 12, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 12, 0);

        session.execute("INSERT INTO najmy_by_nieruchomosc "
                + "(nieruchomosc_id, data_rozpoczecia, id, najemca_id, "
                + "najemca_login, nieruchomosc_adres, data_zakonczenia) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                mieszkanie.getId(), start.toInstant(ZoneOffset.UTC), UUID.randomUUID(), UUID.randomUUID(),
                "test_user", mieszkanie.getPelnyAdres(), end.toInstant(ZoneOffset.UTC));

        LocalDateTime testStart = LocalDateTime.of(2024, 7, 1, 12, 0);
        LocalDateTime testEnd = LocalDateTime.of(2024, 8, 1, 12, 0);

        boolean zajeta = repozytorium.czyJestZajeta(mieszkanie, testStart, testEnd);
        assertThat(zajeta).isTrue();

        LocalDateTime testStart2 = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime testEnd2 = LocalDateTime.of(2025, 2, 1, 12, 0);

        boolean zajeta2 = repozytorium.czyJestZajeta(mieszkanie, testStart2, testEnd2);
        assertThat(zajeta2).isFalse();
    }

    @Test
    @DisplayName("Test konwersji modelu mieszkania do dokumentu i z powrotem")
    void KonwersjaMieszkania() {

        UUID testId = UUID.randomUUID();
        Mieszkanie original = new Mieszkanie(testId, "Test Miasto", "Test Dzielnica",
                "ul. Test 1", 2, "gazowe", true);

        repozytorium.dodaj(original);
        Nieruchomosc retrieved = repozytorium.znajdz(testId);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isInstanceOf(Mieszkanie.class);

        Mieszkanie mieszkanie = (Mieszkanie) retrieved;
        assertThat(mieszkanie.getId()).isEqualTo(original.getId());
        assertThat(mieszkanie.getMiasto()).isEqualTo(original.getMiasto());
        assertThat(mieszkanie.getDzielnica()).isEqualTo(original.getDzielnica());
        assertThat(mieszkanie.getAdres()).isEqualTo(original.getAdres());
        assertThat(mieszkanie.getLiczbaPokoi()).isEqualTo(original.getLiczbaPokoi());
        assertThat(mieszkanie.getTypOgrzewania()).isEqualTo(original.getTypOgrzewania());
        assertThat(mieszkanie.isCzyUmeblowane()).isEqualTo(original.isCzyUmeblowane());
    }

    @Test
    @DisplayName("Test konwersji modelu domu do dokumentu i z powrotem")
    void KonwersjaDomu() {

        UUID testId = UUID.randomUUID();
        Dom original = new Dom(testId, "Test City", "Test District",
                "ul. Test Dom 5", 1000, "bliźniak", false);

        repozytorium.dodaj(original);
        Nieruchomosc retrieved = repozytorium.znajdz(testId);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isInstanceOf(Dom.class);

        Dom dom = (Dom) retrieved;
        assertThat(dom.getId()).isEqualTo(original.getId());
        assertThat(dom.getMiasto()).isEqualTo(original.getMiasto());
        assertThat(dom.getDzielnica()).isEqualTo(original.getDzielnica());
        assertThat(dom.getAdres()).isEqualTo(original.getAdres());
        assertThat(dom.getPowierzchniaDzialki()).isEqualTo(original.getPowierzchniaDzialki());
        assertThat(dom.getTypBudynku()).isEqualTo(original.getTypBudynku());
        assertThat(dom.isCzyZOgrodem()).isEqualTo(original.isCzyZOgrodem());
    }

    @Test
    @DisplayName("Powinien zwrócić null dla nieistniejącego UUID")
    void TestNulla() {

        Nieruchomosc znaleziona = repozytorium.znajdz(UUID.randomUUID());

        assertThat(znaleziona).isNull();
    }

    @Test
    @DisplayName("Powinien generować poprawny pełny adres")
    void SprawdzPelnyAdres() {
        Mieszkanie mieszkanie = new Mieszkanie("Szczecin", "Centrum", "pl. Grunwaldzki 1",
                3, "centralne", false);

        String pelnyAdres = mieszkanie.getPelnyAdres();

        assertThat(pelnyAdres).isEqualTo("Szczecin, Centrum, pl. Grunwaldzki 1");
    }
}
