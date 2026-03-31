package io.kafbat.ui;

import io.kafbat.ui.service.ssl.SkipSecurityProvider;
import io.kafbat.ui.util.DynamicConfigOperations;
import java.security.Security;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(exclude = LdapAutoConfiguration.class)
public class KafkaUiApplication {

  public static void main(String[] args) {
    startApplication(args);
  }

  public static void startApplication(String[] args) {
    Security.addProvider(new SkipSecurityProvider());

    new SpringApplicationBuilder(KafkaUiApplication.class)
        .initializers(DynamicConfigOperations.dynamicConfigPropertiesInitializer())
        .build()
        .run(args);
  }
}
