FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/video-service-1.0.0.jar app.jar
EXPOSE 3004
ENTRYPOINT ["java","-jar","/app/app.jar"]
