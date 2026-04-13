FROM jenkins/jenkins:lts
USER root

RUN apt-get update && \
    apt-get install -y \
    git \
    coreutils \
    fontconfig \
    maven \
    curl \
    unzip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN curl https://static.snyk.io/cli/latest/snyk-linux -o snyk && \
    chmod +x snyk && \
    mv snyk /usr/local/bin/

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Fix permissions for the gradle cache we defined in docker-compose
RUN mkdir -p /root/.gradle && chmod 777 /root/.gradle

USER jenkins