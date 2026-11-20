FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/platform-service-1.0.0.jar app.jar
EXPOSE 3001
ENTRYPOINT ["java","-jar","/app/app.jar"]
