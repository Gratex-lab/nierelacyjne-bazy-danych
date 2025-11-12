package wypozyczalnia.objects.nieruchomosc;

import java.util.Objects;
import jakarta.persistence.*;

/**
 * Klasa reprezentująca dom w wypożyczalni. Dziedziczy po klasie Nieruchomosc.
 */
@Entity
@Table(name = "domy")
@PrimaryKeyJoinColumn(name = "nieruchomosc_id")
public class Dom extends Nieruchomosc {

    @Column(name = "powierzchnia_dzialki", nullable = false)
    private int powierzchniaDzialki;

    @Column(name = "typ_budynku", nullable = false, length = 20)
    private String typBudynku;

    @Column(name = "czy_z_ogrodem", nullable = false)
    private boolean czyZOgrodem;

    protected Dom() {
    }

    public Dom(String miasto, String dzielnica, String adres,
            int powierzchniaDzialki, String typBudynku, boolean czyZOgrodem) {
        super(miasto, dzielnica, adres);
        this.powierzchniaDzialki = powierzchniaDzialki;
        this.typBudynku = Objects.requireNonNull(typBudynku, "Typ budynku nie może być nullem");
        this.czyZOgrodem = czyZOgrodem;
    }

    public int getPowierzchniaDzialki() {
        return powierzchniaDzialki;
    }

    public String getTypBudynku() {
        return typBudynku;
    }

    public boolean isCzyZOgrodem() {
        return czyZOgrodem;
    }
}
