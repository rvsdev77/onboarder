FROM eclipse-temurin:22-jdk AS build
WORKDIR /app
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=build /app/build/libs/onboarder-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
