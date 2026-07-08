FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace/backend

COPY backend/pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S groupshop \
  && adduser -S groupshop -G groupshop \
  && mkdir -p /app/uploads \
  && chown -R groupshop:groupshop /app

COPY --from=build /workspace/backend/target/*.jar /app/app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

USER groupshop

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
