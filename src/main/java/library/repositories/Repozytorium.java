package library.repositories;

import java.util.UUID;

/**
 * Generyczny interfejs repozytorium dla operacji CRUD. Implementuje wzorzec
 * Repository dla wszystkich encji.
 */
public interface Repozytorium<T> {

    /**
     * Dodaje nową encję do bazy danych.
     */
    void dodaj(T obiekt);

    /**
     * Usuwa encję z bazy danych.
     */
    void usun(T obiekt);

    /**
     * Znajduje encję po jej UUID.
     */
    T znajdz(UUID uuid);
}
