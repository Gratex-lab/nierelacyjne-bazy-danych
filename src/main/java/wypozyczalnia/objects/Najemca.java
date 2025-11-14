package wypozyczalnia.objects;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import java.util.Objects;

/**
 * Klasa reprezentująca najemcę wypożyczalni nieruchomości jako dokument
 * MongoDB. Zawiera mechanizm aktywacji/deaktywacji konta.
 */
public class Najemca {

    @BsonId
    private ObjectId id;

    @BsonProperty("login")
    private String login;

    @BsonProperty("aktywny")
    private boolean active;

    /**
     * Konstruktor bez argumentów
     */
    public Najemca() {
        this.active = true;
    }

    @BsonCreator
    public Najemca(@BsonProperty("login") String login) {
        this.login = Objects.requireNonNull(login, "Login nie może być nullem.");
        this.active = true;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
                + ", aktywny=" + active
                + '}';
    }
}
