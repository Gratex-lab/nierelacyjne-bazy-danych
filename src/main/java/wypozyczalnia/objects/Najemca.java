package wypozyczalnia.objects;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.Objects;

/**
 * Klasa reprezentująca najemcę wypożyczalni nieruchomości jako dokument MongoDB.
 * Zawiera mechanizm aktywacji/deaktywacji konta.
 */
public class Najemca {

    private ObjectId id;
    private String login;
    private boolean aktywny;

    /**
     * Konstruktor bez argumentów
     */
    public Najemca() {
        this.aktywny = true;
    }

    public Najemca(String login) {
        this.login = Objects.requireNonNull(login, "Login nie może być nullem.");
        this.aktywny = true;
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

    public boolean czyAktywny() {
        return aktywny;
    }

    public void setAktywny(boolean aktywny) {
        this.aktywny = aktywny;
    }

    /**
     * Konwertuje obiekt do dokumentu MongoDB
     */
    public Document toDocument() {
        Document document = new Document();
        if (id != null) {
            document.append("_id", id);
        }
        document.append("login", login)
                .append("aktywny", aktywny);
        return document;
    }

    /**
     * Tworzy obiekt z dokumentu MongoDB
     */
    public static Najemca fromDocument(Document document) {
        if (document == null) {
            return null;
        }
        
        Najemca najemca = new Najemca();
        najemca.setId(document.getObjectId("_id"));
        najemca.setLogin(document.getString("login"));
        najemca.setAktywny(document.getBoolean("aktywny", true));
        return najemca;
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
        return "Najemca{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", aktywny=" + aktywny +
                '}';
    }
}
