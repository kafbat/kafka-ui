FROM ubuntu:latest

RUN apt update && apt -y install \
    maven \
    openjdk-17-jdk

RUN curl https://get.docker.com/ | bash -
RUN curl https://get.pnpm.io/install.sh | bash -

COPY . /build

COPY .dev/builder/docker-entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

WORKDIR /build

ENTRYPOINT ["entrypoint.sh"]

CMD mvn -DskipTests=true -Pprod clean package
