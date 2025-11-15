package wypozyczalnia.objects.nieruchomosc;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.types.ObjectId;
import java.util.Objects;

/**
 * Abstrakcyjna klasa reprezentująca nieruchomość w wypożyczalni jako dokument
 * MongoDB. Implementuje dziedziczenie poprzez pole typu.
 */
@BsonDiscriminator(key = "typ")
public abstract class Nieruchomosc {

    @BsonId
    protected ObjectId id;

    @BsonProperty("miasto")
    protected String miasto;

    @BsonProperty("dzielnica")
    protected String dzielnica;

    @BsonProperty("adres")
    protected String adres;

    @BsonProperty("typ")
    protected String typ; // "mieszkanie" lub "dom"

    @BsonProperty("version")
    protected long version = 0;

    protected Nieruchomosc() {
    }

    @BsonCreator
    public Nieruchomosc(
            @BsonProperty("miasto") String miasto,
            @BsonProperty("dzielnica") String dzielnica,
            @BsonProperty("adres") String adres,
            @BsonProperty("typ") String typ,
            @BsonProperty("version") Long version) {
        this.miasto = Objects.requireNonNull(miasto, "Miasto nie może być nullem");
        this.dzielnica = Objects.requireNonNull(dzielnica, "Dzielnica nie może być nullem");
        this.adres = Objects.requireNonNull(adres, "Adres nie może być nullem");
        this.typ = Objects.requireNonNull(typ, "Typ nie może być nullem");
        this.version = version != null ? version : 0;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Zwraca pełny adres nieruchomości
     */
    public String getPelnyAdres() {
        return miasto + ", " + dzielnica + ", " + adres;
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
