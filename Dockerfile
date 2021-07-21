FROM openjdk:8-jdk-alpine
ARG JAR_FILE=core-connector/target/*.jar
COPY ${JAR_FILE} app.jar
ENV MLCONN_OUTBOUND_ENDPOINT=http://simulator:3004
ENV CBS_NAME="CBS CO. LTD."
ENV CBS_HOST="https://localhost/api"
ENV CBS_USERNAME=username
ENV CBS_PASSWORD=password
ENV CBS_AUTH_CLIENT_ID=clientId
ENV CBS_AUTH_CLIENT_SECRET=clientSecret
ENV CBS_AUTH_GRANT_TYPE=grantType
ENV CBS_AUTH_SCOPE=scope
ENV CBS_AUTH_ENCRYPTED_PASS=false
ENV CBS_AUTH_TENANT_ID=tenantId
ENTRYPOINT ["java", "-Dml-conn.outbound.host=${MLCONN_OUTBOUND_ENDPOINT}", "-Dcbs.name=${CBS_NAME}", "-Dcbs.host=${CBS_HOST}", "-Dcbs.username=${CBS_USERNAME}", "-Dcbs.password=${CBS_PASSWORD}", "-Dcbs.scope=${CBS_AUTH_SCOPE}", "-Dcbs.client-id=${CBS_AUTH_CLIENT_ID}", "-Dcbs.client-secret=${CBS_AUTH_CLIENT_SECRET}", "-Dcbs.grant-type=${CBS_AUTH_GRANT_TYPE}", "-Dcbs.is-password-encrypted=${CBS_AUTH_ENCRYPTED_PASS}", "-Dcbs.tenant-id=${CBS_AUTH_TENANT_ID}", "-jar", "/app.jar"]
EXPOSE 3003