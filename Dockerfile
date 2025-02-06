FROM gcr.io/distroless/java17
COPY build/libs/*.jar ./
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"
ENTRYPOINT ["java", "-jar", "/app.jar"]
EXPOSE 8080