FROM node:20 AS webappbuild

WORKDIR /app
COPY . .

RUN cd /app/frontend && npm install && npm run build

RUN mkdir -p /var/www/

RUN cp -r /app/frontend/build/* /var/www/
RUN chmod +755 /var/www/

RUN chown -R root:www-data /var/www/

# -------------------------------------------------------------------------------
# Step for building the java library

FROM gradle:8.10-jdk21 AS javabuild
WORKDIR /app

# Layer build files for dependency caching
COPY build.gradle settings.gradle gradle /app/
RUN gradle --no-daemon dependencies || true

# Copy sources
COPY . /app

# Run unit tests explicitly (build also runs tests, but this keeps intent clear)
RUN gradle --no-daemon clean test

# If tests passed, produce the artifact
RUN mkdir -p artifacts && gradle --no-daemon build
# -------------------------------------------------------------------------------
# Step for hosting the server

FROM tomcat:10.1-jre21-temurin-jammy

RUN apt-get update && apt-get install -y gettext-base unzip curl && apt-get clean

# Clean default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Install Tuckey Rewrite Filter
RUN curl -L -o /usr/local/tomcat/lib/urlrewritefilter-5.1.3.jar \
    https://repo1.maven.org/maven2/org/tuckey/urlrewritefilter/5.1.3/urlrewritefilter-5.1.3.jar

# Copy frontend build into ROOT webapp
COPY --from=webappbuild /var/www/ /usr/local/tomcat/webapps/ROOT/

# Create WEB-INF directory manually and copy rewrite config
RUN mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF
COPY --from=webappbuild /app/frontend/tomcat/* /usr/local/tomcat/webapps/ROOT/WEB-INF/
# COPY src/main/webapp/WEB-INF/urlrewrite.xml /usr/local/tomcat/webapps/ROOT/WEB-INF/

# Copy WAR and extract into separate context
COPY --from=javabuild /app/build/libs/irma_idin_server.war /usr/local/tomcat/webapps/
RUN unzip /usr/local/tomcat/webapps/irma_idin_server.war -d /usr/local/tomcat/webapps/irma_idin_server \
    && rm /usr/local/tomcat/webapps/irma_idin_server.war

# Copy support scripts
COPY --from=javabuild /app/start.sh .
COPY --from=javabuild /app/idin-fontend-config-template.txt .

EXPOSE 8080

CMD ["/bin/bash", "-c", "./start.sh"]
