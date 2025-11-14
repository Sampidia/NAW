# Build stage
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle gradle/
COPY gradlew settings.gradle ./
COPY server/build.gradle.kts server/
COPY common/build.gradle.kts common/

# Copy source code
COPY server/src server/src/
COPY common/src common/src/

# Build the application
RUN ./gradlew :server:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR (shadowJar produces naija-ayo-server.jar)
COPY --from=0 /app/server/build/libs/naija-ayo-server.jar app.jar

# Expose port
EXPOSE 8080

# Set environment variables
ENV MONGODB_URI=""
ENV MONGODB_DATABASE="naija_ayo"
ENV JWT_SECRET=""

# Run the application
CMD ["java", "-jar", "app.jar"]
