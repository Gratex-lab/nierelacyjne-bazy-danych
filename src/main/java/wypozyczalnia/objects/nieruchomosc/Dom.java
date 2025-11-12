package wypozyczalnia.objects.nieruchomosc;

import org.bson.Document;
import java.util.Objects;

/**
 * Klasa reprezentująca dom w wypożyczalni jako dokument MongoDB.
 * Dziedziczy po klasie Nieruchomosc.
 */
public class Dom extends Nieruchomosc {

    private int powierzchniaDzialki;
    private String typBudynku;
    private boolean czyZOgrodem;

    public Dom() {
        super();
    }

    public Dom(String miasto, String dzielnica, String adres,
            int powierzchniaDzialki, String typBudynku, boolean czyZOgrodem) {
        super(miasto, dzielnica, adres, "dom");
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
    public Document toDocument() {
        Document document = createBaseDocument();
        document.append("powierzchniaDzialki", powierzchniaDzialki)
                .append("typBudynku", typBudynku)
                .append("czyZOgrodem", czyZOgrodem);
        return document;
    }

    /**
     * Tworzy obiekt Dom z dokumentu MongoDB
     */
    public static Dom fromDocument(Document document) {
        if (document == null) {
            return null;
        }

        Dom dom = new Dom();
        dom.fillBaseFields(document);
        dom.setPowierzchniaDzialki(document.getInteger("powierzchniaDzialki", 0));
        dom.setTypBudynku(document.getString("typBudynku"));
        dom.setCzyZOgrodem(document.getBoolean("czyZOgrodem", false));
        return dom;
    }

    @Override
    public String toString() {
        return "Dom{" +
                "id=" + id +
                ", miasto='" + miasto + '\'' +
                ", dzielnica='" + dzielnica + '\'' +
                ", adres='" + adres + '\'' +
                ", powierzchniaDzialki=" + powierzchniaDzialki +
                ", typBudynku='" + typBudynku + '\'' +
                ", czyZOgrodem=" + czyZOgrodem +
                '}';
    }
}
