package io.kafbat.ui.config.auth.azure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class AzureEntraLoginCallbackHandlerTest {

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

  @Mock
  private OAuthBearerTokenCallback oauthBearerTokenCallBack;

  @Mock
  private OAuthBearerToken oauthBearerToken;

  @Mock
  private TokenCredential tokenCredential;

  @Mock
  private AccessToken accessToken;

  private AzureEntraLoginCallbackHandler azureEntraLoginCallbackHandler;

  @BeforeEach
  public void beforeEach() {
    azureEntraLoginCallbackHandler = new AzureEntraLoginCallbackHandler();
    azureEntraLoginCallbackHandler.setTokenCredential(tokenCredential);
  }

  @Test
  public void shouldProvideTokenToCallbackWithSuccessfulTokenRequest()
      throws UnsupportedCallbackException {
    final Map<String, Object> configs = new HashMap<>();
    configs.put(
        "bootstrap.servers",
        List.of("test-eh.servicebus.windows.net:9093"));

    when(tokenCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));
    when(accessToken.getToken()).thenReturn(VALID_SAMPLE_TOKEN);

    azureEntraLoginCallbackHandler.configure(configs, null, null);
    azureEntraLoginCallbackHandler.handle(new Callback[] {oauthBearerTokenCallBack});

    final ArgumentCaptor<TokenRequestContext> contextCaptor =
        ArgumentCaptor.forClass(TokenRequestContext.class);
    final ArgumentCaptor<OAuthBearerToken> tokenCaptor =
        ArgumentCaptor.forClass(OAuthBearerToken.class);

    verify(tokenCredential, times(1)).getToken(contextCaptor.capture());
    verify(oauthBearerTokenCallBack, times(0)).error(anyString(), anyString(), anyString());
    verify(oauthBearerTokenCallBack, times(1)).token(tokenCaptor.capture());

    final TokenRequestContext tokenRequestContext = contextCaptor.getValue();
    assertThat(tokenRequestContext, is(notNullValue()));
    assertThat(
        tokenRequestContext.getScopes(),
        is(List.of("https://test-eh.servicebus.windows.net/.default")));
    assertThat(tokenRequestContext.getClaims(), is(nullValue()));
    assertThat(tokenRequestContext.getTenantId(), is(nullValue()));
    assertFalse(tokenRequestContext.isCaeEnabled());

    assertThat(tokenCaptor.getValue(), is(notNullValue()));
    assertEquals(VALID_SAMPLE_TOKEN, tokenCaptor.getValue().value());
  }

  @Test
  public void shouldProvideErrorToCallbackWithTokenError() throws UnsupportedCallbackException {
    final Map<String, Object> configs = new HashMap<>();
    configs.put(
        "bootstrap.servers",
        List.of("test-eh.servicebus.windows.net:9093"));

    when(tokenCredential.getToken(any(TokenRequestContext.class)))
        .thenThrow(new RuntimeException("failed to acquire token"));

    azureEntraLoginCallbackHandler.configure(configs, null, null);
    azureEntraLoginCallbackHandler.handle(new Callback[] {oauthBearerTokenCallBack});

    verify(oauthBearerTokenCallBack, times(1))
        .error(
            "invalid_grant",
            "Failed to acquire Azure token for Event Hub Authentication. "
                + "Please ensure valid Azure credentials are configured.",
            null);
    verify(oauthBearerTokenCallBack, times(0)).token(any());
  }

  @Test
  public void shouldThrowExceptionWithNullBootstrapServers() {
    final Map<String, Object> configs = new HashMap<>();

    assertThrows(IllegalArgumentException.class, () -> azureEntraLoginCallbackHandler.configure(
        configs, null, null));
  }

  @Test
  public void shouldThrowExceptionWithMultipleBootstrapServers() {
    final Map<String, Object> configs = new HashMap<>();
    configs.put("bootstrap.servers", List.of("server1", "server2"));

    assertThrows(IllegalArgumentException.class, () -> azureEntraLoginCallbackHandler.configure(
        configs, null, null));
  }

  @Test
  public void shouldThrowExceptionWithUnsupportedCallback() {
    assertThrows(UnsupportedCallbackException.class, () -> azureEntraLoginCallbackHandler.handle(
        new Callback[] {mock(Callback.class)}));
  }

  @Test
  public void shouldDoNothingOnClose() {
    azureEntraLoginCallbackHandler.close();
  }

  @Test
  public void shouldSupportDefaultConstructor() {
    new AzureEntraLoginCallbackHandler();
  }
}
