FROM openjdk:21-jdk-slim

RUN mkdir /app
RUN mkdir /app/template
RUN mkdir /app/blogFolder
RUN mkdir /app/userFolder

WORKDIR /app

COPY target/*.jar /app/app.jar
COPY target/deps /app/deps/
COPY template/* /app/template/

ENV TARGET_FOLDER=/app/blogFolder
ENV USER_FOLDER=/app/userFolder

RUN chmod -R 777 /app/userFolder
RUN chmod -R 777 /app/blogFolder

CMD ["java", "-cp", "/app/app.jar:/app/deps/*", "info.gabrielszabo.ublog.App"]