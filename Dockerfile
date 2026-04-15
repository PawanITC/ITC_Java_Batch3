FROM jenkins/jenkins:lts
USER root

# Install dependencies including Docker CLI
RUN apt-get update && \
    apt-get install -y \
    git \
    coreutils \
    fontconfig \
    maven \
    curl \
    unzip \
    apt-transport-https \
    ca-certificates \
    gnupg2 \
    software-properties-common && \
    # Add Docker’s official GPG key
    curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - && \
    # Set up the Docker stable repository
    add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable" && \
    apt-get update && \
    apt-get install -y docker-ce-cli && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Snyk
RUN curl https://static.snyk.io/cli/latest/snyk-linux -o snyk && \
    chmod +x snyk && \
    mv snyk /usr/local/bin/

# Set up Java Environment
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Fix permissions for Gradle cache
RUN mkdir -p /root/.gradle && chmod 777 /root/.gradle

# CRITICAL: Ensure the jenkins user is in the docker group 
# (Group ID 999 is standard for Docker Desktop, but we'll add it by name)
RUN groupadd -g 999 docker || true && \
    usermod -aG docker jenkins

USER jenkins