# Build stage
FROM eclipse-temurin:17-jdk-focal as builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle gradle/
COPY gradlew settings.gradle ./
COPY server/build.gradle.kts server/
COPY common/build.gradle.kts common/

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY server/src server/src/
COPY common/src common/src/

# Build the application
RUN ./gradlew :server:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-focal

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/server/build/libs/naija-ayo-server.jar app.jar

# Expose the port Render will map to
EXPOSE 10000

# Run the application with conservative, hard-coded memory limits to prevent OOM kills.
# -Xms sets the initial heap size, -Xmx sets the maximum heap size.
CMD ["java", "-Xms128m", "-Xmx256m", "-jar", "app.jar"]
