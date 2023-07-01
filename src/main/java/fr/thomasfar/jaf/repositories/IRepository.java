package fr.thomasfar.jaf.repositories;

import java.util.HashMap;
import java.util.Optional;

public class IRepository<T, ID> implements Repository<T, ID> {

    private final HashMap<ID, T> storage = new HashMap<>();

    @Override
    public Iterable<T> findAll() {
        return storage.values();
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public void deleteById(ID id) {
        storage.remove(id);
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }

    @Override
    public boolean existsById(ID id) {
        return storage.containsKey(id);
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public <S extends T> void save(ID id, S entity) {
        storage.put(id, entity);
    }
}
