package wypozyczalnia.repositories;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import wypozyczalnia.config.CassandraConfig;
import wypozyczalnia.objects.nieruchomosc.Dom;
import wypozyczalnia.objects.nieruchomosc.Mieszkanie;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;

/**
 * Repozytorium dla encji Nieruchomosc w Cassandra. Obsługuje operacje CRUD oraz
 * sprawdzanie dostępności nieruchomości z denormalizacją danych.
 */
public class NieruchomoscRepozytorium implements Repozytorium<Nieruchomosc> {

    private final CqlSession session;
    private final CassandraConfig config;

    // Prepared statements dla optymalizacji
    private final PreparedStatement insertNieruchomosc;
    private final PreparedStatement selectByUuid;
    private final PreparedStatement deleteByUuid;
    private final PreparedStatement updateByUuid;

    public NieruchomoscRepozytorium(CqlSession session) {
        this.session = session;
        this.config = CassandraConfig.getInstance();

        // Przygotowanie zapytań
        this.insertNieruchomosc = session.prepare(
                "INSERT INTO nieruchomosci (id, miasto, dzielnica, adres, typ_nieruchomosci, "
                + "liczba_pokoi, typ_ogrzewania, czy_umeblowane, powierzchnia_dzialki, typ_budynku, "
                + "czy_z_ogrodem) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "IF NOT EXISTS");

        this.selectByUuid = session.prepare(
                "SELECT * FROM nieruchomosci WHERE id = ?");

        this.deleteByUuid = session.prepare(
                "DELETE FROM nieruchomosci WHERE id = ?");

        this.updateByUuid = session.prepare(
                "UPDATE nieruchomosci SET miasto=?, dzielnica=?, adres=?, "
                + "typ_nieruchomosci=?, liczba_pokoi=?, typ_ogrzewania=?, "
                + "czy_umeblowane=?, powierzchnia_dzialki=?, typ_budynku=?, "
                + "czy_z_ogrodem=? WHERE id=?");
    }

    @Override
    public void dodaj(Nieruchomosc nieruchomosc) {
        if (nieruchomosc.getId() == null) {
            nieruchomosc.setId(UUID.randomUUID());
        }

        // Sprawdzenie czy nieruchomość już istnieje
        Nieruchomosc istniejaca = znajdz(nieruchomosc.getId());
        if (istniejaca != null) {
            throw new IllegalArgumentException("Nieruchomość o ID '" + nieruchomosc.getId() + "' już istnieje.");
        }

        // Przygotowanie wartości dla różnych typów nieruchomości
        Integer liczbaPokoi = null;
        String typOgrzewania = null;
        Boolean czyUmeblowane = null;
        Integer powierzchniaDzialki = null;
        String typBudynku = null;
        Boolean czyZOgrodem = null;

        switch (nieruchomosc) {
            case Mieszkanie mieszkanie -> {
                liczbaPokoi = mieszkanie.getLiczbaPokoi();
                typOgrzewania = mieszkanie.getTypOgrzewania();
                czyUmeblowane = mieszkanie.isCzyUmeblowane();
            }
            case Dom dom -> {
                powierzchniaDzialki = dom.getPowierzchniaDzialki();
                typBudynku = dom.getTypBudynku();
                czyZOgrodem = dom.isCzyZOgrodem();
            }
            default -> {
            }
        }

        session.execute(insertNieruchomosc.bind(
                nieruchomosc.getId(),
                nieruchomosc.getMiasto(),
                nieruchomosc.getDzielnica(),
                nieruchomosc.getAdres(),
                nieruchomosc.getTypNieruchomosci(),
                liczbaPokoi,
                typOgrzewania,
                czyUmeblowane,
                powierzchniaDzialki,
                typBudynku,
                czyZOgrodem
        ).setConsistencyLevel(config.getWriteConsistency()));
    }

    @Override
    public void usun(Nieruchomosc nieruchomosc) {
        if (nieruchomosc.getId() == null) {
            throw new IllegalArgumentException("Nie można usunąć nieruchomości bez ID");
        }

        session.execute(deleteByUuid.bind(nieruchomosc.getId())
                .setConsistencyLevel(config.getWriteConsistency()));
    }

    @Override
    public Nieruchomosc znajdz(UUID nieruchomoscUuid) {
        if (nieruchomoscUuid == null) {
            return null;
        }

        ResultSet resultSet = session.execute(
                selectByUuid.bind(nieruchomoscUuid)
                        .setConsistencyLevel(config.getReadConsistency()));

        Row row = resultSet.one();
        if (row == null) {
            return null;
        }

        return mapRowToNieruchomosc(row);
    }

    private Nieruchomosc mapRowToNieruchomosc(Row row) {
        UUID id = row.getUuid("id");
        String miasto = row.getString("miasto");
        String dzielnica = row.getString("dzielnica");
        String adres = row.getString("adres");
        String typ = row.getString("typ_nieruchomosci");

        if ("MIESZKANIE".equals(typ)) {
            Integer liczbaPokoi = row.getInt("liczba_pokoi");
            String typOgrzewania = row.getString("typ_ogrzewania");
            Boolean czyUmeblowane = row.getBoolean("czy_umeblowane");

            return new Mieszkanie(id, miasto, dzielnica, adres,
                    liczbaPokoi != null ? liczbaPokoi : 0,
                    typOgrzewania != null ? typOgrzewania : "",
                    czyUmeblowane != null ? czyUmeblowane : false);
        } else if ("DOM".equals(typ)) {
            Integer powierzchniaDzialki = row.getInt("powierzchnia_dzialki");
            String typBudynku = row.getString("typ_budynku");
            Boolean czyZOgrodem = row.getBoolean("czy_z_ogrodem");

            return new Dom(id, miasto, dzielnica, adres,
                    powierzchniaDzialki != null ? powierzchniaDzialki : 0,
                    typBudynku != null ? typBudynku : "",
                    czyZOgrodem != null ? czyZOgrodem : false);
        }

        throw new IllegalStateException("Nieznany typ nieruchomości: " + typ);
    }

    /**
     * Sprawdza czy nieruchomość jest zajęta w podanym okresie. Wykorzystuje
     * denormalizowaną tabelę najmy_by_nieruchomosc.
     */
    public boolean czyJestZajeta(Nieruchomosc nieruchomosc, LocalDateTime start, LocalDateTime koniec) {
        if (nieruchomosc.getId() == null || start == null || koniec == null) {
            return false;
        }

        // Pobieramy wszystkie najmy dla nieruchomości i sprawdzamy kolizje dat
        ResultSet resultSet = session.execute(
                "SELECT data_rozpoczecia, data_zakonczenia FROM najmy_by_nieruchomosc WHERE nieruchomosc_id = ? ALLOW FILTERING",
                nieruchomosc.getId());

        for (Row row : resultSet) {
            Instant startInstant = row.getInstant("data_rozpoczecia");
            Instant endInstant = row.getInstant("data_zakonczenia");

            if (startInstant != null && endInstant != null) {
                LocalDateTime existingStart = LocalDateTime.ofInstant(startInstant, ZoneOffset.UTC);
                LocalDateTime existingEnd = LocalDateTime.ofInstant(endInstant, ZoneOffset.UTC);

                // Sprawdź kolizję dat: nowy okres koliduje z istniejącym jeśli:
                // start < existingEnd && koniec > existingStart
                if (!start.isAfter(existingEnd) && !koniec.isBefore(existingStart)) {
                    return true; // Kolizja wykryta
                }
            }
        }

        return false; // Brak kolizji
    }

    /**
     * Aktualizuje nieruchomość
     */
    @Override
    public void aktualizuj(Nieruchomosc nieruchomosc) {
        if (nieruchomosc.getId() == null) {
            throw new IllegalArgumentException("Nie można zaktualizować nieruchomości bez ID");
        }
        session.execute(updateByUuid.bind(
                nieruchomosc.getMiasto(),
                nieruchomosc.getDzielnica(),
                nieruchomosc.getAdres(),
                nieruchomosc.getTypNieruchomosci(),
                nieruchomosc instanceof Mieszkanie mieszkanie ? mieszkanie.getLiczbaPokoi() : null,
                nieruchomosc instanceof Mieszkanie mieszkanie ? mieszkanie.getTypOgrzewania() : null,
                nieruchomosc instanceof Mieszkanie mieszkanie ? mieszkanie.isCzyUmeblowane() : null,
                nieruchomosc instanceof Dom dom ? dom.getPowierzchniaDzialki() : null,
                nieruchomosc instanceof Dom dom ? dom.getTypBudynku() : null,
                nieruchomosc instanceof Dom dom ? dom.isCzyZOgrodem() : null,
                nieruchomosc.getId()
        ).setConsistencyLevel(config.getWriteConsistency()));
    }
}
