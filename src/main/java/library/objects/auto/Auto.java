package library.objects.auto;

import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.*;

/**
 * Abstrakcyjna klasa reprezentująca auto w wypożyczalni. Implementuje
 * dziedziczenie typu JOINED dla różnych typów samochodów.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "auta")
public abstract class Auto {

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "marka", nullable = false)
    private String marka;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "numer_rejestracyjny", unique = true, nullable = false, length = 10)
    private String numerRejestracyjny;

    @Version
    @Column(name = "version")
    private Long version;

    protected Auto() {
    }

    public Auto(String marka, String model, String numerRejestracyjny) {
        this.marka = Objects.requireNonNull(marka, "Marka nie może być nullem");
        this.model = Objects.requireNonNull(model, "Model nie może być nullem");
        this.numerRejestracyjny = Objects.requireNonNull(numerRejestracyjny, "Numer rejestracyjny nie może być nullem");
    }

    public UUID getId() {
        return id;
    }

    public String getMarka() {
        return marka;
    }

    public String getModel() {
        return model;
    }

    public String getNumerRejestracyjny() {
        return numerRejestracyjny;
    }

    /**
     * Zwraca pełną nazwę auta
     */
    public String getPelnaNazwa() {
        return marka + " " + model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Auto)) {
            return false;
        }
        Auto auto = (Auto) o;
        return id.equals(auto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
