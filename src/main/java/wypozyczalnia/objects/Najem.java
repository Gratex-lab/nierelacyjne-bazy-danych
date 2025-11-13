package wypozyczalnia.objects;

import org.bson.Document;
import org.bson.types.ObjectId;
import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

/**
 * Klasa reprezentująca najem nieruchomości przez najemcę jako dokument MongoDB.
 * Obsługuje transakcje i spójność danych.
 */
public class Najem {

    private ObjectId id;
    private ObjectId najemcaId;
    private ObjectId nieruchomoscId;
    private LocalDateTime dataRozpoczecia;
    private LocalDateTime dataZakonczenia;

    private Najemca najemca;
    private Nieruchomosc nieruchomosc;

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

    /**
     * Konwertuje obiekt do dokumentu MongoDB
     */
    public Document toDocument() {
        Document document = new Document();
        if (id != null) {
            document.append("_id", id);
        }
        document.append("najemcaId", najemcaId)
                .append("nieruchomoscId", nieruchomoscId)
                .append("dataRozpoczecia", Date.from(dataRozpoczecia.withNano(0).atZone(ZoneId.systemDefault()).toInstant()))
                .append("dataZakonczenia", Date.from(dataZakonczenia.withNano(0).atZone(ZoneId.systemDefault()).toInstant()));
        return document;
    }

    /**
     * Tworzy obiekt z dokumentu MongoDB
     */
    public static Najem fromDocument(Document document) {
        if (document == null) {
            return null;
        }

        Najem najem = new Najem();
        najem.setId(document.getObjectId("_id"));
        najem.setNajemcaId(document.getObjectId("najemcaId"));
        najem.setNieruchomoscId(document.getObjectId("nieruchomoscId"));

        Date dataRozpoczecia = document.getDate("dataRozpoczecia");
        Date dataZakonczenia = document.getDate("dataZakonczenia");

        if (dataRozpoczecia != null) {
            najem.setDataRozpoczecia(LocalDateTime.ofInstant(dataRozpoczecia.toInstant(), ZoneId.systemDefault()).withNano(0));
        }
        if (dataZakonczenia != null) {
            najem.setDataZakonczenia(LocalDateTime.ofInstant(dataZakonczenia.toInstant(), ZoneId.systemDefault()).withNano(0));
        }

        return najem;
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
