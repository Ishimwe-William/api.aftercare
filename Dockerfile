# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and project files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# ✅ Fix: Give execute permission to mvnw
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw package -DskipTests

# Run stage
FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-Xmx350m", "-Xss256k", "-XX:+UseSerialGC", "-jar", "/app.jar"]
