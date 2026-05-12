package io.kafbat.ui.model;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public record InternalQuorumInfo(int leaderId,
                                 long leaderEpoch,
                                 long highWatermark,
                                 List<ReplicaState> voters,
                                 List<ReplicaState> observers,
                                 Map<Integer, Node> nodes) {

  public record ReplicaState(int replicaId,
                             String replicaDirectoryId,
                             long logEndOffset,
                             OptionalLong lastFetchTimestamp,
                             OptionalLong lastCaughtUpTimestamp) {
  }

  public record Node(int nodeId, List<RaftVoterEndpoint> endpoints) {
  }

  public record RaftVoterEndpoint(
      String name,
      String host,
      int port
  ) {
  }
}
