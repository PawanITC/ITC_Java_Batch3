# Quick Reference: Testing Guide

## Run Tests

### All Tests
```bash
cd backend/product-service
./gradlew.bat test
```

### Specific Layer
```bash
# Repository tests only
./gradlew.bat test --tests "com.itc.funkart.product_service.repository.*"

# Service tests only
./gradlew.bat test --tests "com.itc.funkart.product_service.serviceImpl.*"

# Controller tests only
./gradlew.bat test --tests "com.itc.funkart.product_service.controller.*"
```

### Specific Feature
```bash
# Category tests
./gradlew.bat test --tests "*Category*"

# Product tests
./gradlew.bat test --tests "*Product*"

# Cart tests
./gradlew.bat test --tests "*Cart*"
```

### Specific Test Class
```bash
./gradlew.bat test --tests "com.itc.funkart.product_service.controller.CartControllerTest"
```

### Specific Test Method
```bash
./gradlew.bat test --tests "CartControllerTest.shouldAddItemToCartSuccessfully"
```

---

## View Test Results

### Browser View
```
Open: build/reports/tests/test/index.html
```

### Terminal View
```bash
# Verbose output
./gradlew.bat test --info

# With stack traces
./gradlew.bat test --stacktrace
```

---

## Test File Locations

### Repository Tests
```
src/test/java/com/itc/funkart/product_service/repository/
├── CartRepositoryTest.java (6 tests)
├── CartItemRepositoryTest.java (5 tests)
├── CategoryRepositoryTest.java (4 tests)
├── ProductRepositoryTest.java (5 tests)
└── ProductImageRepositoryTest.java (2 tests)
```

### Service Tests
```
src/test/java/com/itc/funkart/product_service/serviceImpl/
├── CategoryServiceImplTest.java (7 tests)
├── ProductServiceImplTest.java (10 tests)
└── CartServiceImplTest.java (13 tests)
```

### Controller Tests
```
src/test/java/com/itc/funkart/product_service/controller/
├── CategoryControllerTest.java (7 tests)
├── ProductControllerTest.java (9 tests)
└── CartControllerTest.java (11 tests)
```

---

## Test Patterns Used

### Repository Test Pattern
```java
@DataJpaTest
@ActiveProfiles("test")
class SampleRepositoryTest {
    @Autowired private SampleRepository repo;
    
    @Test
    void shouldSaveEntity() {
        // Arrange
        Sample sample = Sample.builder().name("test").build();
        
        // Act
        Sample saved = repo.save(sample);
        
        // Assert
        assertThat(saved.getId()).isNotNull();
    }
}
```

### Service Test Pattern
```java
@ExtendWith(MockitoExtension.class)
class SampleServiceTest {
    @Mock private SampleRepository repo;
    @InjectMocks private SampleService service;
    
    @Test
    void shouldReturnValue() {
        // Arrange
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        
        // Act
        Result result = service.getById(1L);
        
        // Assert
        assertThat(result).isNotNull();
        verify(repo, times(1)).findById(1L);
    }
}
```

### Controller Test Pattern
```java
@WebMvcTest(SampleController.class)
class SampleControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private SampleService service;
    
    @Test
    void shouldReturnOk() throws Exception {
        // Arrange
        when(service.getAll()).thenReturn(List.of(sample));
        
        // Act & Assert
        mockMvc.perform(get("/api/samples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }
}
```

---

## Common Assertions

### AssertJ
```java
// General
assertThat(value).isNotNull();
assertThat(value).isEqualTo(expected);
assertThat(list).hasSize(3);
assertThat(string).contains("text");

// Collections
assertThat(list).isEmpty();
assertThat(list).isNotEmpty();
assertThat(list).containsExactly(item1, item2);

// Numbers
assertThat(number).isPositive();
assertThat(number).isBetween(1, 10);

// Exceptions
assertThatThrownBy(() -> service.delete(999))
    .isInstanceOf(ResourceNotFoundException.class);
```

### Mockito
```java
// Setup
when(repo.findById(1L)).thenReturn(Optional.of(entity));
doNothing().when(repo).delete(entity);
doThrow(exception).when(repo).save(entity);

// Verification
verify(repo, times(1)).findById(1L);
verify(repo, never()).delete(any());
verify(repo, atLeastOnce()).save(any());
```

### MockMvc
```java
// Requests
mockMvc.perform(get("/api/items"))
mockMvc.perform(post("/api/items").content(json))
mockMvc.perform(put("/api/items/1").content(json))
mockMvc.perform(delete("/api/items/1"))

// Responses
.andExpect(status().isOk())
.andExpect(jsonPath("$.id").value(1L))
.andExpect(content().contentType("application/json"))
```

---

## Troubleshooting

### Test Fails: "Column not found"
**Solution:** Ensure @DataJpaTest uses test database (H2)
```java
@ActiveProfiles("test")
```

### Test Fails: "No beans found"
**Solution:** For @WebMvcTest, mock all dependencies
```java
@MockBean
private ServiceClass service;
```

### Test Hangs
**Solution:** Check for infinite loops or missing mocking
```bash
./gradlew.bat test --info --stack-trace
```

### Port Already in Use
**Solution:** Use random ports for controller tests
```java
@WebMvcTest // Uses random port automatically
```

---

## Best Practices

### ✅ DO
- Use descriptive test names
- Test one concept per test
- Mock external dependencies
- Use test profiles for isolation
- Follow Arrange-Act-Assert pattern
- Test both success and failure cases

### ❌ DON'T
- Test implementation details
- Create dependencies between tests
- Use sleep() for timing
- Test multiple concerns in one test
- Ignore exceptions
- Leave hardcoded test data

---

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      - run: ./gradlew test
```

### Jenkins Example
```groovy
pipeline {
    stages {
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
    }
    post {
        always {
            junit 'build/test-results/**/*.xml'
        }
    }
}
```

---

## Coverage Reports

### Generate with JaCoCo
```bash
./gradlew test jacocoTestReport
```

### View Report
```
build/reports/jacoco/test/html/index.html
```

### Minimum Coverage
Add to build.gradle:
```gradle
jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = 'CLASS'
            excludes = ['**/dto/**']
            includes = ['com/itc/funkart/**']
            
            limit {
                minimum = 0.80
            }
        }
    }
}
```

---

## Useful Gradle Commands

```bash
# Clean and test
./gradlew clean test

# Test with coverage
./gradlew test jacocoTestReport

# Test specific pattern
./gradlew test --tests "*Cart*"

# Verbose output
./gradlew test --info

# Stack trace on failure
./gradlew test --stacktrace

# Continue on failure
./gradlew test --continue

# Force rerun (no cache)
./gradlew cleanTest test
```

---

## Documentation Files

- **TEST_COVERAGE_REPORT.md** - Comprehensive test documentation
- **TEST_FILES_INDEX.md** - Index of all test files
- **QUICK_REFERENCE.md** - This file
- **README.md** - Project documentation

---

## Support

For questions or issues:
1. Check test logs: `build/test-results/`
2. View HTML report: `build/reports/tests/test/index.html`
3. Review test code comments
4. Check test method names for behavior expectations

