package wypozyczalnia.repositories;

import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import wypozyczalnia.config.CassandraConfig;
import wypozyczalnia.objects.Najemca;

/**
 * Repozytorium dla encji Najemca w Cassandra. Obsługuje operacje CRUD oraz
 * wyszukiwanie po loginie z denormalizacją danych.
 */
public class NajemcaRepozytorium implements Repozytorium<Najemca> {

    private final CqlSession session;
    private final CassandraConfig config;

    // Prepared statements dla optymalizacji
    private final PreparedStatement insertNajemca;
    private final PreparedStatement insertNajemcaByLogin;
    private final PreparedStatement selectByUuid;
    private final PreparedStatement selectByLogin;
    private final PreparedStatement deleteByUuid;
    private final PreparedStatement deleteByLogin;
    private final PreparedStatement updateByUuid;
    private final PreparedStatement updateByLogin;

    public NajemcaRepozytorium(CqlSession session) {
        this.session = session;
        this.config = CassandraConfig.getInstance();

        // Przygotowanie zapytań
        this.insertNajemca = session.prepare(
                "INSERT INTO najemcy (id, login, aktywny) VALUES (?, ?, ?) "
                + "IF NOT EXISTS");

        this.insertNajemcaByLogin = session.prepare(
                "INSERT INTO najemcy_by_login (login, id, aktywny) VALUES (?, ?, ?) "
                + "IF NOT EXISTS");

        this.selectByUuid = session.prepare(
                "SELECT id, login, aktywny FROM najemcy WHERE id = ?");

        this.selectByLogin = session.prepare(
                "SELECT login, id, aktywny FROM najemcy_by_login WHERE login = ?");

        this.deleteByUuid = session.prepare(
                "DELETE FROM najemcy WHERE id = ?");

        this.deleteByLogin = session.prepare(
                "DELETE FROM najemcy_by_login WHERE login = ?");

        this.updateByUuid = session.prepare(
                "UPDATE najemcy SET aktywny = ? WHERE id = ?");

        this.updateByLogin = session.prepare(
                "UPDATE najemcy_by_login SET aktywny = ? WHERE login = ?");
    }

    @Override
    public void dodaj(Najemca najemca) {
        if (najemca.getId() == null) {
            najemca.setId(UUID.randomUUID());
        }

        // Sprawdzenie unikalności loginu
        Najemca istniejacy = znajdzLogin(najemca.getLogin());
        if (istniejacy != null) {
            throw new IllegalArgumentException("Najemca z loginem '" + najemca.getLogin() + "' już istnieje.");
        }

        // Denormalizacja - zapisanie do obu tabel
        session.execute(insertNajemca.bind(
                najemca.getId(),
                najemca.getLogin(),
                najemca.czyAktywny()
        ).setConsistencyLevel(config.getWriteConsistency()));

        session.execute(insertNajemcaByLogin.bind(
                najemca.getLogin(),
                najemca.getId(),
                najemca.czyAktywny()
        ).setConsistencyLevel(config.getWriteConsistency()));
    }

    @Override
    public void usun(Najemca najemca) {
        if (najemca.getId() == null) {
            throw new IllegalArgumentException("Nie można usunąć najemcy bez ID");
        }

        // Usunięcie z obu tabel (denormalizacja)
        session.execute(deleteByUuid.bind(najemca.getId())
                .setConsistencyLevel(config.getWriteConsistency()));

        session.execute(deleteByLogin.bind(najemca.getLogin())
                .setConsistencyLevel(config.getWriteConsistency()));
    }

    @Override
    public Najemca znajdz(UUID najemcaId) {
        if (najemcaId == null) {
            return null;
        }

        ResultSet resultSet = session.execute(
                selectByUuid.bind(najemcaId)
                        .setConsistencyLevel(config.getReadConsistency()));

        Row row = resultSet.one();
        if (row == null) {
            return null;
        }

        return new Najemca(
                row.getUuid("id"),
                row.getString("login"),
                row.getBoolean("aktywny")
        );
    }

    /**
     * Wyszukuje najemcę po unikalnym loginie.
     */
    public Najemca znajdzLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            return null;
        }

        ResultSet resultSet = session.execute(
                selectByLogin.bind(login)
                        .setConsistencyLevel(config.getReadConsistency()));

        Row row = resultSet.one();
        if (row == null) {
            return null;
        }

        return new Najemca(
                row.getUuid("id"),
                row.getString("login"),
                row.getBoolean("aktywny")
        );
    }

    /**
     * Aktualizuje najemcę (tylko pole aktywny)
     */
    @Override
    public void aktualizuj(Najemca najemca) {
        if (najemca.getId() == null) {
            throw new IllegalArgumentException("Nie można zaktualizować najemcy bez ID");
        }

        // Aktualizacja w obu tabelach (denormalizacja)
        session.execute(updateByUuid.bind(
                najemca.czyAktywny(),
                najemca.getId()
        ).setConsistencyLevel(config.getWriteConsistency()));

        session.execute(updateByLogin.bind(
                najemca.czyAktywny(),
                najemca.getLogin()
        ).setConsistencyLevel(config.getWriteConsistency()));
    }
}
