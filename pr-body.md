## Description

This PR introduces a simpler way to obtain OAuth2 session cookies for programmatic API access, addressing issue #532.

## Changes

### New endpoint: POST /api/token
- Accepts `client_id`, `client_secret`, and optional `scope` parameters
- Exchanges credentials directly with the OAuth2 provider's token endpoint
- Returns the access token for use as a Bearer token in subsequent API requests

### Configuration
- New `apiTokenEnabled` property on `OAuth2Provider` (defaults to `false`) to opt-in to programmatic access
- `/api/token` added to auth whitelist for unauthenticated access

### JWT Bearer Authentication
- When a JWK Set URI is available from the OAuth2 provider configuration, JWT bearer authentication is automatically enabled
- Clients can use tokens obtained from `/api/token` directly in `Authorization: Bearer <token>` headers

## Usage Example

```bash
# Obtain token
TOKEN=$(curl -X POST http://localhost:8080/api/token \
  -d "client_id=your-client-id" \
  -d "client_secret=your-client-secret" | jq -r '.access_token')

# Use token for API calls
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/clusters
```

Fixed the issue as described. Happy to add contributor credit if desired.