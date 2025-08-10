# Step 1: Use an official Maven image to build the project
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies first (better caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the project files
COPY src ./src

# Package the application (skip tests for faster build)
RUN mvn clean package -DskipTests

# Step 2: Use a lightweight JDK image to run the app
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set environment variable for Render's dynamic port
ENV PORT=8080

# Expose the port
EXPOSE 8080

# Run the jar file
CMD ["java", "-jar", "app.jar"]
