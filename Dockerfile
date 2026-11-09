# Stage 1: Build the application using Maven
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy wrapper and pom files first
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Give execution permission to the Maven wrapper
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

# Copy the actual source code and package
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/barangay-ease-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]