package wypozyczalnia.repositories;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import wypozyczalnia.config.CassandraConfig;
import wypozyczalnia.objects.Najem;

/**
 * Repozytorium dla encji Najem w Cassandra. Obsługuje operacje CRUD oraz
 * zapytania biznesowe z denormalizacją danych.
 */
public class NajemRepozytorium implements Repozytorium<Najem> {

    private final CqlSession session;
    private final CassandraConfig config;

    // Prepared statements dla optymalizacji
    private final PreparedStatement insertNajem;
    private final PreparedStatement insertNajemByNajemca;
    private final PreparedStatement insertNajemByNieruchomosc;
    private final PreparedStatement selectByUuid;
    private final PreparedStatement selectByNajemcaAndNieruchomosc;
    private final PreparedStatement deleteByUuid;
    private final PreparedStatement deleteByNajemcaAndNieruchomosc;
    private final PreparedStatement deleteByNieruchomoscAndData;
    private final PreparedStatement updateNajem;
    private final PreparedStatement updateNajemByNajemca;
    private final PreparedStatement updateNajemByNieruchomosc;

    public NajemRepozytorium(CqlSession session) {
        this.session = session;
        this.config = CassandraConfig.getInstance();

        // Przygotowanie zapytań
        this.insertNajem = session.prepare(
                "INSERT INTO najmy (id, najemca_id, najemca_login, nieruchomosc_id, "
                + "nieruchomosc_adres, data_rozpoczecia, data_zakonczenia) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)");

        this.insertNajemByNajemca = session.prepare(
                "INSERT INTO najmy_by_najemca (najemca_id, nieruchomosc_id, id, najemca_login, "
                + "nieruchomosc_adres, data_rozpoczecia, data_zakonczenia) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)");

        this.insertNajemByNieruchomosc = session.prepare(
                "INSERT INTO najmy_by_nieruchomosc (nieruchomosc_id, data_rozpoczecia, data_zakonczenia, "
                + "id, najemca_id, najemca_login, nieruchomosc_adres) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)");

        this.selectByUuid = session.prepare(
                "SELECT * FROM najmy WHERE id = ?");

        this.selectByNajemcaAndNieruchomosc = session.prepare(
                "SELECT * FROM najmy_by_najemca WHERE najemca_id = ? AND nieruchomosc_id = ?");

        this.deleteByUuid = session.prepare(
                "DELETE FROM najmy WHERE id = ?");

        this.deleteByNajemcaAndNieruchomosc = session.prepare(
                "DELETE FROM najmy_by_najemca WHERE najemca_id = ? AND nieruchomosc_id = ?");

        this.deleteByNieruchomoscAndData = session.prepare(
                "DELETE FROM najmy_by_nieruchomosc WHERE nieruchomosc_id = ? AND data_rozpoczecia = ?");

        this.updateNajem = session.prepare(
                "UPDATE najmy SET najemca_id=?, najemca_login=?, nieruchomosc_id=?, "
                + "nieruchomosc_adres=?, data_rozpoczecia=?, data_zakonczenia=? WHERE id=?");

        this.updateNajemByNajemca = session.prepare(
                "UPDATE najmy_by_najemca SET id=?, najemca_login=?, nieruchomosc_adres=?, "
                + "data_rozpoczecia=?, data_zakonczenia=? WHERE najemca_id=? AND nieruchomosc_id=?");

        this.updateNajemByNieruchomosc = session.prepare(
                "UPDATE najmy_by_nieruchomosc SET data_zakonczenia=?, id=?, najemca_id=?, "
                + "najemca_login=?, nieruchomosc_adres=? WHERE nieruchomosc_id=? AND data_rozpoczecia=?");
    }

    /**
     * Konwertuje LocalDateTime na Instant dla Cassandry
     */
    private Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * Konwertuje Instant z Cassandry na LocalDateTime
     */
    private LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public void dodaj(Najem najem) {
        if (najem.getId() == null) {
            najem.setId(UUID.randomUUID());
        }

        // Denormalizacja - zapisanie do wszystkich trzech tabel
        session.execute(insertNajem.bind(
                najem.getId(),
                najem.getNajemcaId(),
                najem.getNajemcaLogin(),
                najem.getNieruchomoscId(),
                najem.getNieruchomoscAdres(),
                localDateTimeToInstant(najem.getDataRozpoczecia()),
                localDateTimeToInstant(najem.getDataZakonczenia())
        ).setConsistencyLevel(config.getWriteConsistency()));

        session.execute(insertNajemByNajemca.bind(
                najem.getNajemcaId(),
                najem.getNieruchomoscId(),
                najem.getId(),
                najem.getNajemcaLogin(),
                najem.getNieruchomoscAdres(),
                localDateTimeToInstant(najem.getDataRozpoczecia()),
                localDateTimeToInstant(najem.getDataZakonczenia())
        ).setConsistencyLevel(config.getWriteConsistency()));

        session.execute(insertNajemByNieruchomosc.bind(
                najem.getNieruchomoscId(),
                localDateTimeToInstant(najem.getDataRozpoczecia()),
                localDateTimeToInstant(najem.getDataZakonczenia()),
                najem.getId(),
                najem.getNajemcaId(),
                najem.getNajemcaLogin(),
                najem.getNieruchomoscAdres()
        ).setConsistencyLevel(config.getWriteConsistency()));
    }

