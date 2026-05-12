FROM confluentinc/cp-schema-registry:7.8.0

USER root
COPY produce-messages.sh /produce-messages.sh
RUN chmod +x /produce-messages.sh
USER appuser

CMD ["/produce-messages.sh"]
