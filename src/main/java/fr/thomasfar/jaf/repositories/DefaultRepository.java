package fr.thomasfar.jaf.repositories;

import fr.thomasfar.jaf.annotations.Repository;
import fr.thomasfar.jaf.entities.DefaultEntity;

import java.util.UUID;

@Repository
public class DefaultRepository extends IRepository<DefaultEntity, UUID> {
}
