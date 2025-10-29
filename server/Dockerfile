FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy from parent directory since Dockerfile is in server/ but needs repo root files
COPY ../gradle gradle
COPY ../gradlew .
COPY ../build.gradle.kts .
COPY ../settings.gradle .

# Copy source code from parent directory
COPY ../server/src src

# Build the application
RUN ./gradlew build --no-daemon

# Create a smaller runtime image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR
COPY --from=0 /app/build/libs/naija-ayo-server-0.0.1-all.jar app.jar

# Expose port
EXPOSE 8080

# Set environment variables
ENV MONGODB_URI=""
ENV MONGODB_DATABASE="naija_ayo"
ENV JWT_SECRET=""

# Run the application
CMD ["java", "-jar", "app.jar"]