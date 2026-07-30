# 빌드 컨텍스트: backend/ (docker-compose.yml에서 build.context: ./backend)

# ---- 빌드 스테이지 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B package -DskipTests

# ---- 실행 스테이지 ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/backend.jar app.jar

EXPOSE 8080
# 컨테이너 메모리 제한(docker-compose의 mem_limit)보다 항상 작게 힙을 잡을 것
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
