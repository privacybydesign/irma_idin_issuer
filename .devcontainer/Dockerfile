FROM openjdk:8-jdk AS build

RUN apt-get update && apt-get install -y gradle && apt-get clean

RUN apt-get install -y neovim zsh bat 
RUN apt-get install -y fzf

RUN sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
RUN alias bat='batcat'
