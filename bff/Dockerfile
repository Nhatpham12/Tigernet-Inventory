# syntax=docker/dockerfile:1.7
# ---- Build: đóng gói Spring Boot bằng Maven (image có sẵn Maven, không tải qua mvnw) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
RUN mvn -B -q -ntp dependency:go-offline
COPY src/ src/
RUN mvn -B -q -ntp -DskipTests package \
 && find target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -exec cp {} /app.jar \;

# ---- Runtime: chỉ JRE ----
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError"
RUN groupadd --system app && useradd --system --gid app --home /app app \
 && mkdir -p /app && chown app:app /app
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
