# Switching to the specific JDK 17 base to match our microservices
FROM jenkins/jenkins:lts-jdk17
USER root

# Install OS Tools, Maven, and dependencies
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

# Install Snyk CLI directly
RUN curl https://static.snyk.io/cli/latest/snyk-linux -o snyk && \
    chmod +x snyk && \
    mv snyk /usr/local/bin/

# Set Environment Variables
# In the JDK17 image, this path is usually already set, 
# but keeping it here ensures your 'mvn' and 'gradlew' find it.
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Create the cache directory for Gradle persistence
RUN mkdir -p /root/.gradle && chmod 777 /root/.gradle

USER jenkins