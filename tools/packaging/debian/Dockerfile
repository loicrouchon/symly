FROM ubuntu:23.10

RUN apt update

# Packaging essentials
RUN apt install -y \
      gpg \
      curl \
      build-essential \
      devscripts \
      debhelper \
    && apt clean

# Symly build dependencies
RUN apt install -y \
      openjdk-17-jdk-headless \
      ant \
      libpicocli-java \
    && apt clean

VOLUME /workspace
WORKDIR /workspace

ENV DEBEMAIL='loic@loicrouchon.com'
ENV DEBFULLNAME='Loic Rouchon'
ENV PPA_URL='ppa:loicrouchon/symly'
ENV GPG_KEY_FINGERPRINT='C3BB9448B16C971103E876BF3A091A0DF2799262'
