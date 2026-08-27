package io.kafbat.ui.mapper;

import io.kafbat.ui.model.InternalQuorumInfo;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.QuorumInfo;
import org.mapstruct.Mapper;

@Mapper(
    componentModel = "spring"
)
public interface QuorumInfoMapper {

  default InternalQuorumInfo toInternalQuorumInfo(QuorumInfo i) {
    return new InternalQuorumInfo(
        i.leaderId(),
        i.leaderEpoch(),
        i.highWatermark(),
        i.voters().stream().map(this::toReplicaState).toList(),
        i.observers().stream().map(this::toReplicaState).toList(),
        i.nodes().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> toNode(e.getValue())
            ))
    );
  }

  default InternalQuorumInfo.ReplicaState toReplicaState(QuorumInfo.ReplicaState src) {
    return new InternalQuorumInfo.ReplicaState(
        src.replicaId(),
        src.replicaDirectoryId().toString(),
        src.logEndOffset(),
        src.lastFetchTimestamp(),
        src.lastCaughtUpTimestamp()
    );
  }

  InternalQuorumInfo.Node toNode(QuorumInfo.Node node);

}
