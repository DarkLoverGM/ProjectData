FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/payment-service-1.0.0.jar app.jar
EXPOSE 3005
ENTRYPOINT ["java","-jar","/app/app.jar"]
