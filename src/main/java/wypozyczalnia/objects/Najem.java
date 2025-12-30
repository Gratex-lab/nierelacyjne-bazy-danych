package wypozyczalnia.objects;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import wypozyczalnia.objects.nieruchomosc.Nieruchomosc;

/**
 * Klasa reprezentująca najem nieruchomości przez najemcę.
 */
public class Najem {

    private UUID id;
    private UUID najemcaId;
    private String najemcaLogin;
    private UUID nieruchomoscId;
    private String nieruchomoscAdres;
    private LocalDateTime dataRozpoczecia;
    private LocalDateTime dataZakonczenia;

    public Najem() {
    }

    public Najem(Najemca najemca, Nieruchomosc nieruchomosc, LocalDateTime dataRozpoczecia, LocalDateTime dataZakonczenia) {
        Objects.requireNonNull(najemca, "Najemca nie może być nullem");
        Objects.requireNonNull(nieruchomosc, "Nieruchomość nie może być nullem");
        Objects.requireNonNull(dataRozpoczecia, "Data rozpoczęcia nie może być nullem");
        Objects.requireNonNull(dataZakonczenia, "Data zakończenia nie może być nullem");

        if (dataZakonczenia.isBefore(dataRozpoczecia)) {
            throw new IllegalArgumentException("Data zakończenia musi być po dacie rozpoczęcia.");
        }

        this.id = UUID.randomUUID();
        this.najemcaId = najemca.getId();
        this.najemcaLogin = najemca.getLogin();
        this.nieruchomoscId = nieruchomosc.getId();
        this.nieruchomoscAdres = nieruchomosc.getPelnyAdres();
        this.dataRozpoczecia = dataRozpoczecia;
        this.dataZakonczenia = dataZakonczenia;
    }

    public Najem(UUID id, UUID najemcaId, String najemcaLogin, UUID nieruchomoscId,
            String nieruchomoscAdres, LocalDateTime dataRozpoczecia, LocalDateTime dataZakonczenia) {
        this.id = id;
        this.najemcaId = najemcaId;
        this.najemcaLogin = najemcaLogin;
        this.nieruchomoscId = nieruchomoscId;
        this.nieruchomoscAdres = nieruchomoscAdres;
        this.dataRozpoczecia = dataRozpoczecia;
        this.dataZakonczenia = dataZakonczenia;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNajemcaId() {
        return najemcaId;
    }

    public void setNajemcaId(UUID najemcaId) {
        this.najemcaId = najemcaId;
    }

    public String getNajemcaLogin() {
        return najemcaLogin;
    }

    public void setNajemcaLogin(String najemcaLogin) {
        this.najemcaLogin = najemcaLogin;
    }

    public UUID getNieruchomoscId() {
        return nieruchomoscId;
    }

    public void setNieruchomoscId(UUID nieruchomoscId) {
        this.nieruchomoscId = nieruchomoscId;
    }

    public String getNieruchomoscAdres() {
        return nieruchomoscAdres;
    }

    public void setNieruchomoscAdres(String nieruchomoscAdres) {
        this.nieruchomoscAdres = nieruchomoscAdres;
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
                + ", najemcaLogin='" + najemcaLogin + '\''
                + ", nieruchomoscId=" + nieruchomoscId
                + ", nieruchomoscAdres='" + nieruchomoscAdres + '\''
                + ", dataRozpoczecia=" + dataRozpoczecia
                + ", dataZakonczenia=" + dataZakonczenia
                + '}';
    }
}
