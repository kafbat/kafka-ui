package io.kafbat.ui.service.sainsburys;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.Subject;
import io.kafbat.ui.model.sainsburys.dynamo.DynamoMaskingEntity;
import io.kafbat.ui.model.sainsburys.dynamo.DynamoPermission;
import io.kafbat.ui.model.sainsburys.dynamo.DynamoRbacEntity;
import io.kafbat.ui.model.sainsburys.dynamo.DynamoSubject;
import io.kafbat.ui.repository.DynamoMaskingEntityRepository;
import io.kafbat.ui.repository.DynamoRbacEntityRepository;
import io.kafbat.ui.service.masking.DataMasking;
import io.kafbat.ui.service.masking.policies.MaskingPolicy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamoClusterProperties {
  private final DynamoMaskingEntityRepository maskingEntityRepository;
  private final DynamoRbacEntityRepository rbacEntityRepository;
  private List<DynamoMaskingEntity> maskingEntityList;

  @PostConstruct
  public void loadConfiguration(){
    this.maskingEntityList = maskingEntityRepository.findAll();
  }

  public void loadMaskingConfiguration(){
    this.maskingEntityList = maskingEntityRepository.findAll();
  }

  public List<ClustersProperties.Masking> retrieveDynamoMaskingToMaskingList(String cluster){
    return this.maskingEntityList.stream()
        .filter(mask->mask.getName().contains(cluster))
        .map(this::mapperDynamoMaskingToMasking)
        .toList();
  }

  private ClustersProperties.Masking mapperDynamoMaskingToMasking(DynamoMaskingEntity source){
    ClustersProperties.Masking masking = new ClustersProperties.Masking();
    masking.setMaskingCharsReplacement(source.getMaskingCharsReplacement());
    masking.setType(ClustersProperties.Masking.Type.valueOf(source.getType()));
    masking.setReplacement(source.getReplacement());
    masking.setFields(source.getFields());
    masking.setFieldsNamePattern(source.getFieldsNamePattern());
    masking.setTopicValuesPattern(source.getTopicValuesPattern());
    masking.setTopicKeysPattern(source.getTopicKeysPattern());

    return masking;
  }

  public List<DataMasking.Mask> retrieveDynamoMasks(String cluster){
    List<DataMasking.Mask> maskList = new ArrayList<>();
    retrieveDynamoMaskingToMaskingList(cluster).forEach(p->{
      DataMasking.Mask mask = new DataMasking.Mask(
          Optional.ofNullable(p.getTopicKeysPattern()).map(Pattern::compile).orElse(null),
          Optional.ofNullable(p.getTopicValuesPattern()).map(Pattern::compile).orElse(null),
          MaskingPolicy.create(p)
      );

      maskList.add(mask);
    });

    return maskList;
  }

  public List<Role> retrieveDynamoRBACUserRoles(){
    return this.rbacEntityRepository.findAll().stream()
        .map(this::mapperDynamoRbacToRole)
        .toList();
  }

  private Role mapperDynamoRbacToRole(DynamoRbacEntity source){
    Role role = new Role();
    role.setName(source.getName());
    role.setClusters(source.getClusters());
    role.setPermissions(source.getPermissions().stream().map(this::mapperDynamoPermissionToPermission).toList());
    role.setSubjects(source.getSubjects().stream().map(this::mapperDynamoSubjectToSubject).toList());

    return role;
  }

  private Permission mapperDynamoPermissionToPermission(DynamoPermission source){
    Permission permission = new Permission();
    permission.setResource(source.getResource());
    permission.setActions(source.getActions());
    permission.setValue(source.getValue());
    permission.transform();

    return permission;
  }

  private Subject mapperDynamoSubjectToSubject(DynamoSubject source){
    Subject subject = new Subject();
    subject.setValue(source.getValue());
    subject.setRegex(source.isRegex());
    subject.setType(source.getType());
    subject.setProvider(source.getProvider());
    subject.setCreatedTime(source.getCreatedTime());
    subject.setExpiryTime(source.getExpiryTime());

    return subject;
  }
}
