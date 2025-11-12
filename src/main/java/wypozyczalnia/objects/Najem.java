package wypozyczalnia.objects;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.*;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;

/**
 * Klasa reprezentująca najem nieruchomości przez najemcę. Obsługuje transakcje
 * ACID i optymistyczne blokady wersji.
 */
@Entity
@Table(name = "najmy")
public class Najem {

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "najemca_id", nullable = false)
    private Najemca najemca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nieruchomosc_id", nullable = false)
    private Nieruchomosc nieruchomosc;

    @Column(name = "data_rozpoczecia", nullable = false)
    private LocalDateTime dataRozpoczecia;

    @Column(name = "data_zakonczenia", nullable = false)
    private LocalDateTime dataZakonczenia;

    @Version
    @Column(name = "version")
    private Long version;

    protected Najem() {
    }

    public Najem(Najemca najemca, Nieruchomosc nieruchomosc, LocalDateTime dataRozpoczecia, LocalDateTime dataZakonczenia) {
        this.najemca = Objects.requireNonNull(najemca, "Najemca nie może być nullem");
        this.nieruchomosc = Objects.requireNonNull(nieruchomosc, "Nieruchomość nie może być nullem");
        this.dataRozpoczecia = Objects.requireNonNull(dataRozpoczecia, "Data rozpoczęcia nie może być nullem");
        this.dataZakonczenia = Objects.requireNonNull(dataZakonczenia, "Data zakończenia nie może być nullem");

        if (dataZakonczenia.isBefore(dataRozpoczecia)) {
            throw new IllegalArgumentException("Data zakończenia musi być po dacie rozpoczęcia.");
        }
    }

    public UUID getId() {
        return id;
    }

    public Najemca getNajemca() {
        return najemca;
    }

    public Nieruchomosc getNieruchomosc() {
        return nieruchomosc;
    }

    public LocalDateTime getDataRozpoczecia() {
        return dataRozpoczecia;
    }

    public LocalDateTime getDataZakonczenia() {
        return dataZakonczenia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Najem najem = (Najem) o;
        return id.equals(najem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
