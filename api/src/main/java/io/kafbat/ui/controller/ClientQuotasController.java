package io.kafbat.ui.controller;

import static java.util.stream.Collectors.toMap;

import io.kafbat.ui.api.ClientQuotasApi;
import io.kafbat.ui.model.ClientQuotasDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.permission.ClientQuotaAction;
import io.kafbat.ui.service.mcp.McpTool;
import io.kafbat.ui.service.quota.ClientQuotaRecord;
import io.kafbat.ui.service.quota.ClientQuotaService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class ClientQuotasController extends AbstractController implements ClientQuotasApi, McpTool {

  private static final Comparator<ClientQuotaRecord> QUOTA_RECORDS_COMPARATOR =
          Comparator.nullsLast(
            Comparator.comparing(ClientQuotaRecord::user, Comparator.nullsLast(String::compareTo))
          )
          .thenComparing(Comparator.nullsLast(
              Comparator.comparing(
                  ClientQuotaRecord::clientId,
                  Comparator.nullsLast(String::compareTo)
              )
          ))
          .thenComparing(Comparator.nullsLast(
              Comparator.comparing(
                  ClientQuotaRecord::ip,
                  Comparator.nullsLast(String::compareTo)
              )
          ));

  private final ClientQuotaService clientQuotaService;

  @Override
  public Mono<ResponseEntity<Flux<ClientQuotasDTO>>> listQuotas(String clusterName,
                                                                ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .operationName("listClientQuotas")
        .clientQuotaActions(ClientQuotaAction.VIEW)
        .build();

    Mono<ResponseEntity<Flux<ClientQuotasDTO>>> operation =
        Mono.just(
            clientQuotaService.getAll(getCluster(clusterName))
                .sort(QUOTA_RECORDS_COMPARATOR)
                .map(this::mapToDto)
        ).map(ResponseEntity::ok);

    return validateAccess(context)
        .then(operation)
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Void>> upsertClientQuotas(String clusterName,
                                                       Mono<ClientQuotasDTO> quotasDto,
                                                       ServerWebExchange exchange) {
    return quotasDto.flatMap(
        quotasUpd -> {
          var context = AccessContext.builder()
              .cluster(clusterName)
              .operationName("upsertClientQuotas")
              .operationParams(Map.of("quotasUpdate", quotasUpd))
              .clientQuotaActions(ClientQuotaAction.EDIT)
              .build();

          Mono<ResponseEntity<Void>> operation = clientQuotaService.upsert(
                  getCluster(clusterName),
                  quotasUpd.getUser(),
                  quotasUpd.getClientId(),
                  quotasUpd.getIp(),
                  Optional.ofNullable(quotasUpd.getQuotas()).orElse(Map.of())
                      .entrySet()
                      .stream()
                      .collect(toMap(Map.Entry::getKey, e -> e.getValue().doubleValue()))
              )
              .map(statusCode -> ResponseEntity.status(statusCode).build());

          return validateAccess(context)
              .then(operation)
              .doOnEach(sig -> audit(context, sig));
        }
    );
  }

  private ClientQuotasDTO mapToDto(ClientQuotaRecord quotaRecord) {
    return new ClientQuotasDTO()
        .user(quotaRecord.user())
        .clientId(quotaRecord.clientId())
        .ip(quotaRecord.ip())
        .quotas(
            quotaRecord.quotas().entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(toMap(
                    Map.Entry::getKey,
                    e -> BigDecimal.valueOf(e.getValue()),
                    (v1, v2) -> null, //won't be called
                    LinkedHashMap::new //to keep order
                ))
        );
  }

}
