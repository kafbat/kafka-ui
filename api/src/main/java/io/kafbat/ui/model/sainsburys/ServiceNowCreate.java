package io.kafbat.ui.model.sainsburys;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceNowCreate implements Serializable {

  private String u_assigned_to;
  private String u_assignment_group;
  private String u_business_service;
  private String u_caller_id;
  private String u_category;
  private String u_subcategory;
  private String u_cmdb_ci;
  private String u_comments;
  private String u_description;
  private int u_impact;
  private int u_urgency;
  private String u_impacted_parties;
  private String u_location_not_found;
  private String u_undefined_location;
  private String u_short_description;
  private int u_state;
  private String u_work_notes;
}
