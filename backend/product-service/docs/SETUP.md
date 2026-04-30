# Environment Setup & Configuration

Guide for setting up your development environment for Product Service testing.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Project Setup](#project-setup)
3. [IDE Configuration](#ide-configuration)
4. [Running Tests](#running-tests)
5. [JMeter Setup](#jmeter-setup)
6. [Verification](#verification)

---

## Prerequisites

### Required
- **Java 11+** - [Download JDK](https://www.oracle.com/java/technologies/downloads/)
- **Git** - [Download Git](https://git-scm.com/)
- **Gradle 9.3+** - Included in project wrapper (`gradlew`)

### Optional but Recommended
- **IDE** - IntelliJ IDEA, VS Code, or Eclipse
- **JMeter** - For load testing [Download](https://jmeter.apache.org/download_jmeter.cgi)
- **Docker** - For running databases (optional)

---

## Project Setup

### Step 1: Clone Repository
```bash
git clone <repository-url>
cd product-service
```

### Step 2: Verify Java Installation
```bash
java -version
# Expected: Java 11 or higher
```

### Step 3: Build Project
```bash
# Windows
./gradlew.bat clean build

# Linux/macOS
./gradlew clean build
```

### Step 4: Verify Installation
```bash
# Run tests to verify setup
./gradlew.bat test

# Expected: BUILD SUCCESSFUL
```

---

## IDE Configuration

### IntelliJ IDEA

#### 1. Open Project
- File → Open
- Select project root folder
- Click "Open as Project"

#### 2. Configure Java
- File → Project Structure
- Project
  - SDK: Select Java 11+
  - Language Level: 11

#### 3. Configure Gradle
- File → Settings → Build, Execution, Deployment → Gradle
  - Gradle: Use Gradle wrapper
  - Run Tests Using: Gradle

#### 4. Mark Test Folders
- Right-click `src/test/java` → Mark Directory as → Test Sources Root
- Right-click `src/test/resources` → Mark Directory as → Test Resources Root

### VS Code

#### 1. Install Extensions
- Extension Pack for Java (Microsoft)
- Gradle for Java (Microsoft)
- Spring Boot Extension Pack (VMware)

#### 2. Open Workspace
```bash
code .
```

#### 3. Configure Settings
Create `.vscode/settings.json`:
```json
{
  "java.home": "/path/to/jdk11",
  "gradle.nestedProjects": true,
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.formatOnSave": true
  }
}
```

### Eclipse

#### 1. Import Project
- File → Import → Gradle → Existing Gradle Project
- Select project folder
- Click Finish

#### 2. Configure Build Path
- Right-click project → Build Path → Configure Build Path
- Source: Ensure `src/test/java` is included
- Libraries: Ensure JRE System Library is set to Java 11+

---

## Project Structure

```
product-service/
├── src/
│   ├── main/
│   │   ├── java/com/itc/funkart/product_service/
│   │   │   ├── entity/              [Entity classes]
│   │   │   ├── repository/          [Repository interfaces]
│   │   │   ├── service/             [Service interfaces]
│   │   │   ├── serviceImpl/          [Service implementations]
│   │   │   ├── controller/          [REST controllers]
│   │   │   ├── dto/                 [Data transfer objects]
│   │   │   ├── exceptions/          [Custom exceptions]
│   │   │   └── ProductServiceApplication.java
│   │   └── resources/
│   │       ├── application.yaml     [Main config]
│   │       ├── application-dev.yml  [Dev profile]
│   │       ├── application-test.yml [Test profile]
│   │       └── application-prod.yml [Prod profile]
│   │
│   └── test/
│       ├── java/com/itc/funkart/product_service/
│       │   ├── repository/          [Repository tests]
│       │   ├── serviceImpl/          [Service tests]
│       │   ├── controller/          [Controller tests]
│       │   ├── integration/         [Integration tests]
│       │   └── performance/         [Performance tests]
│       └── resources/
│           └── application-test.yml
│
├── jmeter_tests/                    [Load testing]
│   ├── *.jmx                        [Test plans]
│   ├── run_load_tests.bat           [Windows script]
│   └── run_load_tests.sh            [Linux/macOS script]
│
├── docs/                            [Documentation]
│   ├── README.md
│   ├── TESTING_GUIDE.md
│   ├── LOAD_TESTING.md
│   ├── QUICK_REFERENCE.md
│   ├── SETUP.md
│   └── ARCHITECTURE.md
│
├── build.gradle                     [Gradle config]
├── settings.gradle                  [Gradle settings]
├── gradlew                          [Gradle wrapper (Linux/macOS)]
├── gradlew.bat                      [Gradle wrapper (Windows)]
└── README.md                        [Main readme]
```

---

## Running Tests

### First Time Setup
```bash
# Clean and build
./gradlew.bat clean build

# Run all tests
./gradlew.bat test
```

### Run Tests During Development
```bash
# Quick test (just compiles, doesn't run)
./gradlew.bat compileTestJava

# Run all tests
./gradlew.bat test

# Run specific test class
./gradlew.bat test --tests "CartControllerTest"

# Run with verbose output
./gradlew.bat test --info
```

### View Test Results
```
HTML Report: build/reports/tests/test/index.html
JUnit XML: build/test-results/test/
```

---

## JMeter Setup

### Installation

**Option 1: Download & Extract**
1. Download from https://jmeter.apache.org/download_jmeter.cgi
2. Extract to a folder
3. Add `bin` folder to PATH

**Option 2: Package Manager**
```bash
# macOS
brew install jmeter

# Ubuntu/Debian
sudo apt-get install jmeter

# Windows (Chocolatey)
choco install jmeter
```

### Verify Installation
```bash
jmeter --version
# Expected: Apache JMeter 5.5 (or newer)
```

### First Load Test
```bash
# Navigate to jmeter_tests
cd jmeter_tests

# Run automated tests (Windows)
run_load_tests.bat

# Run automated tests (Linux/macOS)
./run_load_tests.sh

# View results
load_test_results/CategoryLoadTest_*/index.html
```

---

## Application Configuration

### Test Profile (application-test.yml)
```yaml
spring:
  application:
    name: product-service
  
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  
  h2:
    console:
      enabled: false

server:
  port: 8080
```

### Properties for Testing

**Database**
- H2 in-memory database (no persistence)
- Auto schema creation (`create-drop`)
- Cleaned between test runs

**Logging**
- DEBUG level for product service
- INFO level for Spring
- WARN level for libraries

---

## Verification Checklist

### Environment Setup
- [ ] Java 11+ installed
- [ ] Gradle works (`./gradlew.bat --version`)
- [ ] Git configured

### Project Setup
- [ ] Project cloned/opened
- [ ] IDE configured
- [ ] Gradle dependencies downloaded
- [ ] Project builds successfully

### Testing Setup
- [ ] Unit tests run and pass
- [ ] Integration tests run and pass
- [ ] Performance tests run and pass
- [ ] Test reports generated

### Load Testing Setup
- [ ] JMeter installed
- [ ] JMeter version verified
- [ ] Load tests run successfully
- [ ] Load test reports generated

### All Tests Pass?
```bash
./gradlew.bat clean test
# Expected: BUILD SUCCESSFUL with all tests passing
```

---

## Troubleshooting Setup

### Issue: "gradle command not found"
```
Solution:
1. Ensure you're in project root directory
2. Use: ./gradlew (Linux/macOS) or gradlew.bat (Windows)
3. Add to PATH if needed
```

### Issue: "JAVA_HOME not set"
```
Solution:
# Linux/macOS
export JAVA_HOME=/path/to/jdk11

# Windows
set JAVA_HOME=C:\path\to\jdk11
```

### Issue: Gradle build fails
```
Solution:
1. Delete .gradle folder: rm -rf .gradle
2. Clean and rebuild: ./gradlew.bat clean build
3. Update gradle: ./gradlew.bat wrapper
```

### Issue: Tests fail with "Connection refused"
```
Solution:
1. Ensure H2 is in test dependencies
2. Check application-test.yml configuration
3. Verify test profile is active
```

### Issue: IDE doesn't recognize test classes
```
Solution:
1. Right-click src/test/java → Mark Directory as → Test Sources Root
2. Refresh project: Ctrl+Shift+F5 (IntelliJ)
3. Clean IDE cache if needed
```

---

## Next Steps

After setup is complete:

1. **Read Documentation**
   - Start with [docs/README.md](../docs/README.md)
   - Review [docs/TESTING_GUIDE.md](../docs/TESTING_GUIDE.md)

2. **Run Tests**
   - Execute: `./gradlew.bat test`
   - View report: `build/reports/tests/test/index.html`

3. **Explore Test Code**
   - Repository tests: `src/test/java/.../repository/`
   - Service tests: `src/test/java/.../serviceImpl/`
   - Controller tests: `src/test/java/.../controller/`

4. **Run Load Tests**
   - Navigate: `cd jmeter_tests`
   - Execute: `run_load_tests.bat` (Windows) or `./run_load_tests.sh`
   - View results: `load_test_results/*/index.html`

---

## Quick Reference

| Task | Command |
|------|---------|
| Build project | `./gradlew.bat clean build` |
| Run all tests | `./gradlew.bat test` |
| Run specific test | `./gradlew.bat test --tests "ClassName"` |
| View test report | `build/reports/tests/test/index.html` |
| Run load tests | `cd jmeter_tests && run_load_tests.bat` |
| Start JMeter GUI | `jmeter` |
| Check Java version | `java -version` |

---

## Support

If you encounter issues:
1. Check [QUICK_REFERENCE.md](QUICK_REFERENCE.md) troubleshooting section
2. Review test error messages carefully
3. Check application logs
4. Consult [TESTING_GUIDE.md](TESTING_GUIDE.md)

---

**Setup Complete!** 🎉

Next: Read [docs/TESTING_GUIDE.md](TESTING_GUIDE.md) for testing overview.

