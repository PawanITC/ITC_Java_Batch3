# SonarQube Setup Guide for Product Service

## Prerequisites
- Docker and Docker Compose installed
- Java 17 or higher
- Gradle 8.10

## Steps to Set Up SonarQube

### 1. Start All Services
```bash
docker-compose up -d
```

### 2. Wait for SonarQube to Initialize
SonarQube takes about 2-3 minutes to fully start. You can check the status:
```bash
docker logs product-sonarqube
```
Look for "Web Server is operational" in the logs.

### 3. Access SonarQube UI
Open http://localhost:9000 in your browser.

**Default Credentials:**
- Username: admin
- Password: admin

### 4. Create a Token for Analysis
1. Go to http://localhost:9000
2. Login with admin/admin
3. Go to Account → Security → Generate Token
4. Create a token named "gradle-token"
5. Copy the token value

### 5. Run Code Quality Analysis
```bash
# Run tests and generate coverage first
./gradlew clean test jacocoTestReport

# Run SonarQube analysis with your token
./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=YOUR_TOKEN_HERE
```

## Alternative: Anonymous Analysis (For Local Development)

If you want to allow anonymous analysis without tokens, update the docker-compose.yml:

```yaml
sonarqube:
  environment:
    - SONAR_FORCEAUTHENTICATION=false
    - SONAR_ANALYSIS_MODE=publish  # Allow anonymous publishing
```

Then run:
```bash
./gradlew sonar -Dsonar.host.url=http://localhost:9000
```

## Viewing Results
After successful analysis, visit http://localhost:9000 to see:
- Code quality metrics
- Coverage reports
- Security vulnerabilities
- Code smells
- Technical debt

## Troubleshooting

### "Not authorized" Error
- Make sure SonarQube is fully started
- Use correct token or enable anonymous analysis
- Check that the project key matches in build.gradle

### "Server not reachable" Error
- Wait longer for SonarQube to start (2-3 minutes)
- Check if port 9000 is available
- Verify docker container is running: `docker ps`

### Gradle Plugin Issues
- Ensure you're using Gradle 8.10 (not 9.x)
- Check plugin version compatibility
- Clean and rebuild: `./gradlew clean build`

## Current Configuration

The project is configured with:
- SonarQube plugin version: 5.0.0.4638
- Gradle version: 8.10
- Project key: funkart-product-service
- Coverage: JaCoCo integration enabled
