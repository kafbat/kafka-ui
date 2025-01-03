package io.kafbat.ui.model;

import lombok.Builder;

@Builder
public record InternalReplica(int broker, boolean leader, boolean inSync) {
}
