#!/bin/sh

CERT_DIR="/etc/kafkaui/certs"
KEYSTORE="$JAVA_HOME/lib/security/cacerts"
STOREPASS="changeit"

if [ -d "$CERT_DIR" ]; then
    for cert in $CERT_DIR/*.crt; do
        if [ -f "$cert" ]; then
            alias=$(basename "$cert" .crt)
            echo "Importing $cert with alias $alias"
            keytool -import -noprompt -trustcacerts -alias "$alias" -file "$cert" -keystore "$KEYSTORE" -storepass "$STOREPASS"
        fi
    done
else
    echo "No certificates directory found at $CERT_DIR"
fi

