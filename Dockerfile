# Java version: must match <java.version> in reposcout-server/pom.xml
ARG JAVA_VERSION=21

# ---------- Stage 1: build the React frontend ----------
FROM node:22-alpine AS frontend
WORKDIR /frontend

# install dependencies first (cached unless package files change)
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# then build the site into /frontend/dist
COPY frontend/ ./
RUN npm run build


# ---------- Stage 2: build the Spring Boot jar ----------
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS backend
WORKDIR /app

COPY reposcout-server/ ./

# put the built frontend where Spring serves static files
COPY --from=frontend /frontend/dist/ ./src/main/resources/static/

RUN chmod +x mvnw && ./mvnw -q package -DskipTests


# ---------- Stage 3: the image that actually runs ----------
FROM eclipse-temurin:${JAVA_VERSION}-jre
WORKDIR /app

# run as a normal user instead of root
RUN useradd --system --create-home appuser
USER appuser

COPY --from=backend /app/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]