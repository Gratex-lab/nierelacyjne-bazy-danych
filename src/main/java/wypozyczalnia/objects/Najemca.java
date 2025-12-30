package wypozyczalnia.objects;

import java.util.Objects;
import java.util.UUID;

/**
 * Klasa reprezentująca najemcę wypożyczalni nieruchomości.
 */
public class Najemca {

    private UUID id;
    private String login;
    private boolean aktywny;

    /**
     * Konstruktor bez argumentów
     */
    public Najemca() {
        this.login = null;
        this.aktywny = true;
    }

    public Najemca(String login) {
        this.id = UUID.randomUUID();
        this.login = Objects.requireNonNull(login, "Login nie może być nullem.");
        this.aktywny = true;
    }

    public Najemca(UUID id, String login, boolean aktywny) {
        this.id = id;
        this.login = login;
        this.aktywny = aktywny;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public boolean czyAktywny() {
        return aktywny;
    }

    public void setAktywny(boolean aktywny) {
        this.aktywny = aktywny;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Najemca najemca = (Najemca) o;
        return Objects.equals(id, najemca.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Najemca{"
                + "id=" + id
                + ", login='" + login + '\''
                + ", aktywny=" + aktywny
                + '}';
    }
}
