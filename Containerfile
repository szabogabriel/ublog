FROM docker.io/library/maven:3-eclipse-temurin-21 AS build

WORKDIR /build

# jmustache and jdi are not on Maven Central, so they are built and installed locally first.
RUN git clone --depth 1 https://github.com/szabogabriel/jMustache.git \
 && git clone --depth 1 https://github.com/szabogabriel/JDI.git \
 && mvn -q -B -f jMustache/pom.xml install -DskipTests \
 && mvn -q -B -f JDI/pom.xml install -DskipTests

COPY pom.xml /build/ublog/pom.xml
COPY src /build/ublog/src
RUN mvn -q -B -f ublog/pom.xml package -DskipTests

FROM docker.io/library/eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /build/ublog/target/ublog-*-fat.jar /app/app.jar
COPY template /app/template
COPY testContent/blogFolder /app/blogFolder
COPY testContent/userFolder /app/userFolder
RUN mkdir -p /app/imageFolder && chmod -R 777 /app/blogFolder /app/userFolder /app/imageFolder

ENV MUSTACHE_TEMPLATES_CACHING_ENABLED=true
ENV MUSTACHE_TEMPLATES_FOLDER=/app/template/personal
ENV MUSTACHE_TEMPLATE_UPDATE=false

ENV SERVER_PORT=8080

ENV SESSION_TIMEOUT=60
ENV SESSION_COOKIE_SECURE=false

ENV SOCKET_HANDLER_THREAD_POOL_SIZE=10
ENV SOCKET_HANDLER_QUEUE_CAPACITY=100

ENV TARGET_FOLDER=/app/blogFolder
ENV USER_FOLDER=/app/userFolder
ENV IMAGE_FOLDER=/app/imageFolder

EXPOSE 8080

CMD ["java", "-Xms8m", "-Xmx8m", "-cp", "/app/app.jar", "info.gabrielszabo.ublog.App"]
