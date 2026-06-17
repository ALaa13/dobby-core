# === Stage 1: Build ===
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew build -x test --no-daemon

# === Stage 2: Runtime ===
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -m dobby
USER dobby
COPY --chown=dobby:dobby --from=builder /app/build/libs/*.jar app.jar
COPY --chown=dobby:dobby ai_prompt.txt* ./
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseG1GC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]