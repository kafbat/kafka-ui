# PKCE built into Kafbat UI (source-code implementation)

This directory documents the **in-application** PKCE implementation on branch
[`issue/1721`](https://github.com/diemar-consulting/kafka-ui/tree/issue/1721).
The alternative, standalone-JAR implementation lives on
[`pkce-bean`](https://github.com/diemar-consulting/kafka-ui/tree/pkce-bean).

## Why

PKCE (RFC 7636) protects the OAuth2 authorization-code flow against interception
and code substitution, including when the OAuth2 client has a client secret
(confidential client). The authorization request includes a
`code_challenge` and `code_challenge_method=S256`; the subsequent token
request includes the corresponding `code_verifier`.

## Source changes

- `api/src/main/java/io/kafbat/ui/config/auth/OAuthSecurityConfig.java`:
  adds a reactive `DefaultServerOAuth2AuthorizationRequestResolver` bean,
  configures it with `OAuth2AuthorizationRequestCustomizers.withPkce()`, and
  explicitly attaches that resolver to the existing reactive
  `oauth2Login` filter-chain configuration.
- The application's existing authentication managers, OIDC/OAuth2 user
  services, RBAC integration, and token-response client remain in place.
- PKCE is applied to authorization-code login, including confidential clients;
  no client secret is removed or changed.

This approach **requires building and deploying a new Kafbat application JAR**.
It does not enable PKCE through a separate extension JAR.

## Tests

- `api/src/test/java/io/kafbat/ui/config/auth/OAuthPkceTest.java`:
  checks that a confidential client produces an S256 challenge matching the
  locally generated verifier, and that independent authorization requests
  generate distinct verifier/state values.
- `api/src/test/java/io/kafbat/ui/config/auth/OAuthPkceIntegrationTest.java`:
  uses a mock OAuth identity provider (WireMock) to validate that the
  authorization request's challenge matches the verifier sent by the token
  exchange client, and that a token response is processed.

Run locally with the repository's Java version (`.java-version`):

```bash
./gradlew :api:check
```

CI validation on this branch:

- [PKCE branch check — successful run](https://github.com/diemar-consulting/kafka-ui/actions/runs/39936880026)
- [Backend build and tests — successful run](https://github.com/diemar-consulting/kafka-ui/actions/runs/39936885381)

Both passed on commit `b9b322e`. These are automated tests against a mock
identity provider; acceptance with a production identity provider is a
separate deployment check.

## Deployment and verification

1. Configure Kafbat's existing `auth.type=OAUTH2` and
   `auth.oauth2.client.*` provider settings as usual.
2. Build and deploy the Kafbat application from this branch.
3. Start a login and inspect the redirect to the identity provider: verify
   `code_challenge` and `code_challenge_method=S256`.
4. Confirm successful authorization-code exchange and login. Never expose
   `code_verifier` or the client secret in production logs.

## Which implementation?

Choose **this branch** when you can rebuild Kafbat and prefer PKCE to live
directly in its security configuration.

Choose **[`pkce-bean`](https://github.com/diemar-consulting/kafka-ui/tree/pkce-bean/pkce-starter)**
when you need to keep the original Kafbat application binary unchanged and
inject PKCE using an external starter JAR and an adjusted launch command.
