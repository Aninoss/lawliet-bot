FROM openjdk:11.0-jre

WORKDIR /home/app

COPY resources /home/app/data/resources

COPY *.jar /home/app/app.jar

RUN useradd -m app

USER app

CMD [ "java", "-jar", "app.jar" ]