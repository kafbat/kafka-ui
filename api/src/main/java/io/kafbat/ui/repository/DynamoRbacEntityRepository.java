package io.kafbat.ui.repository;

import io.kafbat.ui.model.rbac.DynamoRbacEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DynamoRbacEntityRepository extends CrudRepository<DynamoRbacEntity, String> {
}
