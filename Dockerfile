FROM node:20 AS webappbuild

RUN apt-get update && apt-get install -y wget rsync unzip

WORKDIR /app
COPY . .

# copy the pre-built frontend package from the gitlab server
RUN mkdir -p /tmp/idin_issuer_webclient
RUN wget https://gitlab.science.ru.nl/irma/github-mirrors/irma_idin_webclient/-/jobs/artifacts/master/download?job=build-nl -O /tmp/idin_issuer_webclient/download

RUN cd /tmp/idin_issuer_webclient && unzip /tmp/idin_issuer_webclient/download

RUN ls -la /tmp/idin_issuer_webclient/build

RUN mkdir -p /var/www/
RUN rsync -r -o -g -p /tmp/idin_issuer_webclient/build/ /var/www/ --chmod=D755,F644 --chown=root:www-data

# -------------------------------------------------------------------------------
# Step for building the java library

FROM openjdk:11-jdk AS javabuild

RUN apt-get update && apt-get install -y gradle && apt-get clean

WORKDIR /app

COPY . .

RUN mkdir -p artifacts && \
    gradle clean && \
    gradle build

# -------------------------------------------------------------------------------
# Step for hosting the server

# FROM tomcat:9-jre11
FROM tomcat:9-jre11-temurin-focal

RUN apt-get update && apt-get install gettext-base unzip 

# Copy the webapp to the webapps directory
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=webappbuild /var/www/ /usr/local/tomcat/webapps/ROOT/

# Copy the war file to the webapps directory
COPY --from=javabuild /app/build/libs/irma_idin_server.war /usr/local/tomcat/webapps/
COPY --from=javabuild /app/start.sh .
COPY --from=javabuild /app/idin-fontend-config-template.txt .

COPY --from=javabuild /app/build/libs/irma_idin_server.war /usr/local/tomcat/webapps/
RUN unzip /usr/local/tomcat/webapps/irma_idin_server.war -d /usr/local/tomcat/webapps/irma_idin_server
RUN rm -rf /usr/local/tomcat/webapps/irma_idin_server.war

EXPOSE 8080

CMD ["/bin/bash", "-C", "./start.sh"]
