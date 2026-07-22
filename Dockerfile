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

# Two-layer dependency caching for fast rebuilds:
#   1. Layer cache — `COPY pom.xml` then resolve, so an unchanged pom skips this
#      whole step on the next build.
#   2. BuildKit cache mount on ~/.m2 — the local repo survives across builds even
#      when the pom *does* change, so Maven only fetches the delta instead of
#      re-downloading the world. Together these turn a cold ~minutes build into a
#      warm seconds one.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp package -DskipTests \
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
