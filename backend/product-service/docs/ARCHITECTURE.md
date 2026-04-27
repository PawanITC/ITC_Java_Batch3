# Test Architecture & Overview

Complete overview of the testing architecture and approach.

## Test Pyramid

```
                    PERFORMANCE (21)
                    Response Time Tests
                    - < 500ms validation
                    - Throughput measurement
                            ↓
            INTEGRATION (41)
        End-to-End Workflow Tests
        - Full application context
        - Real dependencies
        - Complete processes
                ↓
        CONTROLLER (23)
        REST API Endpoint Tests
        - HTTP endpoints
        - Status codes
        - Request/Response
            ↓
        SERVICE (27)
        Business Logic Tests
        - Mocked dependencies
        - Exception handling
        - Edge cases
            ↓
    REPOSITORY (19)
    Database Operation Tests
    - CRUD operations
    - Relationships
    - Constraints
```

---

## Testing Levels Explained

### Level 1: Repository Tests (Base)
**Purpose:** Validate data persistence

**Approach:**
- Direct database testing with H2 in-memory
- No mocking, real entities
- Test CRUD operations

**Count:** 19 tests

**Example:**
```java
@DataJpaTest
class CartRepositoryTest {
    @Test
    void shouldSaveCart() { /* ... */ }
}
```

---

### Level 2: Service Tests
**Purpose:** Validate business logic

**Approach:**
- Unit testing with mocks
- Isolated from database
- Test service interactions

**Count:** 27 tests

**Example:**
```java
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock CartRepository cartRepository;
    
    @Test
    void shouldAddItemToCart() { /* ... */ }
}
```

---

### Level 3: Controller Tests
**Purpose:** Validate REST endpoints

**Approach:**
- Slice testing with MockMvc
- Test HTTP layer
- Mock services

**Count:** 23 tests

**Example:**
```java
@WebMvcTest(CartController.class)
class CartControllerTest {
    @Autowired MockMvc mockMvc;
    
    @Test
    void shouldGetCart() throws Exception { /* ... */ }
}
```

---

### Level 4: Integration Tests
**Purpose:** Validate complete workflows

**Approach:**
- Full Spring context
- Real all dependencies
- End-to-end scenarios

**Count:** 41 tests

**Example:**
```java
@SpringBootTest
@Transactional
class CartServiceIntegrationTest {
    @Test
    void shouldCompleteShoppingWorkflow() { /* ... */ }
}
```

---

### Level 5: Performance Tests
**Purpose:** Validate performance & scalability

**Approach:**
- Measure response times
- Monitor throughput
- Check resource usage

**Count:** 21 tests

**Example:**
```java
@SpringBootTest
class ProductServicePerformanceTest {
    @Test
    void shouldCreateProductUnder500ms() { /* ... */ }
}
```

---

## Load Testing (JMeter)

### Purpose
Test system behavior under increasing load and sudden spikes.

### 4 Scenarios

**Scenario 1: Light Load**
- 50 concurrent users
- 5 minutes duration
- Category endpoints

**Scenario 2: Medium Load**
- 100 concurrent users
- 10 minutes duration
- Product endpoints

**Scenario 3: Heavy Load**
- 200 concurrent users
- 10 minutes duration
- Complete shopping workflow

**Scenario 4: Spike Test**
- 500 concurrent users
- 2 minutes duration
- Sudden traffic surge

---

## Testing Statistics

### Coverage by Count
```
Repository:  19 tests  (12.3%)  ████████░░░░░░░░░░░
Service:     27 tests  (17.4%)  ███████████░░░░░░░░
Controller:  23 tests  (14.8%)  ██████████░░░░░░░░░
Integration: 41 tests  (26.5%)  ██████████████░░░░░
Performance: 21 tests  (13.5%)  █████████░░░░░░░░░░
Load Tests:  4 scenarios
───────────────────────────────
TOTAL:      155 tests (100%)
```

### Coverage by Feature
```
Category:  18 tests  (11.6%)
Product:   46 tests  (29.7%)
Cart:      83 tests  (53.5%)
```

---

## Testing Approach

### Unit Tests (Repository + Service)
```
Characteristics:
✓ Fast (< 100ms each)
✓ Isolated
✓ Deterministic
✓ No dependencies

Use:
- Development
- Rapid feedback
- Edge case testing
```

### Integration Tests
```
Characteristics:
✓ Full context
✓ Real dependencies
✓ Database operations
✓ Slower than unit tests

Use:
- Before commits
- Feature verification
- Cross-layer testing
```

### Performance Tests
```
Characteristics:
✓ Measure response times
✓ Monitor throughput
✓ Check memory
✓ Load baseline

Use:
- Regression detection
- Performance validation
- Baseline establishment
```

### Load Tests
```
Characteristics:
✓ Concurrent users
✓ Sustained load
✓ Real HTTP
✓ External tool (JMeter)

Use:
- Capacity planning
- Spike scenarios
- Production readiness
```

---

## Execution Order

### Development Cycle
```
1. Write code
   ↓
2. Run unit tests (fast feedback)
   ↓
3. All tests pass? Continue
   ↓
4. Before commit: Run full test suite
   ↓
5. All green? Commit
   ↓
6. CI/CD runs all tests
   ↓
7. Ready for release
```

### Before Release
```
1. Unit tests pass ✓
2. Integration tests pass ✓
3. Performance tests pass ✓
4. Load tests pass ✓
5. No regressions ✓
6. Performance baseline met ✓
7. Ready for production ✓
```

---

