FROM jenkins/jenkins:lts
USER root

# Install essential tools and setup Docker CLI repo
RUN apt-get update && \
    apt-get install -y \
    git \
    coreutils \
    fontconfig \
    maven \
    curl \
    unzip \
    ca-certificates \
    gnupg && \
    # Create directory for Docker GPG key
    install -m 0755 -d /etc/apt/keyrings && \
    # Download GPG key
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    chmod a+r /etc/apt/keyrings/docker.gpg && \
    # Add the repository to Apt sources
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    # Install Docker CLI
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

# Ensure the jenkins user is in the docker group 
RUN groupadd -g 999 docker || true && \
    usermod -aG docker jenkins

USER jenkins