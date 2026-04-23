#stage 1 - Build with Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS maven-build

# Build and install common-api dependency first
WORKDIR /common-lib
COPY common-lib/ ./
RUN mvn clean install -DskipTests

# Now build the main application
WORKDIR /app
COPY user-manager/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY user-manager/src ./src
RUN mvn clean package -DskipTests

#stage 2 - Extract JAR layers
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy JAR from maven build stage
COPY --from=maven-build /app/target/*.jar app.jar

# Unpackage jar file
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf /app/app.jar)

#stage 3 - Final runtime image
FROM eclipse-temurin:17-jre-alpine

# Add Maintainer Info
LABEL maintainer="TQD Team"

# Add volume pointing to /tmp
VOLUME /tmp

# Copy unpackaged application to new container
ARG DEPENDENCY=/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Execute the application
ENTRYPOINT ["java","-cp","app:app/lib/*","vn.tqd.mobilemall.usermanager.UserManagerApplication"]