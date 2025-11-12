package wypozyczalnia.objects.nieruchomosc;

import java.util.Objects;
import jakarta.persistence.*;

/**
 * Klasa reprezentująca mieszkanie w wypożyczalni. Dziedziczy po klasie
 * Nieruchomosc.
 */
@Entity
@Table(name = "mieszkania")
@PrimaryKeyJoinColumn(name = "nieruchomosc_id")
public class Mieszkanie extends Nieruchomosc {

    @Column(name = "liczba_pokoi", nullable = false)
    private int liczbaPokoi;

    @Column(name = "typ_ogrzewania", nullable = false, length = 20)
    private String typOgrzewania;

    @Column(name = "czy_umeblowane", nullable = false)
    private boolean czyUmeblowane;

    protected Mieszkanie() {
    }

    public Mieszkanie(String miasto, String dzielnica, String adres,
            int liczbaPokoi, String typOgrzewania, boolean czyUmeblowane) {
        super(miasto, dzielnica, adres);
        this.liczbaPokoi = liczbaPokoi;
        this.typOgrzewania = Objects.requireNonNull(typOgrzewania, "Typ ogrzewania nie może być nullem");
        this.czyUmeblowane = czyUmeblowane;
    }

    public int getLiczbaPokoi() {
        return liczbaPokoi;
    }

    public String getTypOgrzewania() {
        return typOgrzewania;
    }

    public boolean isCzyUmeblowane() {
        return czyUmeblowane;
    }
}
