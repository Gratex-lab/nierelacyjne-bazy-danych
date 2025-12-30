package wypozyczalnia.objects.nieruchomosc;

import java.util.Objects;
import java.util.UUID;

/**
 * Klasa reprezentująca mieszkanie w wypożyczalni.
 */
public class Mieszkanie extends Nieruchomosc {

    private int liczbaPokoi;
    private String typOgrzewania;
    private boolean czyUmeblowane;

    public Mieszkanie() {
        super();
        setTypNieruchomosci("MIESZKANIE");
    }

    public Mieszkanie(String miasto, String dzielnica, String adres,
            int liczbaPokoi, String typOgrzewania, boolean czyUmeblowane) {
        super(miasto, dzielnica, adres);
        setTypNieruchomosci("MIESZKANIE");
        this.liczbaPokoi = liczbaPokoi;
        this.typOgrzewania = Objects.requireNonNull(typOgrzewania, "Typ ogrzewania nie może być nullem");
        this.czyUmeblowane = czyUmeblowane;
    }

    public Mieszkanie(UUID id, String miasto, String dzielnica, String adres,
            int liczbaPokoi, String typOgrzewania, boolean czyUmeblowane) {
        super(id, miasto, dzielnica, adres, "MIESZKANIE");
        this.liczbaPokoi = liczbaPokoi;
        this.typOgrzewania = typOgrzewania;
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
                + "id=" + getId()
                + ", miasto='" + getMiasto() + '\''
                + ", dzielnica='" + getDzielnica() + '\''
                + ", adres='" + getAdres() + '\''
                + ", liczbaPokoi=" + liczbaPokoi
                + ", typOgrzewania='" + typOgrzewania + '\''
                + ", czyUmeblowane=" + czyUmeblowane
                + '}';
    }
}
