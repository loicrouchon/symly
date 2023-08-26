FROM debian:bookworm

RUN apt update \
    && apt install -y openjdk-17-jdk-headless maven libpicocli-java \
    && apt clean
RUN apt install -y maven-debian-helper \
    && apt clean

VOLUME /workspace
WORKDIR /workspace
