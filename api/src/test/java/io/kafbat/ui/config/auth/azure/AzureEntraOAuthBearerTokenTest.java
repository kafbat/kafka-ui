package io.kafbat.ui.config.auth.azure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azure.core.credential.AccessToken;
import java.time.OffsetDateTime;
import java.util.Set;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.junit.jupiter.api.Test;

public class AzureEntraOAuthBearerTokenTest {

  // These are not real tokens. It was generated using fake values with an invalid signature,
  // so it is safe to store here.
  private static final String VALID_SAMPLE_TOKEN =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IjlHbW55RlBraGMzaE91UjIybXZTdmduTG83WSIsImtpZCI6IjlHbW55"
          + "RlBraGMzaE91UjIybXZTdmduTG83WSJ9.eyJhdWQiOiJodHRwczovL3NhbXBsZS5zZXJ2aWNlYnVzLndpbmRvd3MubmV0IiwiaX"
          + "NzIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvc2FtcGxlLyIsImlhdCI6MTY5ODQxNTkxMiwibmJmIjoxNjk4NDE1OTEzLCJleH"
          + "AiOjE2OTg0MTU5MTQsImFjciI6IjEiLCJhaW8iOiJzYW1wbGUtYWlvIiwiYW1yIjpbXSwiYXBwaWQiOiJzYW1wbGUtYXBwLWlkIi"
          + "wiYXBwaWRhY3IiOiIwIiwiZmFtaWx5X25hbWUiOiJTYW1wbGUiLCJnaXZlbl9uYW1lIjoiU2FtcGxlIiwiZ3JvdXBzIjpbXSwiaX"
          + "BhZGRyIjoiMTI3LjAuMC4xIiwibmFtZSI6IlNhbXBsZSBOYW1lIiwib2lkIjoic2FtcGxlLW9pZCIsIm9ucHJlbV9zaWQiOiJzYW"
          + "1wbGUtb25wcmVtX3NpZCIsInB1aWQiOiJzYW1wbGUtcHVpZCIsInJoIjoic2FtcGxlLXJoIiwic2NwIjoiZXZlbnRfaHViIHN0b3"
          + "JhZ2VfYWNjb3VudCIsInN1YiI6IlNhbXBsZSBTdWJqZWN0IiwidGlkIjoic2FtcGxlLXRpZCIsInVuaXF1ZV9uYW1lIjoic2FtcG"
          + "xlQG1pY3Jvc29mdC5jb20iLCJ1cG4iOiJzYW1wbGVAbWljcm9zb2Z0LmNvbSIsInV0aSI6InNhbXBsZS11dGkiLCJ2ZXIiOiIxLj"
          + "AiLCJ3aWRzIjpbXX0.DC_guYOsDlRc5GsXE39dn_zlBX54_Y8_mDTLXLgienl9dPMX5RE2X1QXGXA9ukZtptMzP_0wcoqDDjNrys"
          + "GrNhztyeOr0YSeMMFq2NQ5vMBzLapwONwsnv55Hn0jOje9cqnMf43z1LHI6q6-rIIRz-SiTuoYUgOTxzFftpt-7FSqLjQpYEH7bL"
          + "p-0yIU_aJUSb5HQTJbtYYOb54hsZ6VXpaiZ013qGtKODbHTG37kdoIw2MPn66CxanLZKeZM31IVxC-duAqxDgK4O2Ne6xRZRIPW1"
          + "yt61QnZutWTJ4bAyhmplym3OWZ369cyiSJek0uyS5tibXeCYG4Kk8UQSFcsyfwgOsD0xvvcXcLexcUcEekoNBj6ixDhWssFzhC8T"
          + "Npy8-QKNe_Tp6qHzJdI6OV71jpDkGvcmseLHC9GOxBWB0IdYbePTFK-rz2dkN3uMUiFwQJvEbORsq1IaQXj2esT0F7sMfqzWQF9h"
          + "koVy4mJg_auvrZlnQkNPdLHfCacU33ZPwtuSS6b-0XolbxZ5DlJ4p1OJPeHl2xsi61qiHuCBsmnkLNtHmyxNTXGs7xc4dEQokaCK"
          + "-FB_lzC3D4mkJMxKWopQGXnQtizaZjyclGpiUFs3mEauxC7RpsbanitxPFs7FK3mY0MQJk9JNVi1oM-8qfEp8nYT2DwFBhLcIp2z"
          + "Q";

  @Test
  void constructorShouldParseToken() {
    final AccessToken accessToken = new AccessToken(VALID_SAMPLE_TOKEN, OffsetDateTime.MIN);

    final AzureEntraOAuthBearerToken azureOAuthBearerToken =
        new AzureEntraOAuthBearerToken(accessToken);

    assertThat(azureOAuthBearerToken, is(notNullValue()));
    assertThat(azureOAuthBearerToken.value(), is(VALID_SAMPLE_TOKEN));
    assertThat(azureOAuthBearerToken.startTimeMs(), is(1698415912000L));
    assertThat(azureOAuthBearerToken.lifetimeMs(), is(1698415914000L));
    assertThat(azureOAuthBearerToken.scope(), is(Set.of("event_hub", "storage_account")));
    assertThat(azureOAuthBearerToken.principalName(), is("sample@microsoft.com"));
    assertTrue(azureOAuthBearerToken.isExpired());
  }

  @Test
  void constructorShouldRejectInvalidToken() {
    assertThrows(SaslAuthenticationException.class, () -> new AzureEntraOAuthBearerToken(
        new AccessToken("invalid", OffsetDateTime.MIN)));
  }
}
