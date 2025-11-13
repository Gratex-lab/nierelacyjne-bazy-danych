package wypozyczalnia.repositories;

import org.bson.types.ObjectId;

/**
 * Generyczny interfejs repozytorium dla operacji CRUD w MongoDB. Implementuje
 * wzorzec Repository dla wszystkich encji.
 */
public interface Repozytorium<T> {

    /**
     * Dodaje nową encję do bazy danych.
     */
    void dodaj(T obiekt);

    /**
     * Znajdź encję po jej ObjectId.
     */
    T znajdz(ObjectId id);

    /**
     * Aktualizuje istniejącą encję w bazie danych.
     */
    void aktualizuj(T obiekt);

    /**
     * Usuwa encję z bazy danych.
     */
    void usun(T obiekt);
}
