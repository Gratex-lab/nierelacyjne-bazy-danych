package library.objects.auto;

import java.util.Objects;
import jakarta.persistence.*;

/**
 * Klasa reprezentująca samochód ciężarowy w wypożyczalni. Dziedziczy po klasie
 * Auto.
 */
@Entity
@Table(name = "auta_ciezarowe")
@PrimaryKeyJoinColumn(name = "auto_id")
public class SamochodCiezarowy extends Auto {

    @Column(name = "ladownosc_kg", nullable = false)
    private int ladownoscKg;

    @Column(name = "kategoria_prawa_jazdy", nullable = false, length = 5)
    private String kategoriaPrawaJazdy;

    @Column(name = "czy_z_pryczepą", nullable = false)
    private boolean czyPrzyczepa;

    protected SamochodCiezarowy() {
    }

    public SamochodCiezarowy(String marka, String model, String numerRejestracyjny,
            int ladownoscKg, String kategoriaPrawaJazdy, boolean czyZPrzyczepa) {
        super(marka, model, numerRejestracyjny);
        this.ladownoscKg = ladownoscKg;
        this.kategoriaPrawaJazdy = Objects.requireNonNull(kategoriaPrawaJazdy, "Kategoria prawa jazdy nie może być nullem");
        this.czyPrzyczepa = czyZPrzyczepa;
    }

    public int getLadownoscKg() {
        return ladownoscKg;
    }

    public String getKategoriaPrawaJazdy() {
        return kategoriaPrawaJazdy;
    }

    public boolean isCzyPrzyczepa() {
        return czyPrzyczepa;
    }
}
