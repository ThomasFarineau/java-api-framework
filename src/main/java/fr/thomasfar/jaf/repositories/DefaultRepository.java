package fr.thomasfar.jaf.repositories;

import fr.thomasfar.jaf.annotations.Repository;

import java.util.UUID;

@Repository
public class DefaultRepository extends IRepository<String, UUID> {
}
