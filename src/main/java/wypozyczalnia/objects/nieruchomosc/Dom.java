package wypozyczalnia.objects.nieruchomosc;

import java.util.Objects;
import java.util.UUID;

/**
 * Klasa reprezentująca dom w wypożyczalni.
 */
public class Dom extends Nieruchomosc {

    private int powierzchniaDzialki;
    private String typBudynku;
    private boolean czyZOgrodem;

    public Dom() {
        super();
        setTypNieruchomosci("DOM");
    }

    public Dom(String miasto, String dzielnica, String adres,
            int powierzchniaDzialki, String typBudynku, boolean czyZOgrodem) {
        super(miasto, dzielnica, adres);
        setTypNieruchomosci("DOM");
        this.powierzchniaDzialki = powierzchniaDzialki;
        this.typBudynku = Objects.requireNonNull(typBudynku, "Typ budynku nie może być nullem");
        this.czyZOgrodem = czyZOgrodem;
    }

    public Dom(UUID id, String miasto, String dzielnica, String adres,
            int powierzchniaDzialki, String typBudynku, boolean czyZOgrodem) {
        super(id, miasto, dzielnica, adres, "DOM");
        this.powierzchniaDzialki = powierzchniaDzialki;
        this.typBudynku = typBudynku;
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
                + "id=" + getId()
                + ", miasto='" + getMiasto() + '\''
                + ", dzielnica='" + getDzielnica() + '\''
                + ", adres='" + getAdres() + '\''
                + ", powierzchniaDzialki=" + powierzchniaDzialki
                + ", typBudynku='" + typBudynku + '\''
                + ", czyZOgrodem=" + czyZOgrodem
                + '}';
    }
}
