package wypozyczalnia.objects.nieruchomosc;

import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.*;

/**
 * Abstrakcyjna klasa reprezentująca nieruchomość w wypożyczalni. Implementuje
 * dziedziczenie typu JOINED dla różnych typów nieruchomości.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "nieruchomosci")
public abstract class Nieruchomosc {

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "miasto", nullable = false)
    private String miasto;

    @Column(name = "dzielnica", nullable = false)
    private String dzielnica;

    @Column(name = "adres", unique = true, nullable = false, length = 100)
    private String adres;

    @Version
    @Column(name = "version")
    private Long version;

    protected Nieruchomosc() {
    }

    public Nieruchomosc(String miasto, String dzielnica, String adres) {
        this.miasto = Objects.requireNonNull(miasto, "Miasto nie może być nullem");
        this.dzielnica = Objects.requireNonNull(dzielnica, "Dzielnica nie może być nullem");
        this.adres = Objects.requireNonNull(adres, "Adres nie może być nullem");
    }

    public UUID getId() {
        return id;
    }

    public String getMiasto() {
        return miasto;
    }

    public String getDzielnica() {
        return dzielnica;
    }

    public String getAdres() {
        return adres;
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
        return id.equals(nieruchomosc.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
