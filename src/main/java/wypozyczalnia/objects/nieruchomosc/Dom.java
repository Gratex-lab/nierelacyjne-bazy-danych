package wypozyczalnia.objects.nieruchomosc;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import java.util.Objects;

/**
 * Klasa reprezentująca dom w wypożyczalni jako dokument MongoDB. Dziedziczy po
 * klasie Nieruchomosc.
 */
@BsonDiscriminator(value = "dom")
public class Dom extends Nieruchomosc {

    @BsonProperty("powierzchniaDzialki")
    private int powierzchniaDzialki;

    @BsonProperty("typBudynku")
    private String typBudynku;

    @BsonProperty("czyZOgrodem")
    private boolean czyZOgrodem;

    public Dom() {
        super();
    }

    @BsonCreator
    public Dom(
            @BsonProperty("miasto") String miasto,
            @BsonProperty("dzielnica") String dzielnica,
            @BsonProperty("adres") String adres,
            @BsonProperty("powierzchniaDzialki") int powierzchniaDzialki,
            @BsonProperty("typBudynku") String typBudynku,
            @BsonProperty("czyZOgrodem") boolean czyZOgrodem,
            @BsonProperty("version") Long version) {
        super(miasto, dzielnica, adres, "dom", version);
        this.powierzchniaDzialki = powierzchniaDzialki;
        this.typBudynku = Objects.requireNonNull(typBudynku, "Typ budynku nie może być nullem");
        this.czyZOgrodem = czyZOgrodem;
    }

    public int getPowierzchniaDzialki() {
        return powierzchniaDzialki;
    }

    public void setPowierzchniaDzialki(int powierzchniaDzialki) {
        this.powierzchniaDzialki = powierzchniaDzialki;
    }

    public String getTypBudynku() {
        return typBudynku;
    }

    public void setTypBudynku(String typBudynku) {
        this.typBudynku = typBudynku;
    }

    public boolean isCzyZOgrodem() {
        return czyZOgrodem;
    }

    public void setCzyZOgrodem(boolean czyZOgrodem) {
        this.czyZOgrodem = czyZOgrodem;
    }

    @Override
    public String toString() {
        return "Dom{"
                + "id=" + id
                + ", miasto='" + miasto + '\''
                + ", dzielnica='" + dzielnica + '\''
                + ", adres='" + adres + '\''
                + ", powierzchniaDzialki=" + powierzchniaDzialki
                + ", typBudynku='" + typBudynku + '\''
                + ", czyZOgrodem=" + czyZOgrodem
                + '}';
    }
}
