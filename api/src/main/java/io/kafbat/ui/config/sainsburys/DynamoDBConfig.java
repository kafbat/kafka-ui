package io.kafbat.ui.config.sainsburys;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDynamoDBRepositories(basePackages = "io.kafbat.ui.repository")
public class DynamoDBConfig {

  @Value("${aws.region: eu-west-1}")
  private String region;



  /**
   * Configures the Amazon DynamoDB client to connect to a local instance.
   * This is typically used for development and testing purposes.
   *
   * @return an instance of AmazonDynamoDB configured for local use.
   */
  @Bean
  public AmazonDynamoDB amazonDynamoDB() {
    return AmazonDynamoDBClientBuilder.standard()
        .withRegion(region)
        .build();
  }
}
