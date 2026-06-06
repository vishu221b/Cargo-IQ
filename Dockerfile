# syntax=docker/dockerfile:1.7

# -----------------------------------------------------------------------------
# Stage 1: build
#
# Using the official maven:eclipse-temurin image avoids needing to commit
# the Maven Wrapper. If you'd rather use ./mvnw, run `mvn -N wrapper:wrapper`
# in the project root once and commit the resulting .mvn/ + mvnw / mvnw.cmd,
# then swap this stage for `FROM eclipse-temurin:21-jdk-jammy AS build` and
# call ./mvnw instead of mvn.
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies separately from source so most rebuilds skip downloads.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests \
 && cp target/cargo-iq-*.jar /workspace/app.jar

# -----------------------------------------------------------------------------
# Stage 2: runtime (JRE only, non-root)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
RUN useradd --system --uid 1001 --create-home cargoiq
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER cargoiq

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
