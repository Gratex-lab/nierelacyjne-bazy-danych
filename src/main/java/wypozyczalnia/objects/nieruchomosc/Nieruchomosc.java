package wypozyczalnia.objects.nieruchomosc;

import java.util.Objects;
import java.util.UUID;

/**
 * Abstrakcyjna klasa reprezentująca nieruchomość w wypożyczalni.
 */
public abstract class Nieruchomosc {

    private UUID id;
    private String miasto;
    private String dzielnica;
    private String adres;
    private String typNieruchomosci;

    protected Nieruchomosc() {
    }

    public Nieruchomosc(String miasto, String dzielnica, String adres) {
        this.id = UUID.randomUUID();
        this.miasto = Objects.requireNonNull(miasto, "Miasto nie może być nullem");
        this.dzielnica = Objects.requireNonNull(dzielnica, "Dzielnica nie może być nullem");
        this.adres = Objects.requireNonNull(adres, "Adres nie może być nullem");
    }

    public Nieruchomosc(UUID id, String miasto, String dzielnica, String adres, String typNieruchomosci) {
        this.id = id;
        this.miasto = miasto;
        this.dzielnica = dzielnica;
        this.adres = adres;
        this.typNieruchomosci = typNieruchomosci;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public String getTypNieruchomosci() {
        return typNieruchomosci;
    }

    public void setTypNieruchomosci(String typNieruchomosci) {
        this.typNieruchomosci = typNieruchomosci;
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
        return "Nieruchomosc{"
                + "id=" + id
                + ", miasto='" + miasto + '\''
                + ", dzielnica='" + dzielnica + '\''
                + ", adres='" + adres + '\''
                + ", typNieruchomosci='" + typNieruchomosci + '\''
                + '}';
    }
}
