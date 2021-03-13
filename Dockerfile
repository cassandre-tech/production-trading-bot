# Install maven and copy project for compilation.
FROM maven:3.6.3-openjdk-11 as build
WORKDIR /build
COPY . .
RUN mvn -DskipTests=true spring-boot:build-image

# Creates our image.
FROM adoptopenjdk/openjdk11 as runnable
COPY --from=build /build/target/production-trading-bot.jar app.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]