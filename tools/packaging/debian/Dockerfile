FROM debian:bookworm

# Build dependencies
RUN apt update \
    && apt install -y \
      openjdk-25-jdk-headless \
      ant \
      libpicocli-java \
    && apt clean

VOLUME /workspace
WORKDIR /workspace
