package wypozyczalnia.objects;

import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.*;

/**
 * Klasa reprezentująca najemcę wypożyczalni nieruchomości. Zawiera mechanizm
 * aktywacji/deaktywacji konta oraz optymistyczne blokady wersji.
 */
@Entity
@Table(name = "najemcy")
public class Najemca {

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "login", unique = true, nullable = false, length = 50)
    private String login;

    @Column(name = "czy_aktywny", nullable = false)
    private boolean aktywny;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Konstruktor bez argumentów wymagany przez JPA
     */
    protected Najemca() {
        this.login = null;
        this.aktywny = true;
    }

    public Najemca(String login) {
        this.login = Objects.requireNonNull(login, "Login nie może być nullem.");
        this.aktywny = true;
    }

    public UUID getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public boolean czyAktywny() {
        return aktywny;
    }

    public void setAktywny(boolean aktywny) {
        this.aktywny = aktywny;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Najemca najemca = (Najemca) o;
        return id.equals(najemca.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
