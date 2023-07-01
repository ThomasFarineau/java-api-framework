package fr.thomasfar.jaf.repositories;

import java.util.Optional;

public interface Repository<T, ID> {
    Iterable<T> findAll();

    Optional<T> findById(ID id);

    void deleteById(ID id);

    void deleteAll();

    boolean existsById(ID id);

    long count();

    <S extends T> void save(ID id, S entity);
}