    @Override
    public void usun(Najem najem) {
        if (najem.getId() == null) {
            throw new IllegalArgumentException("Nie można usunąć najmu bez ID");
        }

        // Usunięcie ze wszystkich trzech tabel (denormalizacja)
        session.execute(deleteByUuid.bind(najem.getId())
                .setConsistencyLevel(config.getWriteConsistency()));

        session.execute(deleteByNajemcaAndNieruchomosc.bind(
                najem.getNajemcaId(),
                najem.getNieruchomoscId())
                .setConsistencyLevel(config.getWriteConsistency()));

        session.execute(deleteByNieruchomoscAndData.bind(
                najem.getNieruchomoscId(),
                localDateTimeToInstant(najem.getDataRozpoczecia()))
                .setConsistencyLevel(config.getWriteConsistency()));
    }

    @Override
    public Najem znajdz(UUID najemUuid) {
        if (najemUuid == null) {
            return null;
        }

        ResultSet resultSet = session.execute(
                selectByUuid.bind(najemUuid)
                        .setConsistencyLevel(config.getReadConsistency()));

        Row row = resultSet.one();
        if (row == null) {
            return null;
        }

        return mapRowToNajem(row);
    }

    /**
     * Znajduje najmy dla danego najemcy i nieruchomości.
     */
    public List<Najem> znajdzNajemcy(UUID nieruchomoscId, UUID najemcaUuid) {
        if (najemcaUuid == null || nieruchomoscId == null) {
            return new ArrayList<>();
        }

        ResultSet resultSet = session.execute(
                selectByNajemcaAndNieruchomosc.bind(najemcaUuid, nieruchomoscId)
                        .setConsistencyLevel(config.getReadConsistency()));

        List<Najem> najmy = new ArrayList<>();
        for (Row row : resultSet) {
            najmy.add(mapRowToNajem(row));
        }

        return najmy;
    }

    /**
     * Liczy aktywne najmy dla najemcy
     */
    public long liczAktywneNajmy(UUID najemcaId, LocalDateTime teraz) {
        if (najemcaId == null || teraz == null) {
            return 0;
        }

        ResultSet resultSet = session.execute(
                "SELECT data_rozpoczecia, data_zakonczenia FROM najmy_by_najemca WHERE najemca_id = ? ALLOW FILTERING",
                najemcaId);

        long aktywneNajmy = 0;
        for (Row row : resultSet) {
            LocalDateTime dataRozpoczecia = instantToLocalDateTime(row.getInstant("data_rozpoczecia"));
            LocalDateTime dataZakonczenia = instantToLocalDateTime(row.getInstant("data_zakonczenia"));

            // Sprawdź czy najem jest aktywny w podanym momencie
            if (dataRozpoczecia != null && dataZakonczenia != null
                    && !teraz.isBefore(dataRozpoczecia) && !teraz.isAfter(dataZakonczenia)) {
                aktywneNajmy++;
            }
        }

        return aktywneNajmy;
    }

    private Najem mapRowToNajem(Row row) {
        return new Najem(
                row.getUuid("id"),
                row.getUuid("najemca_id"),
                row.getString("najemca_login"),
                row.getUuid("nieruchomosc_id"),
                row.getString("nieruchomosc_adres"),
                instantToLocalDateTime(row.getInstant("data_rozpoczecia")),
                instantToLocalDateTime(row.getInstant("data_zakonczenia"))
        );
    }

    /**
     * Aktualizuje najem
     */
    @Override
    public void aktualizuj(Najem najem) {
        if (najem.getId() == null) {
            throw new IllegalArgumentException("Nie można zaktualizować najmu bez ID");
        }

        // Sprawdzenie czy najem istnieje
        Najem istniejacy = znajdz(najem.getId());
        if (istniejacy == null) {
            throw new IllegalArgumentException("Najem o ID '" + najem.getId() + "' nie istnieje.");
        }

        // Aktualizacja we wszystkich trzech tabelach
        session.execute(updateNajem.bind(
                najem.getNajemcaId(),
                najem.getNajemcaLogin(),
                najem.getNieruchomoscId(),
                najem.getNieruchomoscAdres(),
                localDateTimeToInstant(najem.getDataRozpoczecia()),
                localDateTimeToInstant(najem.getDataZakonczenia()),
                najem.getId()
        ).setConsistencyLevel(config.getWriteConsistency()));

        session.execute(updateNajemByNajemca.bind(
                najem.getId(),
                najem.getNajemcaLogin(),
                najem.getNieruchomoscAdres(),
                localDateTimeToInstant(najem.getDataRozpoczecia()),
                localDateTimeToInstant(najem.getDataZakonczenia()),
                najem.getNajemcaId(),
                najem.getNieruchomoscId()
        ).setConsistencyLevel(config.getWriteConsistency()));

        session.execute(updateNajemByNieruchomosc.bind(
                localDateTimeToInstant(najem.getDataZakonczenia()),
                najem.getId(),
                najem.getNajemcaId(),
                najem.getNajemcaLogin(),
                najem.getNieruchomoscAdres(),
                najem.getNieruchomoscId(),
                localDateTimeToInstant(najem.getDataRozpoczecia())
        ).setConsistencyLevel(config.getWriteConsistency()));
    }
}
