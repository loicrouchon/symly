FROM debian:bookworm

# Build dependencies
RUN apt update \
    && apt install -y \
      openjdk-17-jdk-headless \
      maven \
      libpicocli-java \
    && apt clean

# Additional dependencies for offline maven build
RUN apt install -y \
      maven-debian-helper \
      libmaven-assembly-plugin-java \
      libmaven-antrun-plugin-java \
    && apt clean

VOLUME /workspace
WORKDIR /workspace
