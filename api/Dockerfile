# The tag is ignored when a sha is included but the reason to add it are:
# 1. Self Documentation: It is difficult to find out what the expected tag is given a sha alone
# 2. Helps dependabot during discovery of upgrades
# We include the SHA to ensure image immutability
FROM azul-zulu:25.38-25.0.5.1-jre-headless-alpine3.23@sha256:46ceee941532ce0c91e14650915fd240061f8cbd5e8031a516f25508f4e0c2f9

RUN apk upgrade --no-cache
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
