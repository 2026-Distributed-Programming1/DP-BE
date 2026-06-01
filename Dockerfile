FROM eclipse-temurin:21-jre

WORKDIR /app

ENV SERVER_PORT=8080
ENV APP_UPLOAD_DISPATCH_DIR=/app/uploads/dispatch

RUN mkdir -p /app/uploads/dispatch

COPY app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
