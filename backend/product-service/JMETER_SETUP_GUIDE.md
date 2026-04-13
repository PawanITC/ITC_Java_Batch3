# JMeter Load Testing - Complete Setup & Execution Guide

## Table of Contents
1. [Installation](#installation)
2. [Quick Start](#quick-start)
3. [Test Scenarios](#test-scenarios)
4. [Running Tests](#running-tests)
5. [Analyzing Results](#analyzing-results)
6. [Troubleshooting](#troubleshooting)

---

## Installation

### Prerequisites
- Java 11 or higher installed
- Product Service running on `localhost:8080`
- Sufficient disk space for results (1-5GB recommended)

### Step 1: Download JMeter

**Option A: Manual Download**
```bash
# Download from official site
https://jmeter.apache.org/download_jmeter.cgi

# Extract archive
tar -xzf apache-jmeter-5.5.tgz
# or on Windows, use Windows Explorer or:
# Extract apache-jmeter-5.5.zip
```

**Option B: Using Package Manager**
```bash
# macOS
brew install jmeter

# Ubuntu/Debian
sudo apt-get install jmeter

# Windows (Chocolatey)
choco install jmeter
```

### Step 2: Verify Installation
```bash
jmeter --version
# Expected output: Apache JMeter 5.5 (or newer)
```

### Step 3: Set Environment Variable (Optional)
```bash
# Linux/macOS
export JMETER_HOME=/path/to/jmeter
export PATH=$PATH:$JMETER_HOME/bin

# Windows (Command Prompt)
set JMETER_HOME=C:\path\to\jmeter
set PATH=%PATH%;%JMETER_HOME%\bin

# Windows (PowerShell)
$env:JMETER_HOME="C:\path\to\jmeter"
```

---

## Quick Start

### Fastest Way to Run Tests

**Using Batch Script (Windows)**
```bash
cd jmeter_tests
run_load_tests.bat
```

**Using Shell Script (Linux/macOS)**
```bash
cd jmeter_tests
chmod +x run_load_tests.sh
./run_load_tests.sh
```

**Manual Single Test**
```bash
jmeter -n -t jmeter_tests/CategoryLoadTest.jmx \
  -l results/category.jtl \
  -j results/category.log \
  -e -o results/category_report
```

---

## Test Scenarios

### Scenario 1: Category Load Test
```
Purpose: Validate category endpoints under light load
File: CategoryLoadTest.jmx
Users: 50 concurrent
Ramp-up: 30 seconds
Duration: 5 minutes
Endpoints:
  - GET /api/categories
  - GET /api/categories/{id}
```

### Scenario 2: Product Load Test
```
Purpose: Validate product endpoints under medium load
File: ProductLoadTest.jmx
Users: 100 concurrent
Ramp-up: 60 seconds
Duration: 10 minutes
Endpoints:
  - GET /api/products
  - GET /api/products/{id}
  - POST /api/products/by-ids
```

### Scenario 3: Cart Workflow Test
```
Purpose: Simulate real shopping workflow under higher load
File: CartLoadTest.jmx
Users: 200 concurrent
Ramp-up: 120 seconds
Duration: 10 minutes
Workflow:
  1. GET /api/cart/{userId}
  2. POST /api/cart/{userId}/items (Add product 1)
  3. POST /api/cart/{userId}/items (Add product 2)
  4. PATCH /api/cart/{userId}/items/1 (Update quantity)
  5. POST /api/cart/{userId}/checkout
```

### Scenario 4: Spike Test
```
Purpose: Observe system behavior under sudden load spike
File: CartLoadTest.jmx (reused)
Users: 500 concurrent (instant)
Ramp-up: 10 seconds
Duration: 2 minutes
Tests system resilience to sudden traffic spikes
```

---

## Running Tests

### Method 1: Using GUI (Interactive)

```bash
# Start JMeter GUI
jmeter

# In GUI:
# 1. File → Open → Select test plan (e.g., CategoryLoadTest.jmx)
# 2. Configure thread group if needed
# 3. Click green play button to start
# 4. Monitor results in listeners
```

### Method 2: Command Line (Non-Interactive)

**Basic Execution**
```bash
jmeter -n -t CategoryLoadTest.jmx \
  -l results.jtl \
  -j jmeter.log
```

**With HTML Report**
```bash
jmeter -n -t CategoryLoadTest.jmx \
  -l results.jtl \
  -j jmeter.log \
  -e -o results_report
```

**With Custom Parameters**
```bash
jmeter -n -t CartLoadTest.jmx \
  -l results.jtl \
  -j jmeter.log \
  -Jthreads=300 \
  -Jrampup=60 \
  -Jduration=300 \
  -e -o results_report
```

### Method 3: Using Batch Script

**Edit script first:**
1. Update `JMETER_HOME` to your JMeter installation path
2. Optionally adjust thread counts in script
3. Run script:

```bash
# Windows
run_load_tests.bat

# Linux/macOS
./run_load_tests.sh
```

### Method 4: Running All Tests Sequentially

```bash
# Run all scenarios with increasing load
jmeter -n -t CategoryLoadTest.jmx -l cat.jtl -j cat.log -e -o cat_report && \
jmeter -n -t ProductLoadTest.jmx -l prod.jtl -j prod.log -e -o prod_report && \
jmeter -n -t CartLoadTest.jmx -l cart.jtl -j cart.log -e -o cart_report && \
jmeter -n -t CartLoadTest.jmx -l spike.jtl -j spike.log \
  -Jthreads=500 -Jrampup=10 -Jduration=120 -e -o spike_report

echo "All tests completed!"
```

---

## Analyzing Results

### HTML Reports
Located in `results_report/index.html` or `jmeter_tests/load_test_results/`

**Key Metrics to Review:**
1. **Sampler Statistics**
   - Count: Total requests sent
   - Average: Mean response time
   - Median: 50th percentile
   - 90%/95%/99%: Percentile response times

2. **Response Times Graph**
   - Shows response time distribution over time
   - Look for increases indicating system slowdown

3. **Transactions Per Second**
   - Shows throughput over time
   - Should remain stable or increase initially

4. **Errors**
   - Total error count and percentage
   - Types of errors encountered

### JTL Results File
Raw results in CSV format:

```bash
# View results
cat results.jtl

# Basic analysis
grep -c "true" results.jtl  # Count successful requests
grep -c "false" results.jtl # Count failed requests
```

### Manual Analysis Script

```bash
# Extract response times
awk -F',' 'NR>1 {print $2}' results.jtl | sort -n | tail -100

# Calculate average
awk -F',' 'NR>1 {sum+=$2; count++} END {print "Average:", sum/count}' results.jtl

# Count errors
awk -F',' '$NF ~ /false/ {count++} END {print "Errors:", count}' results.jtl
```

---

## Performance Targets

| Metric | Target | Acceptable | Warning |
|--------|--------|-----------|---------|
| Average Response Time | < 200ms | < 500ms | > 500ms |
| 95th Percentile | < 500ms | < 1000ms | > 1000ms |
| 99th Percentile | < 1000ms | < 2000ms | > 2000ms |
| Error Rate | < 1% | < 5% | > 5% |
| Throughput | > 100 req/sec | > 50 req/sec | < 50 req/sec |

---

## Troubleshooting

### Issue: "Connection refused" Error
```
Problem: Cannot connect to application
Solution:
1. Verify application is running: curl http://localhost:8080/actuator/health
2. Check port: netstat -an | grep 8080
3. Check firewall settings
4. Verify URL in test plan
```

### Issue: "Out of Memory" Error
```
Problem: JMeter runs out of memory during test
Solution:
# Increase JVM heap
jmeter -Xmx4g -n -t test.jmx -l results.jtl

# Or set environment variable
export _JVM_OPT=-Xmx4g
```

### Issue: High Error Rate
```
Problem: Many requests failing
Solutions:
1. Check application logs
2. Reduce number of threads
3. Verify test data is correct
4. Check if endpoints return 404
5. Monitor server resources (CPU, memory, connections)
```

### Issue: Inconsistent Results
```
Problem: Test results vary significantly
Solutions:
1. Run test multiple times
2. Warm up application first
3. Ensure other processes aren't consuming resources
4. Check for network issues
5. Increase ramp-up time for stability
```

### Issue: "Address already in use"
```
Problem: Port already in use
Solution:
# Find process using port 8080
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill process
kill -9 <PID>  # macOS/Linux
taskkill /PID <PID> /F  # Windows
```

---

## Performance Optimization Tips

If tests show poor performance:

1. **Database Level**
   - Add missing indexes
   - Optimize N+1 queries
   - Monitor slow queries

2. **Application Level**
   - Implement caching (Redis)
   - Use connection pooling
   - Optimize batch sizes
   - Add pagination

3. **Infrastructure**
   - Increase JVM heap
   - Increase database connections
   - Use load balancing
   - Scale horizontally

4. **JMeter**
   - Increase think time
   - Reduce concurrent threads
   - Check for connection pooling
   - Use Keep-Alive

---

## CI/CD Integration

### GitHub Actions
```yaml
name: Load Test
on: [schedule]
jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Install JMeter
        run: |
          sudo apt-get update
          sudo apt-get install -y jmeter
      - name: Run Load Tests
        run: |
          cd jmeter_tests
          jmeter -n -t CategoryLoadTest.jmx \
            -l results.jtl -e -o results_report
      - name: Upload Results
        uses: actions/upload-artifact@v2
        with:
          name: load-test-results
          path: jmeter_tests/results_report/
```

### Jenkins
```groovy
pipeline {
    triggers {
        cron('H H * * *')  // Daily
    }
    stages {
        stage('Load Test') {
            steps {
                dir('jmeter_tests') {
                    sh 'jmeter -n -t CategoryLoadTest.jmx \
                        -l results.jtl -j jmeter.log \
                        -e -o results_report'
                }
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts 'jmeter_tests/results_report/'
            }
        }
    }
}
```

---

## Expected Test Duration

| Test | Duration | Total Time |
|------|----------|-----------|
| Category Load | 5 min | 5 min |
| Product Load | 10 min | 15 min |
| Cart Load | 10 min | 25 min |
| Spike Test | 2 min | 27 min |
| Report Generation | ~5 min | 32 min |

---

## Next Steps After Load Testing

1. **Analyze results** - Review HTML reports
2. **Identify bottlenecks** - Check response times and errors
3. **Optimize** - Implement fixes for identified issues
4. **Retest** - Run tests again to verify improvements
5. **Establish baseline** - Document performance metrics
6. **Monitor** - Set up alerts for performance degradation

---

## Additional Resources

- [JMeter Official Documentation](https://jmeter.apache.org/usermanual/index.html)
- [Best Practices Guide](https://jmeter.apache.org/usermanual/best-practices.html)
- [Functions & Variables](https://jmeter.apache.org/usermanual/functions.html)
- [Performance Tuning](https://jmeter.apache.org/usermanual/properties_reference.html)

---

**Happy Load Testing!** 🚀

