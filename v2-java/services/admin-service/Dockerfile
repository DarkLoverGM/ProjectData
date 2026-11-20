FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/admin-service-1.0.0.jar app.jar
EXPOSE 3006
ENTRYPOINT ["java","-jar","/app/app.jar"]
