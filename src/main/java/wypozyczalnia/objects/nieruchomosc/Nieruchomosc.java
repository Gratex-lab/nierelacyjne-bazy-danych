package wypozyczalnia.objects.nieruchomosc;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.Objects;

/**
 * Abstrakcyjna klasa reprezentująca nieruchomość w wypożyczalni jako dokument
 * MongoDB. Implementuje dziedziczenie poprzez pole typu.
 */
public abstract class Nieruchomosc {

    protected ObjectId id;
    protected String miasto;
    protected String dzielnica;
    protected String adres;
    protected String typ; // "mieszkanie" lub "dom"

    protected Nieruchomosc() {
    }

    public Nieruchomosc(String miasto, String dzielnica, String adres, String typ) {
        this.miasto = Objects.requireNonNull(miasto, "Miasto nie może być nullem");
        this.dzielnica = Objects.requireNonNull(dzielnica, "Dzielnica nie może być nullem");
        this.adres = Objects.requireNonNull(adres, "Adres nie może być nullem");
        this.typ = Objects.requireNonNull(typ, "Typ nie może być nullem");
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getMiasto() {
        return miasto;
    }

    public void setMiasto(String miasto) {
        this.miasto = miasto;
    }

    public String getDzielnica() {
        return dzielnica;
    }

    public void setDzielnica(String dzielnica) {
        this.dzielnica = dzielnica;
    }

    public String getAdres() {
        return adres;
    }

    public void setAdres(String adres) {
        this.adres = adres;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    /**
     * Zwraca pełny adres nieruchomości
     */
    public String getPelnyAdres() {
        return miasto + ", " + dzielnica + ", " + adres;
    }

    /**
     * Konwertuje obiekt do dokumentu MongoDB
     */
    public abstract Document toDocument();

    /**
     * Tworzy bazowy dokument z wspólnymi polami
     */
    protected Document createBaseDocument() {
        Document document = new Document();
        if (id != null) {
            document.append("_id", id);
        }
        document.append("miasto", miasto)
                .append("dzielnica", dzielnica)
                .append("adres", adres)
                .append("typ", typ);
        return document;
    }

    /**
     * Wypełnia bazowe pola z dokumentu
     */
    protected void fillBaseFields(Document document) {
        this.id = document.getObjectId("_id");
        this.miasto = document.getString("miasto");
        this.dzielnica = document.getString("dzielnica");
        this.adres = document.getString("adres");
        this.typ = document.getString("typ");
    }

    /**
     * Tworzy odpowiedni typ nieruchomości z dokumentu MongoDB
     */
    public static Nieruchomosc fromDocument(Document document) {
        if (document == null) {
            return null;
        }

        String typ = document.getString("typ");
        if ("mieszkanie".equals(typ)) {
            return Mieszkanie.fromDocument(document);
        } else if ("dom".equals(typ)) {
            return Dom.fromDocument(document);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Nieruchomosc)) {
            return false;
        }
        Nieruchomosc nieruchomosc = (Nieruchomosc) o;
        return Objects.equals(id, nieruchomosc.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{"
                + "id=" + id
                + ", miasto='" + miasto + '\''
                + ", dzielnica='" + dzielnica + '\''
                + ", adres='" + adres + '\''
                + ", typ='" + typ + '\''
                + '}';
    }
}
