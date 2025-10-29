FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .

# Copy Gradle settings from root
COPY ../settings.gradle settings.gradle

# Copy source code
COPY src src

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