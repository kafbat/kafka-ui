# The tag is ignored when a sha is included but the reason to add it are:
# 1. Self Documentation: It is difficult to find out what the expected tag is given a sha alone
# 2. Helps dependabot during discovery of upgrades
FROM azul/zulu-openjdk-alpine:24.0.1-jre-headless@sha256:6b6ee650c6570c1977e6291d8510199ea5d4d35baef04b30ab142a3a3c409c7e

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
