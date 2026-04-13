# Load Testing with JMeter - Product Service

## Overview
This guide provides complete load testing setup for the Product Service using Apache JMeter.

## Prerequisites
1. JMeter installed (download from https://jmeter.apache.org/download_jmeter.cgi)
2. Product Service running on localhost:8080
3. Java 11+

## Setup Instructions

### 1. Download JMeter
- Extract JMeter zip file
- Add JMeter bin directory to PATH

### 2. Verify Installation
```bash
jmeter --version
```

## Load Test Scenarios

### Scenario 1: Category Endpoints Load Test
- **Thread Group:** 50 concurrent users
- **Ramp-up:** 30 seconds
- **Duration:** 5 minutes
- **Endpoints:**
  - GET /api/categories
  - GET /api/categories/{id}

### Scenario 2: Product Endpoints Load Test
- **Thread Group:** 100 concurrent users
- **Ramp-up:** 60 seconds
- **Duration:** 10 minutes
- **Endpoints:**
  - GET /api/products
  - GET /api/products/{id}
  - POST /api/products/by-ids

### Scenario 3: Cart Operations Load Test
- **Thread Group:** 200 concurrent users
- **Ramp-up:** 120 seconds
- **Duration:** 10 minutes
- **Endpoints:**
  - GET /api/cart/{userId}
  - POST /api/cart/{userId}/items
  - PATCH /api/cart/{userId}/items/{productId}
  - POST /api/cart/{userId}/checkout

### Scenario 4: Spike Test
- **Thread Group:** 500 sudden users
- **Instant ramp-up:** 0 seconds
- **Duration:** 2 minutes
- **Monitors system behavior under sudden load**

### Scenario 5: Stress Test
- **Thread Group:** 1000 concurrent users
- **Ramp-up:** 300 seconds
- **Duration:** 5 minutes
- **Find breaking point of system**

## Expected Performance Targets

| Metric | Target | Acceptable |
|--------|--------|-----------|
| Average Response Time | < 200ms | < 500ms |
| 95th Percentile | < 500ms | < 1000ms |
| 99th Percentile | < 1000ms | < 2000ms |
| Error Rate | < 1% | < 5% |
| Throughput | > 100 req/sec | > 50 req/sec |

## Running Load Tests via GUI (Simple Method)

### Step 1: Start JMeter GUI
```bash
jmeter
```

### Step 2: Create Test Plan
1. Right-click on "Test Plan"
2. Add → Thread Groups → Thread Group
3. Set: Threads: 50, Ramp-up: 30, Duration: 300

### Step 3: Add HTTP Requests
1. Right-click on Thread Group
2. Add → Sampler → HTTP Request
3. Configure:
   - Server Name: localhost
   - Port: 8080
   - Path: /api/categories

### Step 4: Add Listeners
1. Right-click on Test Plan
2. Add → Listener → View Results Tree
3. Add → Listener → Summary Report
4. Add → Listener → Response Time Graph

### Step 5: Run Test
1. Click Start (Green Play Button)
2. Monitor results in real-time

## Running Load Tests via Command Line (Recommended for CI/CD)

### Basic Load Test
```bash
jmeter -n -t CategoryLoadTest.jmx -l results.jtl -j jmeter.log
```

### With HTML Report Generation
```bash
jmeter -n -t CategoryLoadTest.jmx -l results.jtl -j jmeter.log -e -o results_html
```

### With Custom Properties
```bash
jmeter -n -t ProductLoadTest.jmx \
  -Jthreads=100 \
  -Jrampup=60 \
  -Jduration=300 \
  -l results.jtl \
  -j jmeter.log
```

## Test Plan Components

### Thread Group Configuration
```
Number of Threads (users): 50-1000 (depending on scenario)
Ramp-up Time (seconds): 30-300 seconds
Loop Count: 1 (run once) or -1 (infinite during duration)
Duration (seconds): 300-600 seconds
```

### HTTP Request Defaults
```
Protocol: HTTP
Domain: localhost
Port: 8080
Path: /api/[endpoint]
Method: GET/POST/PATCH
```

### Response Assertions
```
Assert that:
- Response Code is 200 or 201
- Response does not contain "error"
- Response Time < 500ms
```

### CSV Data Set Config
```
File: test_data.csv
Delimiter: ,
Variable Names: userId, productId
```

## Performance Analysis

### Key Metrics to Monitor
1. **Response Time**
   - Average
   - Percentiles (50th, 95th, 99th)
   - Min/Max

2. **Throughput**
   - Requests per second
   - Bytes per second

3. **Error Rate**
   - Total errors
   - Error percentage
   - Error types

4. **Resource Usage**
   - Memory consumption
   - CPU usage
   - Database connections

## Test Data Preparation

### Generate Test Data
```bash
# Create test_data.csv with user IDs and product IDs
cat > test_data.csv << EOF
userId,productId
1,1
2,2
3,3
...
1000,100
EOF
```

## Interpreting Results

### Summary Report Fields
- **Samples:** Number of requests sent
- **Average:** Mean response time (ms)
- **Median:** 50th percentile response time
- **90% Line:** 90th percentile response time
- **95% Line:** 95th percentile response time
- **99% Line:** 99th percentile response time
- **Min:** Minimum response time
- **Max:** Maximum response time
- **Error %:** Percentage of failed requests
- **Throughput:** Requests per second

### Good Results Indicators
✅ Error rate < 1%
✅ Average response time < 200ms
✅ 95th percentile < 500ms
✅ Throughput > 100 req/sec

### Red Flags
❌ Error rate > 5%
❌ Response time increasing over time
❌ System timeout errors
❌ Throughput decreasing under sustained load

## Optimizing Results

If tests reveal performance issues:

1. **Check Database Indexes**
   - Ensure all frequently queried fields are indexed
   
2. **Optimize Queries**
   - Check for N+1 query problems
   - Use JPA projections
   
3. **Increase Resources**
   - More memory for application
   - More database connections
   - Load balancing

4. **Cache Implementation**
   - Add Redis caching
   - Implement HTTP caching headers
   - Database query caching

5. **Connection Pooling**
   - Increase connection pool size
   - Monitor connection exhaustion

## CI/CD Integration

### Jenkins Example
```groovy
stage('Load Test') {
    steps {
        sh 'jmeter -n -t LoadTests.jmx -l results.jtl -j jmeter.log -e -o results_html'
        publishHTML([
            reportDir: 'results_html',
            reportFiles: 'index.html',
            reportName: 'JMeter Report'
        ])
    }
}
```

### GitHub Actions Example
```yaml
- name: Run Load Tests
  run: |
    jmeter -n -t LoadTests.jmx \
      -l results.jtl \
      -j jmeter.log \
      -e -o results_html
```

## Troubleshooting

### JMeter Issues
1. **Out of Memory**
   - Increase JVM heap: `jmeter -Xmx2g`

2. **Connection Refused**
   - Verify application is running on correct port
   - Check firewall settings

3. **High Error Rate**
   - Check application logs
   - Verify test data is correct
   - Check endpoint paths

4. **Inconsistent Results**
   - Run multiple times
   - Warm up the application first
   - Check for external factors (network, CPU)

## Best Practices

✅ Start with low load and gradually increase
✅ Warm up the application before tests
✅ Run tests multiple times for consistency
✅ Monitor server resources during tests
✅ Save test results for comparison
✅ Document all test scenarios
✅ Include think time between requests
✅ Use realistic test data
✅ Test during off-peak hours
✅ Have baseline measurements

## Next Steps

1. Start with Category endpoints load test (50 users)
2. Progress to Product endpoints (100 users)
3. Test Cart operations (200 users)
4. Run spike test (sudden 500 users)
5. Identify any bottlenecks
6. Optimize and retest
7. Document baseline performance

## Sample Load Test Execution

```bash
# Scenario 1: Category Load Test
jmeter -n -t CategoryLoadTest.jmx -l category_results.jtl -j jmeter.log -e -o category_report

# Scenario 2: Product Load Test
jmeter -n -t ProductLoadTest.jmx -l product_results.jtl -j jmeter.log -e -o product_report

# Scenario 3: Cart Load Test
jmeter -n -t CartLoadTest.jmx -l cart_results.jtl -j jmeter.log -e -o cart_report

# Scenario 4: Spike Test
jmeter -n -t SpikeTest.jmx -l spike_results.jtl -j jmeter.log -e -o spike_report
```

## Performance Baseline (Expected Results)

Based on performance tests already run:
- Single operation: < 500ms ✅
- Throughput: > 5 ops/sec ✅
- Memory: < 100MB for 50 operations ✅

With proper load testing, we should see:
- Throughput: > 50-100 req/sec
- Average response: 100-200ms
- Error rate: < 1%

