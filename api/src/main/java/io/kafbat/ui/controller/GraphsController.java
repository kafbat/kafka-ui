package io.kafbat.ui.controller;

import io.kafbat.ui.api.GraphsApi;
import io.kafbat.ui.model.GraphDataRequestDTO;
import io.kafbat.ui.model.GraphDescriptionDTO;
import io.kafbat.ui.model.GraphDescriptionsDTO;
import io.kafbat.ui.model.GraphParameterDTO;
import io.kafbat.ui.model.PrometheusApiQueryResponseDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.prometheus.model.QueryResponse;
import io.kafbat.ui.service.graphs.GraphDescription;
import io.kafbat.ui.service.graphs.GraphsService;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class GraphsController extends AbstractController implements GraphsApi {

  private static final PrometheusApiMapper MAPPER = Mappers.getMapper(PrometheusApiMapper.class);

  @Mapper
  interface PrometheusApiMapper {
    PrometheusApiQueryResponseDTO fromClientResponse(QueryResponse resp);
  }

  private final GraphsService graphsService;

  @Override
  public Mono<ResponseEntity<PrometheusApiQueryResponseDTO>> getGraphData(String clusterName,
                                                                          Mono<GraphDataRequestDTO> graphDataRequestDto,
                                                                          ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .operationName("getGraphData")
        .build();

    return accessControlService.validateAccess(context)
        .then(
            graphDataRequestDto.flatMap(req ->
                    graphsService.getGraphData(
                        getCluster(clusterName),
                        req.getId(),
                        Optional.ofNullable(req.getFrom()).map(OffsetDateTime::toInstant).orElse(null),
                        Optional.ofNullable(req.getTo()).map(OffsetDateTime::toInstant).orElse(null),
                        req.getParameters()
                    ).map(MAPPER::fromClientResponse))
                .map(ResponseEntity::ok)
        ).doOnEach(sig -> auditService.audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<GraphDescriptionsDTO>> getGraphsList(String clusterName,
                                                                  ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .operationName("getGraphsList")
        .build();

    var graphs = graphsService.getGraphs(getCluster(clusterName));
    return accessControlService.validateAccess(context).then(
        Mono.just(ResponseEntity.ok(new GraphDescriptionsDTO().graphs(graphs.map(this::map).toList()))));
  }

  private GraphDescriptionDTO map(GraphDescription graph) {
    return new GraphDescriptionDTO()
        .id(graph.id())
        .defaultPeriod(Optional.ofNullable(graph.defaultInterval()).map(Duration::toString).orElse(null))
        .type(graph.isRange() ? GraphDescriptionDTO.TypeEnum.RANGE : GraphDescriptionDTO.TypeEnum.INSTANT)
        .parameters(graph.params().stream().map(GraphParameterDTO::new).toList());
  }
}
