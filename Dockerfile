FROM node:20 AS webappbuild

RUN apt-get update && apt-get install -y wget rsync unzip

WORKDIR /app
COPY . .

RUN mkdir -p /tmp/idin_issuer_webclient
RUN wget https://gitlab.science.ru.nl/irma/github-mirrors/irma_idin_webclient/-/jobs/artifacts/master/download?job=build-nl -O /tmp/idin_issuer_webclient/download

RUN cd /tmp/idin_issuer_webclient && unzip /tmp/idin_issuer_webclient/download

RUN ls -la /tmp/idin_issuer_webclient/build

RUN mkdir -p /var/www/pbdf-website/uitgifte/idin/
RUN rsync -r -o -g -p /tmp/idin_issuer_webclient/build/ /var/www/pbdf-website/uitgifte/idin/ --chmod=D755,F644 --chown=root:www-data

COPY ./.secrets/idin-config-webclient.json /var/www/pbdf-website/uitgifte/idin/conf.json

# -------------------------------------------------------------------------------

# FROM openjdk:8-jdk AS javabuild
FROM gradle:7.6-jdk11 AS javabuild

RUN apt-get update && apt-get install -y gradle && apt-get clean

# Set the working directory inside the container
WORKDIR /app

COPY . .

# Run the Gradle build commands
RUN mkdir -p artifacts && \
    gradle clean && \
    gradle build
# mv build/libs/irma_idin_server.war /app/artifacts/irma_idin_server.war


# -------------------------------------------------------------------------------

FROM tomee:9.1-jre11

# Copy the webapp to the webapps directory
RUN rm -rf /usr/local/tomee/webapps/*
COPY --from=webappbuild /var/www/ /usr/local/tomee/webapps/ROOT/

# Copy the war file to the webapps directory
COPY --from=javabuild /app/build/libs/irma_idin_server.war /usr/local/tomee/webapps/

ENV CONFIG_DIR="/config"

RUN mkdir $CONFIG_DIR
RUN openssl genrsa -out $CONFIG_DIR/sk.pem 2048
RUN openssl pkcs8 -topk8 -inform PEM -outform DER -in $CONFIG_DIR/sk.pem -out $CONFIG_DIR/sk.der -nocrypt
RUN openssl rsa -in $CONFIG_DIR/sk.pem -pubout -outform PEM -out $CONFIG_DIR/pk.pem
RUN openssl pkey -pubin -inform PEM -outform DER -in $CONFIG_DIR/pk.pem -out $CONFIG_DIR/pk.der

COPY ./.secrets/config.xml $CONFIG_DIR/
COPY ./.secrets/config.json $CONFIG_DIR/

ENV IRMA_CONF=$CONFIG_DIR
EXPOSE 8080

# Default command: show the contents of the output directory
# CMD ["gradle", "appRun"]
