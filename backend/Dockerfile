# ---- Build stage ----------------------------------------------------------
# WHY a multi-stage build: the final image only ships the runtime JRE + jar,
# not the full JDK + Maven + .m2 cache. That's a ~250MB image instead of ~1GB.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy pom first to maximize Docker layer cache hits when only sources change.
COPY pom.xml ./
RUN mkdir -p /root/.m2 && \
    apt-get update && apt-get install -y --no-install-recommends maven && \
    mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime stage --------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