## Technology Stack

### Testing Frameworks
- **JUnit 5** - Test execution
- **Mockito** - Mocking
- **AssertJ** - Assertions
- **Spring Test** - Spring integration

### Testing Tools
- **@DataJpaTest** - Repository testing
- **@ExtendWith** - Extension mechanism
- **@WebMvcTest** - Controller testing
- **@SpringBootTest** - Full context testing
- **MockMvc** - HTTP testing
- **H2** - In-memory database

### Load Testing
- **JMeter** - Load & performance testing
- **HTML Reports** - Result visualization

---

## Test Isolation Strategy

### Repository Tests
```
Each test:
✓ Gets fresh H2 database
✓ Clean state
✓ Automatic rollback
✓ Independent execution
```

### Service Tests
```
Each test:
✓ Mock dependencies
✓ No side effects
✓ Isolated setup
✓ Independent assertions
```

### Controller Tests
```
Each test:
✓ MockMvc (no real server)
✓ Mock services
✓ Isolated requests
✓ Independent responses
```

### Integration Tests
```
Each test:
✓ @Transactional (auto-rollback)
✓ Unique data (timestamps)
✓ Isolated context
✓ Clean state
```

---

## Performance Targets

```
Repository Layer:
  Response: < 50ms
  Error: 0%

Service Layer:
  Response: < 200ms
  Error: 0%

Controller Layer:
  Response: < 300ms
  Error: 0%

Integration Layer:
  Response: < 500ms
  Error: < 1%

Performance Layer:
  Response: < 200ms
  Throughput: > 5 ops/sec
  Error: 0%

Load Testing:
  Average: 100-200ms
  95th: < 500ms
  Error: < 1%
```

---

## Continuous Integration

### Automated Testing
```
On every commit:
1. Compile code
2. Run unit tests
3. Run integration tests
4. Run performance tests
5. Generate reports
6. Publish results
```

### Scheduled Testing
```
Daily/Weekly:
1. Run full test suite
2. Run load tests
3. Compare performance baseline
4. Alert on regressions
```

---

## Metrics & Monitoring

### What We Measure

**Execution Metrics**
- Total tests run
- Tests passed/failed
- Execution time
- Pass rate %

**Performance Metrics**
- Average response time
- Percentile response times (50th, 95th, 99th)
- Throughput (requests/second)
- Error rate %

**Resource Metrics**
- Memory usage
- CPU usage
- Database connections
- Thread count

---

## Quality Gates

### Before Committing
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] No new failures
- [ ] Code compiles

### Before Merging
- [ ] All tests pass
- [ ] Performance acceptable
- [ ] No regressions
- [ ] Code reviewed

### Before Release
- [ ] 155+ tests pass
- [ ] Load tests pass
- [ ] No known issues
- [ ] Documentation updated
- [ ] Performance baseline met

---

## Best Practices

### ✅ DO
- ✅ Write tests before code (TDD)
- ✅ Keep tests simple and focused
- ✅ Use meaningful test names
- ✅ Test both happy and error paths
- ✅ Mock external dependencies
- ✅ Isolate test data
- ✅ Run tests frequently
- ✅ Maintain baseline metrics

### ❌ DON'T
- ❌ Test implementation details
- ❌ Create test dependencies
- ❌ Use sleep() for timing
- ❌ Leave hardcoded test data
- ❌ Test other teams' code
- ❌ Ignore test failures
- ❌ Skip tests for speed

---

## File Organization

```
src/test/java/com/itc/funkart/product_service/
│
├── repository/                    [Repository Tests]
│   ├── CartRepositoryTest.java    (6 tests)
│   ├── CartItemRepositoryTest.java (5 tests)
│   ├── CategoryRepositoryTest.java (4 tests)
│   ├── ProductRepositoryTest.java (5 tests)
│   └── ProductImageRepositoryTest.java (2 tests)
│
├── serviceImpl/                    [Service Tests]
│   ├── CategoryServiceImplTest.java (7 tests)
│   ├── ProductServiceImplTest.java (10 tests)
│   └── CartServiceImplTest.java (13 tests)
│
├── controller/                    [Controller Tests]
│   ├── CategoryControllerTest.java (7 tests)
│   ├── ProductControllerTest.java (9 tests)
│   └── CartControllerTest.java (11 tests)
│
├── integration/                   [Integration Tests]
│   ├── CategoryServiceIntegrationTest.java (8 tests)
│   ├── ProductServiceIntegrationTest.java (12 tests)
│   ├── CartServiceIntegrationTest.java (13 tests)
│   └── CrossFeatureIntegrationTest.java (8 tests)
│
└── performance/                   [Performance Tests]
    ├── ProductServicePerformanceTest.java (11 tests)
    └── CartServicePerformanceTest.java (10 tests)
```

---

## Summary

| Aspect | Value |
|--------|-------|
| Total Tests | 155 |
| Test Layers | 5 |
| Load Scenarios | 4 |
| Test Execution | ~5 minutes |
| Load Test Duration | ~32 minutes |
| Pass Rate Target | 100% |
| Performance Target | < 200ms avg |
| Error Rate Target | < 1% |

---

## Next Steps

1. **Understand Testing Approach** ← You are here
2. **Read [TESTING_GUIDE.md](TESTING_GUIDE.md)** - Detailed test documentation
3. **Read [QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Commands reference
4. **Run Tests** - Execute: `./gradlew.bat test`
5. **Review Results** - Open: `build/reports/tests/test/index.html`

---

**Ready to test!** 🚀

