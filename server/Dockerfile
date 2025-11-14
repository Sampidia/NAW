FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy Gradle wrapper and root build files
COPY ../gradle gradle
COPY ../gradlew .
COPY ../settings.gradle .

# Create a minimal build.gradle.kts for server module in Docker
RUN echo "plugins { kotlin(\"jvm\") version \"1.9.21\"; application; id(\"com.github.johnrengelman.shadow\") version \"8.1.1\" }\n\
group = \"com.naijaayo\"\n\
version = \"0.0.1\"\n\
repositories { mavenCentral() }\n\
dependencies {\n\
    implementation(\"io.ktor:ktor-server-core-jvm:2.3.7\")\n\
    implementation(\"io.ktor:ktor-server-netty-jvm:2.3.7\")\n\
    implementation(\"io.ktor:ktor-server-websockets-jvm:2.3.7\")\n\
    implementation(\"io.ktor:ktor-server-auth-jwt-jvm:2.3.7\")\n\
    implementation(\"io.ktor:ktor-server-content-negotiation-jvm:2.3.7\")\n\
    implementation(\"io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.7\")\n\
    implementation(\"io.ktor:ktor-server-cors-jvm:2.3.7\")\n\
    implementation(\"io.ktor:ktor-server-call-logging-jvm:2.3.7\")\n\
    implementation(\"org.mongodb:mongodb-driver-kotlin-coroutine:4.11.0\")\n\
    implementation(\"org.mongodb:mongodb-driver-core:4.11.0\")\n\
    implementation(\"org.mongodb:bson:4.11.0\")\n\
    implementation(\"at.favre.lib:bcrypt:0.10.2\")\n\
    implementation(\"ch.qos.logback:logback-classic:1.4.14\")\n\
    testImplementation(\"io.ktor:ktor-server-tests-jvm:2.3.7\")\n\
    testImplementation(\"org.jetbrains.kotlin:kotlin-test-junit:1.9.21\")\n\
}\n\
tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = \"17\" }\n\
application { mainClass.set(\"com.naijaayo.worldwide.ApplicationKt\") }\n\
tasks.shadowJar {\n\
    archiveBaseName.set(\"naija-ayo-server\")\n\
    archiveClassifier.set(\"\")\n\
    archiveVersion.set(\"\")\n\
    manifest { attributes[\"Main-Class\"] = \"com.naijaayo.worldwide.ApplicationKt\" }\n\
}" > build.gradle.kts

# Copy source code
COPY ../server/src src

# Build the application
RUN ./gradlew build --no-daemon

# Create a smaller runtime image
FROM openjdk:21-jdk-slim

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