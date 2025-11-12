package library.objects;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.*;
import library.objects.auto.Auto;

/**
 * Klasa reprezentująca wypożyczenie auta przez klienta. Obsługuje transakcje
 * ACID i optymistyczne blokady wersji.
 */
@Entity
@Table(name = "wypozyczenia")
public class Wypozyczenie {

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "klient_id", nullable = false)
    private Klient klient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auto_id", nullable = false)
    private Auto auto;

    @Column(name = "data_rozpoczecia", nullable = false)
    private LocalDateTime dataRozpoczecia;

    @Column(name = "data_zakonczenia", nullable = false)
    private LocalDateTime dataZakonczenia;

    @Version
    @Column(name = "version")
    private Long version;

    protected Wypozyczenie() {
    }

    public Wypozyczenie(Klient klient, Auto auto, LocalDateTime dataRozpoczecia, LocalDateTime dataZakonczenia) {
        this.klient = Objects.requireNonNull(klient, "Klient nie może być nullem");
        this.auto = Objects.requireNonNull(auto, "Auto nie może być nullem");
        this.dataRozpoczecia = Objects.requireNonNull(dataRozpoczecia, "Data rozpoczęcia nie może być nullem");
        this.dataZakonczenia = Objects.requireNonNull(dataZakonczenia, "Data zakończenia nie może być nullem");

        if (dataZakonczenia.isBefore(dataRozpoczecia)) {
            throw new IllegalArgumentException("Data zakończenia musi być po dacie rozpoczęcia.");
        }
    }

    public UUID getId() {
        return id;
    }

    public Klient getKlient() {
        return klient;
    }

    public Auto getAuto() {
        return auto;
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
        Wypozyczenie wypozyczenie = (Wypozyczenie) o;
        return id.equals(wypozyczenie.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
