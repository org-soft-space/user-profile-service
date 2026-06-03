# =========================
# Build stage
# =========================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /build

# Gradle wrapper и конфиги (кэш зависимостей)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew dependencies --no-daemon || true

# Исходники
COPY src src

# Сборка Spring Boot JAR
RUN ./gradlew bootJar --no-daemon

# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /build/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
