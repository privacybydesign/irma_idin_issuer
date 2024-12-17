## Stage 1: Build the WAR file
FROM openjdk:8-jdk AS build

# Install Gradle
RUN apt-get update && apt-get install -y gradle && apt-get clean

# Set the working directory inside the container
WORKDIR /app

# Copy all project files to the working directory
COPY . .

# Run the Gradle build commands
RUN mkdir artifacts && \
    gradle clean && \
    gradle build && \
    mv build/libs/irma_idin_server.war /app/artifacts/irma_idin_server.war

# Stage 2: Final image to hold the artifact
# FROM alpine:latest

# Set working directory
# WORKDIR /output

# Copy the built WAR file from the previous stage
# COPY --from=build /app/artifacts/irma_idin_server.war .

RUN mkdir /config

COPY ./.secrets/config.xml /config
COPY ./.secrets/config.json /config

ENV IRMA_CONF="/config"

# Default command: show the contents of the output directory
CMD ["gradle", "appRun"]
