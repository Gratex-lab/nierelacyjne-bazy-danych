package library.objects.auto;

import java.util.Objects;
import jakarta.persistence.*;

/**
 * Klasa reprezentująca samochód osobowy w wypożyczalni. Dziedziczy po klasie
 * Auto.
 */
@Entity
@Table(name = "auta_osobowe")
@PrimaryKeyJoinColumn(name = "auto_id")
public class SamochodOsobowy extends Auto {

    @Column(name = "liczba_miejsc", nullable = false)
    private int liczbaMiejsc;

    @Column(name = "typ_paliwa", nullable = false, length = 20)
    private String typPaliwa;

    @Column(name = "automatyczna_skrzynia", nullable = false)
    private boolean automatycznaSkrzynia;

    protected SamochodOsobowy() {
    }

    public SamochodOsobowy(String marka, String model, String numerRejestracyjny,
            int liczbaMiejsc, String typPaliwa, boolean automatycznaSkrzynia) {
        super(marka, model, numerRejestracyjny);
        this.liczbaMiejsc = liczbaMiejsc;
        this.typPaliwa = Objects.requireNonNull(typPaliwa, "Typ paliwa nie może być nullem");
        this.automatycznaSkrzynia = automatycznaSkrzynia;
    }

    public int getLiczbaMiejsc() {
        return liczbaMiejsc;
    }

    public String getTypPaliwa() {
        return typPaliwa;
    }

    public boolean czyAutomatycznaSkrzynia() {
        return automatycznaSkrzynia;
    }
}
