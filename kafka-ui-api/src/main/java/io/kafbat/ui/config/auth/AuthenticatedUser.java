package io.kafbat.ui.config.auth;

import java.util.Collection;

public record AuthenticatedUser(String principal, Collection<String> groups) {

}
