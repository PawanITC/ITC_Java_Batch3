# Swagger UI Setup & Troubleshooting Guide

## ✅ Fixed Issues

We've implemented the following fixes to resolve the Swagger UI 404 error:

### 1. **Global CORS Configuration** ✓
Created `CorsConfig.java` to enable Cross-Origin Resource Sharing (CORS) for:
- Swagger UI (http://localhost:9090)
- Frontend applications (localhost:5173, localhost:3000)
- All API endpoints

### 2. **Removed Conflicting @CrossOrigin Annotations** ✓
Removed individual `@CrossOrigin` annotations from:
- ProductController
- CategoryController
- CartController

This prevents annotation conflicts with the global CORS configuration.

### 3. **Added Comprehensive OpenAPI Annotations** ✓
Documented all endpoints with:
- Detailed descriptions
- Parameter documentation
- Response examples
- Error codes
- Usage examples

---

## 🚀 How to Use Swagger UI Now

### Step 1: Start the Application
```bash
./gradlew bootRun
```

Wait for the message: **"Application started in X seconds"**

### Step 2: Open Swagger UI
Go to: **http://localhost:9090/swagger-ui.html**

### Step 3: Test an Endpoint
1. Click on **"Get all products"** under **Products**
2. Click **"Try it out"**
3. Click **"Execute"**
4. You should see the response with products data

---

## 🔧 Configuration Details

### CORS Settings (CorsConfig.java)
```java
Allowed Origins:
- http://localhost:5173      (Frontend dev)
- http://localhost:3000      (Alternative port)
- http://localhost:9090      (Swagger UI on API)
- http://localhost:8080      (Alternative app port)
- http://localhost:4200      (Angular dev)
- http://funkart.local       (Local domain)
- https://funkart.com        (Production - update with your domain)

Allowed Methods:
- GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD

Allowed Headers:
- All headers (*)

Credentials: 
- Allowed (true)

Preflight Cache:
- 3600 seconds (1 hour)
```

### OpenAPI Configuration (OpenAPIConfig.java)
```yaml
API Docs Path:     /v3/api-docs
Swagger UI Path:   /swagger-ui.html
Operations Sort:   By HTTP method (GET, POST, etc.)
Tags Sort:         Alphabetical
Request Duration:  Displayed
Model Expand:      2 levels deep
Show Extensions:   Yes
```

---

## 📋 Troubleshooting Steps

### Issue 1: Still Getting 404 Error in Swagger UI

**Solution:**
1. **Stop the current application** (if running)
2. **Clean rebuild:**
   ```bash
   ./gradlew clean build -x test
   ```
3. **Restart the application:**
   ```bash
   ./gradlew bootRun
   ```
4. **Clear browser cache:**
   - Hard refresh: `Ctrl + Shift + R` (Windows) or `Cmd + Shift + R` (Mac)
   - Or use incognito/private window
5. **Access Swagger UI again:**
   - Go to: http://localhost:9090/swagger-ui.html

### Issue 2: "Cannot GET /api/products" or "404 Not Found"

**Possible Causes:**
- Application not running
- Port 9090 not available
- Endpoint path is incorrect
- Controller not properly annotated

**Solutions:**
1. **Verify application is running:**
   ```bash
   # Look for console message: "Started ProductServiceApplication"
   ```

2. **Check port availability:**
   ```powershell
   netstat -ano | findstr :9090
   # If occupied, change application.yml: server.port: 9091
   ```

3. **Verify endpoint exists:**
   - Open http://localhost:9090/v3/api-docs
   - Look for your endpoint in the JSON

4. **Check logs for errors:**
   - Look for "ERROR" or "Exception" in application logs

### Issue 3: CORS Errors in Browser Console

**Error Message:**
```
Access to XMLHttpRequest at 'http://localhost:9090/api/products' 
from origin 'http://localhost:9090' has been blocked by CORS policy
```

**Solution:**
1. Verify `CorsConfig.java` exists in `src/main/java/com/itc/funkart/product_service/config/`
2. Check that it has `@Configuration` annotation
3. Ensure no conflicting `@CrossOrigin` annotations on controllers
4. Rebuild and restart application

### Issue 4: Swagger UI Not Loading

**Solution:**
1. Check if Springdoc dependency is in build.gradle:
   ```gradle
   implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
   ```

2. Verify `springdoc` configuration in application.yml:
   ```yaml
   springdoc:
     api-docs:
       path: /v3/api-docs
     swagger-ui:
       path: /swagger-ui.html
       enabled: true
   ```

3. Try accessing raw OpenAPI doc:
   - http://localhost:9090/v3/api-docs
   - Should return JSON

### Issue 5: Endpoints Listed But No Data Shows

**Most Common Cause:** Application or database not initialized

**Solutions:**
1. **Ensure Docker containers are running:**
   ```bash
   docker-compose up -d
   ```

2. **Check PostgreSQL connection:**
   ```bash
   docker ps | grep postgres
   ```

3. **Check application logs for database errors:**
   - Look for "Connection refused" or "database connection failed"

4. **Verify application.yml database configuration:**
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: update  # Auto-creates tables
   ```

---

## ✅ Verification Checklist

- [ ] Application running on port 9090
- [ ] Swagger UI loads at http://localhost:9090/swagger-ui.html
- [ ] Endpoints are listed with descriptions
- [ ] "Try it out" button works
- [ ] Responses return 200 OK with data
- [ ] CORS errors don't appear in browser console
- [ ] Works in both Swagger UI and Postman

---

## 📊 API Endpoints Status

Once application is running, verify these endpoints:

```
GET  http://localhost:9090/api/products              ✓ List all products
GET  http://localhost:9090/api/products/{id}         ✓ Get product by ID
POST http://localhost:9090/api/products/by-ids       ✓ Get multiple products

GET  http://localhost:9090/api/categories            ✓ List all categories
GET  http://localhost:9090/api/categories/{id}       ✓ Get category by ID

GET  http://localhost:9090/api/cart/{userId}         ✓ Get user cart
POST http://localhost:9090/api/cart/{userId}/items   ✓ Add to cart
```

---

## 🔍 Testing Without Swagger UI

If Swagger UI isn't working, you can still test using curl:

```bash
# Get all products
curl http://localhost:9090/api/products

# Get categories
curl http://localhost:9090/api/categories

# Get OpenAPI spec
curl http://localhost:9090/v3/api-docs
```

---

## 📝 Files Modified/Created

**New Files:**
- `src/main/java/com/itc/funkart/product_service/config/CorsConfig.java` - Global CORS configuration
- `src/main/java/com/itc/funkart/product_service/config/OpenAPIConfig.java` - OpenAPI configuration
- `docs/API_DOCUMENTATION.md` - Comprehensive API guide

**Modified Files:**
- `build.gradle` - Added Springdoc dependency
- `application.yml` - Added Springdoc configuration
- `ProductController.java` - Added OpenAPI annotations, removed @CrossOrigin
- `CategoryController.java` - Added OpenAPI annotations, removed @CrossOrigin
- `CartController.java` - Added OpenAPI annotations, removed @CrossOrigin

---

## 🎯 Next Steps

1. **Ensure application is running:**
   ```bash
   ./gradlew bootRun
   ```

2. **Access Swagger UI:**
   - http://localhost:9090/swagger-ui.html

3. **Test endpoints:**
   - Click endpoint → "Try it out" → "Execute"

4. **Check responses:**
   - Should see 200 OK with data

---

## 💡 Tips for Better Experience

- **Search endpoints:** Use the search box at top of Swagger UI
- **Try examples:** Click any endpoint to see detailed documentation
- **Export API:** Download OpenAPI spec for use with other tools
- **Use incognito window:** Helps with browser caching issues
- **Check logs:** Application logs show what's happening

---

## ❓ Still Having Issues?

**Restart Everything:**
```bash
# 1. Stop application (Ctrl+C)
# 2. Clean rebuild
./gradlew clean build -x test

# 3. Start fresh
./gradlew bootRun

# 4. Clear browser cache and reload
```

**Check Logs for Errors:**
- Look for "ERROR" or "Exception" in console output
- Check database connection messages
- Verify all dependencies loaded

**Verify Configuration:**
- CorsConfig.java has `@Configuration` annotation
- OpenAPIConfig.java has `@Bean` method
- application.yml has springdoc settings
- All @CrossOrigin annotations removed from controllers

---

**All configurations are now in place! The application should be fully functional with Swagger UI working seamlessly.**
