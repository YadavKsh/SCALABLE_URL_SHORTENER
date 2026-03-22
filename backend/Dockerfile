FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package

EXPOSE 8081

CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]