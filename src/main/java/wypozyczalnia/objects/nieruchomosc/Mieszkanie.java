package wypozyczalnia.objects.nieruchomosc;

import org.bson.Document;
import java.util.Objects;

/**
 * Klasa reprezentująca mieszkanie w wypożyczalni jako dokument MongoDB.
 * Dziedziczy po klasie Nieruchomosc.
 */
public class Mieszkanie extends Nieruchomosc {

    private int liczbaPokoi;
    private String typOgrzewania;
    private boolean czyUmeblowane;

    public Mieszkanie() {
        super();
    }

    public Mieszkanie(String miasto, String dzielnica, String adres,
            int liczbaPokoi, String typOgrzewania, boolean czyUmeblowane) {
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
    public Document toDocument() {
        Document document = createBaseDocument();
        document.append("liczbaPokoi", liczbaPokoi)
                .append("typOgrzewania", typOgrzewania)
                .append("czyUmeblowane", czyUmeblowane);
        return document;
    }

    /**
     * Tworzy obiekt Mieszkanie z dokumentu MongoDB
     */
    public static Mieszkanie fromDocument(Document document) {
        if (document == null) {
            return null;
        }

        Mieszkanie mieszkanie = new Mieszkanie();
        mieszkanie.fillBaseFields(document);
        mieszkanie.setLiczbaPokoi(document.getInteger("liczbaPokoi", 0));
        mieszkanie.setTypOgrzewania(document.getString("typOgrzewania"));
        mieszkanie.setCzyUmeblowane(document.getBoolean("czyUmeblowane", false));
        return mieszkanie;
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
