# Install maven and copy project for compilation.
FROM maven:3.6.3-openjdk-11-slim as build
WORKDIR /build
# Copy just pom.xml (dependencies and dowload them all for offline access later - cache layer).
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copy source files and compile them (.dockerignore should handle what to copy).
COPY . .
RUN mvn -DskipTests=true package spring-boot:repackage

# Creates our image.
FROM adoptopenjdk/openjdk11 as runnable
COPY --from=build /build/target/production-trading-bot.jar app.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]