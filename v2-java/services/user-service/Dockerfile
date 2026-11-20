FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/user-service-1.0.0.jar app.jar
EXPOSE 3003
ENTRYPOINT ["java","-jar","/app/app.jar"]
