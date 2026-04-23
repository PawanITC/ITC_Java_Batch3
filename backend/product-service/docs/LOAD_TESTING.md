# Load Testing with JMeter - Product Service

Complete guide for setting up and running load tests using Apache JMeter.

## Table of Contents

1. [Installation](#installation)
2. [Quick Start](#quick-start)
3. [Test Scenarios](#test-scenarios)
4. [Running Tests](#running-tests)
5. [Analyzing Results](#analyzing-results)
6. [Performance Targets](#performance-targets)
7. [Troubleshooting](#troubleshooting)
8. [CI/CD Integration](#cicd-integration)

---

## Installation

### Prerequisites
- Java 11 or higher installed
- Product Service running on `localhost:8080`
- 1-5GB disk space for test results

### Step 1: Download JMeter

**Option A: Manual Download**
```bash
# From https://jmeter.apache.org/download_jmeter.cgi
# Extract and add to PATH
```

**Option B: Package Manager**
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
# Expected: Apache JMeter 5.5 (or newer)
```

---

## Quick Start

### Fastest Way: Automated Scripts

**Windows**
```bash
cd jmeter_tests
run_load_tests.bat
```

**Linux/macOS**
```bash
cd jmeter_tests
chmod +x run_load_tests.sh
./run_load_tests.sh
```

### Manual Single Test
```bash
jmeter -n -t jmeter_tests/CategoryLoadTest.jmx \
  -l results/category.jtl \
  -e -o results/category_report
```

### GUI (Interactive)
```bash
jmeter
# File → Open → Select test plan → Click Play button
```

---

## Test Scenarios

### Scenario 1: Category Load Test (Light)
```
Purpose: Validate category endpoints
Users: 50 concurrent
Ramp-up: 30 seconds
Duration: 5 minutes (300 seconds)
Endpoints:
  - GET /api/categories
  - GET /api/categories/{id}
```

### Scenario 2: Product Load Test (Medium)
```
Purpose: Validate product endpoints
Users: 100 concurrent
Ramp-up: 60 seconds
Duration: 10 minutes (600 seconds)
Endpoints:
  - GET /api/products
  - GET /api/products/{id}
  - POST /api/products/by-ids
```

### Scenario 3: Cart Workflow Test (Heavy)
```
Purpose: Simulate complete shopping workflow
Users: 200 concurrent
Ramp-up: 120 seconds
Duration: 10 minutes (600 seconds)
Workflow:
  1. GET /api/cart/{userId}
  2. POST /api/cart/{userId}/items (Add product 1)
  3. POST /api/cart/{userId}/items (Add product 2)
  4. PATCH /api/cart/{userId}/items/{id} (Update quantity)
  5. POST /api/cart/{userId}/checkout
```

### Scenario 4: Spike Test (Sudden Load)
```
Purpose: Test system under sudden traffic surge
Users: 500 concurrent (instant)
Ramp-up: 10 seconds
Duration: 2 minutes (120 seconds)
Use: CartLoadTest.jmx with adjusted parameters
```

---

## Running Tests

### Automated Execution (Recommended)

**Windows**
```batch
cd jmeter_tests
run_load_tests.bat
```

**Linux/macOS**
```bash
cd jmeter_tests
./run_load_tests.sh
```

Script will:
- Run all 4 test scenarios
- Generate HTML reports
- Create summary report
- Save results with timestamp

### Manual Execution

**Category Test**
```bash
jmeter -n -t CategoryLoadTest.jmx \
  -l results/category.jtl \
  -j results/category.log \
  -e -o results/category_report
```

**Product Test**
```bash
jmeter -n -t ProductLoadTest.jmx \
  -l results/product.jtl \
  -j results/product.log \
  -e -o results/product_report
```

**Cart Test**
```bash
jmeter -n -t CartLoadTest.jmx \
  -l results/cart.jtl \
  -j results/cart.log \
  -e -o results/cart_report
```

**Spike Test**
```bash
jmeter -n -t CartLoadTest.jmx \
  -l results/spike.jtl \
  -j results/spike.log \
  -Jthreads=500 \
  -Jrampup=10 \
  -Jduration=120 \
  -e -o results/spike_report
```

### Custom Parameters
```bash
jmeter -n -t test.jmx \
  -l results.jtl \
  -Jthreads=300 \        # Custom thread count
  -Jrampup=120 \         # Custom ramp-up
  -Jduration=600 \       # Custom duration
  -e -o report
```

### Increase JVM Memory
```bash
jmeter -Xmx4g -n -t test.jmx -l results.jtl -e -o report
```

---

## Analyzing Results

### View HTML Reports
After tests complete, open in browser:
```
load_test_results/CategoryLoadTest_*/index.html
load_test_results/ProductLoadTest_*/index.html
load_test_results/CartLoadTest_*/index.html
load_test_results/SpikeTest_*/index.html
```

### Key Metrics in Reports

**Summary Table**
- **Samples:** Total requests sent
- **Average:** Mean response time (ms)
- **Median:** 50th percentile response time
- **90% Line:** 90th percentile response time
- **95% Line:** 95th percentile response time
- **99% Line:** 99th percentile response time
- **Min/Max:** Minimum and maximum response times
- **Error %:** Percentage of failed requests
- **Throughput:** Requests per second

**Response Time Graph**
- Shows response time distribution over test duration
- Look for increases indicating slowdown
- Identifies performance degradation patterns

**Transactions Per Second (TPS)**
- Shows throughput stability over time
- Should remain stable or increase initially
- Sudden drops indicate issues

### Analysis Tips
✅ Compare percentiles: 50th should be close to average
✅ Check 95th/99th percentiles for outliers
✅ Low error rate is crucial (< 1% is excellent)
✅ Consistent throughput indicates stability

---

## Performance Targets

| Metric | Target | Acceptable | Warning |
|--------|--------|-----------|---------|
| Average Response | < 200ms | < 500ms | > 500ms |
| 95th Percentile | < 500ms | < 1000ms | > 1000ms |
| 99th Percentile | < 1000ms | < 2000ms | > 2000ms |
| Error Rate | < 1% | < 5% | > 5% |
| Throughput | > 100 req/sec | > 50 req/sec | < 50 req/sec |

### Interpreting Results

✅ **Good Results**
- Error rate < 1%
- Average response < 200ms
- 95th percentile < 500ms
- Stable throughput
- No timeout errors

❌ **Red Flags**
- Error rate > 5%
- Response time increasing over time
- System timeout errors
- Throughput decreasing
- High memory usage

---

## Troubleshooting

### Issue: "Connection refused"
```
Problem: Cannot connect to application
Solution:
1. Verify app running: curl http://localhost:8080/actuator/health
2. Check port: netstat -an | grep 8080
3. Check firewall settings
```

### Issue: "Out of Memory"
```
Problem: JMeter runs out of memory
Solution:
jmeter -Xmx4g -n -t test.jmx -l results.jtl
```

### Issue: High Error Rate
```
Problem: Many requests failing
Solutions:
1. Check application logs
2. Reduce number of threads
3. Verify test endpoints exist
4. Monitor server resources
5. Increase ramp-up time
```

### Issue: Inconsistent Results
```
Problem: Results vary significantly
Solutions:
1. Run test multiple times
2. Warm up application first
3. Stop other resource-heavy processes
4. Check for network issues
5. Increase ramp-up time
```

---

## Performance Optimization

If tests reveal issues:

### 1. Database Level
- Add missing indexes
- Optimize queries
- Check for N+1 problems
- Monitor slow queries

### 2. Application Level
- Implement caching (Redis)
- Use connection pooling
- Optimize batch sizes
- Add pagination

### 3. Infrastructure
- Increase JVM heap
- Increase database connections
- Use load balancing
- Scale horizontally

### 4. JMeter Tuning
- Increase think time
- Reduce concurrent threads
- Verify connection pooling
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
        run: sudo apt-get install -y jmeter
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
stage('Load Test') {
    steps {
        dir('jmeter_tests') {
            sh 'jmeter -n -t CategoryLoadTest.jmx \
                -l results.jtl -e -o results_report'
        }
        archiveArtifacts 'jmeter_tests/results_report/'
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
| Report Generation | ~5 min | ~32 min |

---

## Best Practices

✅ Start with low load and gradually increase
✅ Warm up application before actual tests
✅ Run tests multiple times for consistency
✅ Monitor server resources during tests
✅ Save test results for comparison
✅ Document all test scenarios
✅ Use realistic test data
✅ Test during off-peak hours
✅ Establish baseline measurements
✅ Run tests regularly to catch regressions

---

## Test Files Location

```
jmeter_tests/
├── CategoryLoadTest.jmx      (50 users, 5 min)
├── ProductLoadTest.jmx       (100 users, 10 min)
├── CartLoadTest.jmx          (200 users, 10 min)
├── run_load_tests.bat        (Windows automation)
├── run_load_tests.sh         (Linux/macOS automation)
└── load_test_results/        (Results directory)
```

---

## Quick Reference

### Run All Tests (Automated)
```bash
jmeter_tests/run_load_tests.bat    # Windows
./jmeter_tests/run_load_tests.sh   # Linux/macOS
```

### Run Single Test
```bash
jmeter -n -t jmeter_tests/CategoryLoadTest.jmx \
  -l results.jtl -e -o results_report
```

### Run with GUI
```bash
jmeter
# File → Open → Select .jmx → Click Play
```

### Increase Memory
```bash
jmeter -Xmx4g -n -t test.jmx -l results.jtl
```

---

## Summary

| Aspect | Details |
|--------|---------|
| Test Plans | 4 scenarios (light → heavy → spike) |
| Automation | Windows & Linux scripts |
| Reports | HTML dashboards with graphs |
| Performance Targets | Clear thresholds defined |
| Documentation | Complete setup guides |
| Status | Ready to run |

**Next Step:** Run load tests with `run_load_tests.bat` or `run_load_tests.sh`

See [docs/QUICK_REFERENCE.md](QUICK_REFERENCE.md) for command reference.

