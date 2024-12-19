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

FROM openjdk:8-jdk AS javabuild

RUN apt-get update && apt-get install -y gradle && apt-get clean

WORKDIR /app

COPY . .

RUN mkdir -p artifacts && \
    gradle clean && \
    gradle build

# -------------------------------------------------------------------------------
# Step for hosting the server

FROM tomee:jre8

RUN apt-get update && apt-get install gettext-base

# Copy the webapp to the webapps directory
RUN rm -rf /usr/local/tomee/webapps/*
COPY --from=webappbuild /var/www/ /usr/local/tomee/webapps/ROOT/

# Copy the war file to the webapps directory
COPY --from=javabuild /app/build/libs/irma_idin_server.war /usr/local/tomee/webapps/
COPY --from=javabuild /app/start.sh .
COPY --from=javabuild /app/idin-fontend-config-template.txt .

EXPOSE 8080

CMD ["/bin/bash", "-C", "./start.sh"]
