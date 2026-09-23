# ---------- Compilacion ----------
# Si este tag no existe en tu registro, usa maven:3-eclipse-temurin-25
# o compila con `mvn package` y cambia la etapa de runtime a copiar target/*.jar.
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

# El repositorio de Maven vive en una cache de BuildKit que persiste entre
# builds: la primera vez descarga todo, las siguientes no descarga nada.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

# ---------- Ejecucion ----------
FROM eclipse-temurin:25-jre

# La aplicacion no corre como root.
RUN groupadd --system luma && useradd --system --gid luma --home /app luma

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown -R luma:luma /app

USER luma
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD ["sh", "-c", "wget -q -O - http://localhost:8080/actuator/health | grep -q UP || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
