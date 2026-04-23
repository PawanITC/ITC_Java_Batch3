# Quick Reference - Commands & Troubleshooting

Quick lookup for common commands and solutions.

## 🚀 Test Execution

### Run All Tests
```bash
./gradlew.bat test
```

### Run by Layer
```bash
# Repository tests only
./gradlew.bat test --tests "*.repository.*"

# Service tests only
./gradlew.bat test --tests "*.serviceImpl.*"

# Controller tests only
./gradlew.bat test --tests "*.controller.*"

# Integration tests only
./gradlew.bat test --tests "*Integration*"

# Performance tests only
./gradlew.bat test --tests "*Performance*"
```

### Run by Feature
```bash
# Category tests
./gradlew.bat test --tests "*Category*"

# Product tests
./gradlew.bat test --tests "*Product*"

# Cart tests
./gradlew.bat test --tests "*Cart*"
```

### Run Specific Class
```bash
./gradlew.bat test --tests "CategoryServiceIntegrationTest"
```

### Run Specific Method
```bash
./gradlew.bat test --tests "CartControllerTest.shouldAddItemToCartSuccessfully"
```

---

## 📊 Load Testing (JMeter)

### Run All Load Tests (Automated)
```bash
# Windows
cd jmeter_tests && run_load_tests.bat

# Linux/macOS
cd jmeter_tests && ./run_load_tests.sh
```

### Run Single Load Test
```bash
# Category test (50 users, 5 min)
jmeter -n -t CategoryLoadTest.jmx -l results.jtl -e -o results_report

# Product test (100 users, 10 min)
jmeter -n -t ProductLoadTest.jmx -l results.jtl -e -o results_report

# Cart test (200 users, 10 min)
jmeter -n -t CartLoadTest.jmx -l results.jtl -e -o results_report

# Spike test (500 users, 2 min)
jmeter -n -t CartLoadTest.jmx -l results.jtl \
  -Jthreads=500 -Jrampup=10 -Jduration=120 \
  -e -o results_report
```

### Custom Parameters
```bash
jmeter -n -t test.jmx -l results.jtl \
  -Jthreads=300 \        # Thread count
  -Jrampup=120 \         # Ramp-up seconds
  -Jduration=600 \       # Duration seconds
  -e -o report
```

### Run JMeter GUI
```bash
jmeter
# File → Open → Select .jmx file → Click green Play button
```

### Increase JVM Memory
```bash
jmeter -Xmx4g -n -t test.jmx -l results.jtl
```

---

## 📈 View Results

### Unit Test Report
```
build/reports/tests/test/index.html
```

### Load Test Reports
```
jmeter_tests/load_test_results/CategoryLoadTest_*/index.html
jmeter_tests/load_test_results/ProductLoadTest_*/index.html
jmeter_tests/load_test_results/CartLoadTest_*/index.html
jmeter_tests/load_test_results/SpikeTest_*/index.html
```

---

## 🔧 Troubleshooting

### Test Compilation Issues

**Issue: "cannot find symbol" error**
```
Solution:
./gradlew.bat clean build
./gradlew.bat compileTestJava
```

**Issue: "incompatible types" error**
```
Solution:
1. Check imports are correct
2. Verify type conversions
3. Run: ./gradlew.bat clean test
```

### Test Execution Issues

**Issue: "Connection refused" (JMeter)**
```
Solution:
1. Verify app running: curl http://localhost:8080/actuator/health
2. Check port: netstat -an | grep 8080
3. Check firewall
```

**Issue: "Out of Memory"**
```
Solution:
./gradlew.bat test -Xmx2g
# or for JMeter:
jmeter -Xmx4g -n -t test.jmx
```

**Issue: "Address already in use"**
```
Solution:
# Find process on port 8080
lsof -i :8080              # macOS/Linux
netstat -ano | findstr 8080 # Windows

# Kill process
kill -9 <PID>              # macOS/Linux
taskkill /PID <PID> /F     # Windows
```

