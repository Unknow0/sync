FROM openjdk:8-jre-alpine

ENTRYPOINT ["java", "-jar", "/opt/sync-server.jar"]

ADD sync-server.jar /opt/