# The tag is ignored when a sha is included but the reason to add it are:
# 1. Self Documentation: It is difficult to find out what the expected tag is given a sha alone
# 2. Helps dependabot during discovery of upgrades
FROM azul/zulu-openjdk-alpine:25-jre-headless@sha256:cb58482b4109684033bedae4dab7a23757c196cb69aca5e2d2f7c74651cf6618

RUN apk add --no-cache \
    # snappy codec
    gcompat \
    # configuring timezones
    tzdata
RUN addgroup -S kafkaui && adduser -S kafkaui -G kafkaui

# creating folder for dynamic config usage (certificates uploads, etc)
RUN mkdir /etc/kafkaui/
RUN chown kafkaui /etc/kafkaui

USER kafkaui

ARG JAR_FILE
COPY $JAR_FILE "/api.jar"

ENV JAVA_OPTS=

EXPOSE 8080

# see JmxSslSocketFactory docs to understand why add-opens is needed
CMD java --add-opens java.rmi/javax.rmi.ssl=ALL-UNNAMED  $JAVA_OPTS -jar api.jar