### JMeter Issues

**Issue: "Connection timeout"**
```
Solution:
1. Increase timeout in test plan
2. Reduce thread count
3. Check server resources
```

**Issue: "High error rate"**
```
Solution:
1. Check application logs
2. Reduce concurrent threads
3. Increase ramp-up time
4. Verify endpoints exist
```

**Issue: "Inconsistent results"**
```
Solution:
1. Run test multiple times
2. Warm up application first
3. Stop other processes
4. Increase ramp-up time
```

### Database Issues

**Issue: "Unique constraint violation" in tests**
```
Solution:
Add @Transactional to test class:
@SpringBootTest
@Transactional  // ← Add this
class MyIntegrationTest { }
```

**Issue: "No qualifying bean found"**
```
Solution:
1. Verify @MockBean is used
2. Check @ExtendWith(MockitoExtension.class)
3. Verify @SpringBootTest is present
```

---

## ✅ Performance Targets

### Response Time
```
Single Operation: < 500ms
Average Response: < 200ms
95th Percentile: < 500ms
99th Percentile: < 1000ms
```

### Throughput & Errors
```
Throughput: > 5 ops/sec (unit tests)
Throughput: > 100 req/sec (load tests)
Error Rate: < 1% (unit/load tests)
Memory: < 100MB for 50 operations
```

---

## 📋 Checklist Before Release

### Testing
- [ ] All 155 tests passing
- [ ] No flaky tests
- [ ] Performance tests meet targets
- [ ] Load tests completed successfully
- [ ] Error rate < 1%

### Documentation
- [ ] Tests documented
- [ ] Commands verified
- [ ] README updated
- [ ] Setup guide current

### Deployment
- [ ] Tests run in CI/CD
- [ ] Results logged
- [ ] Performance baseline established
- [ ] Team notified

---

## 🎯 Common Workflows

### Daily Development
```bash
# Quick test run
./gradlew.bat test

# Specific feature
./gradlew.bat test --tests "*Cart*"

# View results
build/reports/tests/test/index.html
```

### Before Commit
```bash
# Full test suite
./gradlew.bat clean test

# Verify no errors
# View report: build/reports/tests/test/index.html
```

### Performance Validation
```bash
# Run load tests
cd jmeter_tests
./run_load_tests.bat (Windows) or ./run_load_tests.sh (Linux)

# View reports
load_test_results/*/index.html
```

### Troubleshooting
```bash
# Verbose output
./gradlew.bat test --info

# Stack traces
./gradlew.bat test --stacktrace

# Single test with debug
./gradlew.bat test --tests "MyTest" -d
```

---

## 📚 Key Files

### Test Configuration
```
src/main/resources/application-test.yml
```

### Test Files
```
src/test/java/com/itc/funkart/product_service/
├── repository/
├── serviceImpl/
├── controller/
├── integration/
└── performance/
```

### Load Testing
```
jmeter_tests/
├── *.jmx
├── run_load_tests.bat
└── run_load_tests.sh
```

---

## 🔗 Related Documentation

- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Comprehensive testing overview
- [LOAD_TESTING.md](LOAD_TESTING.md) - Load testing setup & execution
- [SETUP.md](SETUP.md) - Environment configuration
- [ARCHITECTURE.md](ARCHITECTURE.md) - Test architecture

---

## ⏱️ Estimated Times

| Task | Time |
|------|------|
| All unit tests | 1-2 min |
| All integration tests | 1-2 min |
| All performance tests | 1-2 min |
| Category load test | 5 min |
| Product load test | 10 min |
| Cart load test | 10 min |
| Spike test | 2 min |
| Full test suite + load | ~32 min |

---

**Need help?** Check [TESTING_GUIDE.md](TESTING_GUIDE.md) or [LOAD_TESTING.md](LOAD_TESTING.md)

