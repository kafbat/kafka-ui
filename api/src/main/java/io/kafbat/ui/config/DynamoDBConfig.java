package io.kafbat.ui.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDynamoDBRepositories(basePackages = "io.kafbat.ui.repository")
public class DynamoDBConfig {

  @Value("${aws.region}")
  private String region;
  @Value("${aws.credentials.access-key}")
  private String accessKey;
  @Value("${aws.credentials.secret-key}")
  private String secretKey;
  @Value("${aws.dynamodb.endpoint}")
  private String endpoint;



  /**
   * Configures the Amazon DynamoDB client to connect to a local instance.
   * This is typically used for development and testing purposes.
   *
   * @return an instance of AmazonDynamoDB configured for local use.
   */
  @Bean
  public AmazonDynamoDB amazonDynamoDB() {
    return AmazonDynamoDBClientBuilder.standard()
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
            endpoint, region))
        .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(accessKey, secretKey)))
        .build();
  }
}
