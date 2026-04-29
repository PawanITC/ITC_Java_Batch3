FROM jenkins/jenkins:lts
USER root

# 1. Install essential tools
RUN apt-get update && \
    apt-get install -y git coreutils fontconfig maven curl unzip ca-certificates gnupg && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    chmod a+r /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && apt-get install -y docker-ce-cli && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# 2. Install Snyk
RUN curl https://static.snyk.io/cli/latest/snyk-linux -o snyk && \
    chmod +x snyk && mv snyk /usr/local/bin/

# 3. FIX: Don't hardcode JAVA_HOME here. Let Jenkins Tools handle it.
# This ensures Jenkins uses its built-in Java to run the UI, 
# while your build uses the Tool JDK you defined.

# 4. Fix permissions and group (Crucial for Docker-out-of-Docker)
RUN groupadd -g 999 docker || true && \
    usermod -aG docker jenkins

USER jenkins