package io.kafbat.ui.repository;

import io.kafbat.ui.model.sainsburys.dynamo.DynamoMaskingEntity;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@EnableScan
@Repository
public interface DynamoMaskingEntityRepository extends CrudRepository<DynamoMaskingEntity, String> {
  List<DynamoMaskingEntity> findAll();
}
