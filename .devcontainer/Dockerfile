FROM openjdk:8-jdk AS build

# Install Gradle
RUN apt-get update && apt-get install -y gradle && apt-get clean

RUN apt-get install -y neovim zsh bat 
RUN apt-get install -y fzf

RUN sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
RUN alias bat='batcat'


# Set the working directory inside the container

# Copy all project files to the working directory
# COPY . .

# Run the Gradle build commands
# RUN mkdir artifacts && \
#     gradle clean && \
#     gradle build && \
#     mv build/libs/irma_idin_server.war /app/artifacts/irma_idin_server.war

# Stage 2: Final image to hold the artifact
# FROM alpine:latest

# Set working directory
# WORKDIR /output

# Copy the built WAR file from the previous stage
# COPY --from=build /app/artifacts/irma_idin_server.war .

