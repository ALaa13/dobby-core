# === Build ===
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Copy build configuration files
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle


# Download dependencies without building (cached unless build.gradle.kts changes)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN ./gradlew build -x test --no-daemon

# === Runtime  ===
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create a non-root user for security
RUN useradd -m dobby
USER dobby

# Copy the JAR from the builder stage
COPY --chown=dobby:dobby --from=builder /app/build/libs/*.jar app.jar

# Copy any AI prompt files if they exist
COPY --chown=dobby:dobby ai_prompt.txt* ./

# Document the port
EXPOSE 8080

# Run with performance optimizations
ENTRYPOINT ["java", "-XX:+UseG1GC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]