package io.kafbat.ui.controller;

import io.kafbat.ui.api.KsqlApi;
import io.kafbat.ui.model.KsqlCommandV2DTO;
import io.kafbat.ui.model.KsqlCommandV2ResponseDTO;
import io.kafbat.ui.model.KsqlResponseDTO;
import io.kafbat.ui.model.KsqlStreamDescriptionDTO;
import io.kafbat.ui.model.KsqlTableDescriptionDTO;
import io.kafbat.ui.model.KsqlTableResponseDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.permission.KsqlAction;
import io.kafbat.ui.service.ksql.KsqlServiceV2;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class KsqlController extends AbstractController implements KsqlApi {

  private final KsqlServiceV2 ksqlServiceV2;

  @Override
  public Mono<ResponseEntity<KsqlCommandV2ResponseDTO>> executeKsql(String clusterName,
                                                                    Mono<KsqlCommandV2DTO> ksqlCmdDo,
                                                                    ServerWebExchange exchange) {
    return ksqlCmdDo.flatMap(
            command -> {
              var context = AccessContext.builder()
                  .cluster(clusterName)
                  .ksqlActions(KsqlAction.EXECUTE)
                  .operationName("executeKsql")
                  .operationParams(command)
                  .build();
              return validateAccess(context).thenReturn(
                      new KsqlCommandV2ResponseDTO().pipeId(
                          ksqlServiceV2.registerCommand(
                              getCluster(clusterName),
                              command.getKsql(),
                              Optional.ofNullable(command.getStreamsProperties()).orElse(Map.of()))))
                  .doOnEach(sig -> audit(context, sig));
            }
        )
        .map(ResponseEntity::ok);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Mono<ResponseEntity<Flux<KsqlResponseDTO>>> openKsqlResponsePipe(String clusterName,
                                                                          String pipeId,
                                                                          ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .ksqlActions(KsqlAction.EXECUTE)
        .operationName("openKsqlResponsePipe")
        .build();

    return validateAccess(context).thenReturn(
        ResponseEntity.ok(ksqlServiceV2.execute(pipeId)
            .map(table -> new KsqlResponseDTO()
                .table(
                    new KsqlTableResponseDTO()
                        .header(table.getHeader())
                        .columnNames(table.getColumnNames())
                        .values((List<List<Object>>) ((List<?>) (table.getValues()))))))
    );
  }

  @Override
  public Mono<ResponseEntity<Flux<KsqlStreamDescriptionDTO>>> listStreams(String clusterName,
                                                                          ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .ksqlActions(KsqlAction.EXECUTE)
        .operationName("listStreams")
        .build();

    return validateAccess(context)
        .thenReturn(ResponseEntity.ok(ksqlServiceV2.listStreams(getCluster(clusterName))))
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Flux<KsqlTableDescriptionDTO>>> listTables(String clusterName,
                                                                        ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .ksqlActions(KsqlAction.EXECUTE)
        .operationName("listTables")
        .build();

    return validateAccess(context)
        .thenReturn(ResponseEntity.ok(ksqlServiceV2.listTables(getCluster(clusterName))))
        .doOnEach(sig -> audit(context, sig));
  }
}
