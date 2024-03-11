ARG image

FROM ${image}

LABEL maintainer="Kafbat Team"

COPY data.sql /docker-entrypoint-initdb.d

EXPOSE 5432
