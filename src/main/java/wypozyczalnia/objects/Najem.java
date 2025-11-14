package wypozyczalnia.objects;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Klasa reprezentująca najem nieruchomości przez najemcę jako dokument MongoDB.
 * Obsługuje transakcje i spójność danych.
 */
public class Najem {

    @BsonId
    private ObjectId id;

    @BsonProperty("najemcaId")
    private ObjectId najemcaId;

    @BsonProperty("nieruchomoscId")
    private ObjectId nieruchomoscId;

    @BsonProperty("dataRozpoczecia")
    private LocalDateTime dataRozpoczecia;

    @BsonProperty("dataZakonczenia")
    private LocalDateTime dataZakonczenia;

    // Transient fields - nie będą serializowane do MongoDB
    private transient Najemca najemca;
    private transient Nieruchomosc nieruchomosc;

    public Najem() {
    }

    public Najem(Najemca najemca, Nieruchomosc nieruchomosc, LocalDateTime dataRozpoczecia, LocalDateTime dataZakonczenia) {
        this.najemca = Objects.requireNonNull(najemca, "Najemca nie może być nullem");
        this.nieruchomosc = Objects.requireNonNull(nieruchomosc, "Nieruchomość nie może być nullem");
        this.najemcaId = najemca.getId();
        this.nieruchomoscId = nieruchomosc.getId();
        this.dataRozpoczecia = Objects.requireNonNull(dataRozpoczecia, "Data rozpoczęcia nie może być nullem");
        this.dataZakonczenia = Objects.requireNonNull(dataZakonczenia, "Data zakończenia nie może być nullem");

        if (dataZakonczenia.isBefore(dataRozpoczecia)) {
            throw new IllegalArgumentException("Data zakończenia musi być po dacie rozpoczęcia.");
        }
    }

    @BsonCreator
    public Najem(
            @BsonProperty("najemcaId") ObjectId najemcaId,
            @BsonProperty("nieruchomoscId") ObjectId nieruchomoscId,
            @BsonProperty("dataRozpoczecia") LocalDateTime dataRozpoczecia,
            @BsonProperty("dataZakonczenia") LocalDateTime dataZakonczenia) {
        this.najemcaId = Objects.requireNonNull(najemcaId, "ID najemcy nie może być nullem");
        this.nieruchomoscId = Objects.requireNonNull(nieruchomoscId, "ID nieruchomości nie może być nullem");
        this.dataRozpoczecia = Objects.requireNonNull(dataRozpoczecia, "Data rozpoczęcia nie może być nullem");
        this.dataZakonczenia = Objects.requireNonNull(dataZakonczenia, "Data zakończenia nie może być nullem");

        if (dataZakonczenia.isBefore(dataRozpoczecia)) {
            throw new IllegalArgumentException("Data zakończenia musi być po dacie rozpoczęcia.");
        }
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getNajemcaId() {
        return najemcaId;
    }

    public void setNajemcaId(ObjectId najemcaId) {
        this.najemcaId = najemcaId;
    }

    public ObjectId getNieruchomoscId() {
        return nieruchomoscId;
    }

    public void setNieruchomoscId(ObjectId nieruchomoscId) {
        this.nieruchomoscId = nieruchomoscId;
    }

    public Najemca getNajemca() {
        return najemca;
    }

    public void setNajemca(Najemca najemca) {
        this.najemca = najemca;
        if (najemca != null) {
            this.najemcaId = najemca.getId();
        }
    }

    public Nieruchomosc getNieruchomosc() {
        return nieruchomosc;
    }

    public void setNieruchomosc(Nieruchomosc nieruchomosc) {
        this.nieruchomosc = nieruchomosc;
        if (nieruchomosc != null) {
            this.nieruchomoscId = nieruchomosc.getId();
        }
    }

    public LocalDateTime getDataRozpoczecia() {
        return dataRozpoczecia;
    }

    public void setDataRozpoczecia(LocalDateTime dataRozpoczecia) {
        this.dataRozpoczecia = dataRozpoczecia;
    }

    public LocalDateTime getDataZakonczenia() {
        return dataZakonczenia;
    }

    public void setDataZakonczenia(LocalDateTime dataZakonczenia) {
        this.dataZakonczenia = dataZakonczenia;
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
        return Objects.equals(id, najem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Najem{"
                + "id=" + id
                + ", najemcaId=" + najemcaId
                + ", nieruchomoscId=" + nieruchomoscId
                + ", dataRozpoczecia=" + dataRozpoczecia
                + ", dataZakonczenia=" + dataZakonczenia
                + '}';
    }
}
