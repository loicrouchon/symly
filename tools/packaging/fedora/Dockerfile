FROM fedora:latest

# Runtime dependencies
RUN dnf install -y \
      java-latest-openjdk-headless \
      picocli \
    && dnf autoremove -y

# Additional dependencies for build
RUN dnf install -y \
      java-latest-openjdk-devel \
      ant \
      rpmdevtools \
    && dnf autoremove -y

# Additional dependencies for build
RUN dnf install -y \
      git \
    && dnf autoremove -y

VOLUME /workspace
WORKDIR /workspace
