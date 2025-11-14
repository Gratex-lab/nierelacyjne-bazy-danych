package wypozyczalnia.objects.nieruchomosc;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import java.util.Objects;

/**
 * Klasa reprezentująca mieszkanie w wypożyczalni jako dokument MongoDB.
 * Dziedziczy po klasie Nieruchomosc.
 */
@BsonDiscriminator(value = "mieszkanie")
public class Mieszkanie extends Nieruchomosc {

    @BsonProperty("liczbaPokoi")
    private int liczbaPokoi;

    @BsonProperty("typOgrzewania")
    private String typOgrzewania;

    @BsonProperty("czyUmeblowane")
    private boolean czyUmeblowane;

    public Mieszkanie() {
        super();
    }

    @BsonCreator
    public Mieszkanie(
            @BsonProperty("miasto") String miasto,
            @BsonProperty("dzielnica") String dzielnica,
            @BsonProperty("adres") String adres,
            @BsonProperty("liczbaPokoi") int liczbaPokoi,
            @BsonProperty("typOgrzewania") String typOgrzewania,
            @BsonProperty("czyUmeblowane") boolean czyUmeblowane) {
        super(miasto, dzielnica, adres, "mieszkanie");
        this.liczbaPokoi = liczbaPokoi;
        this.typOgrzewania = Objects.requireNonNull(typOgrzewania, "Typ ogrzewania nie może być nullem");
        this.czyUmeblowane = czyUmeblowane;
    }

    public int getLiczbaPokoi() {
        return liczbaPokoi;
    }

    public void setLiczbaPokoi(int liczbaPokoi) {
        this.liczbaPokoi = liczbaPokoi;
    }

    public String getTypOgrzewania() {
        return typOgrzewania;
    }

    public void setTypOgrzewania(String typOgrzewania) {
        this.typOgrzewania = typOgrzewania;
    }

    public boolean isCzyUmeblowane() {
        return czyUmeblowane;
    }

    public void setCzyUmeblowane(boolean czyUmeblowane) {
        this.czyUmeblowane = czyUmeblowane;
    }

    @Override
    public String toString() {
        return "Mieszkanie{"
                + "id=" + id
                + ", miasto='" + miasto + '\''
                + ", dzielnica='" + dzielnica + '\''
                + ", adres='" + adres + '\''
                + ", liczbaPokoi=" + liczbaPokoi
                + ", typOgrzewania='" + typOgrzewania + '\''
                + ", czyUmeblowane=" + czyUmeblowane
                + '}';
    }
}
