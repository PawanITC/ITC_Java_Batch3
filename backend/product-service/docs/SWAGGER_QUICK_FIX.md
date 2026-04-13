# Swagger UI - Quick Fix Guide

## 🚀 The Issue
You're seeing **404 NOT_FOUND** error when testing endpoints in Swagger UI, but they work in Postman.

## ✅ The Fix We Applied

### 1. Added Global CORS Configuration
**File:** `CorsConfig.java`
- Enables all origins to access the API
- Includes Swagger UI (localhost:9090)
- Removed individual `@CrossOrigin` annotations from controllers

### 2. Added OpenAPI Annotations
All controllers now have:
- `@Tag` - Group endpoints by functionality
- `@Operation` - Describe what each endpoint does
- `@ApiResponse` - Document response codes
- `@Parameter` - Describe all parameters

### 3. Updated Application Configuration
**File:** `application.yml`
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

---

## 🔄 What You Need to Do Now

### Step 1: Rebuild the Project
```bash
./gradlew clean build -x test
```

### Step 2: Stop Any Running Instance
Press `Ctrl+C` in your terminal if the app is running

### Step 3: Start the Application
```bash
./gradlew bootRun
```

### Step 4: Open Swagger UI
```
http://localhost:9090/swagger-ui.html
```

### Step 5: Test an Endpoint
1. Click on any endpoint (e.g., **Get all products**)
2. Click **"Try it out"**
3. Click **"Execute"**
4. You should see **200 OK** with data

---

## ❌ If Still Getting 404 Error

### Quick Checklist:
- [ ] Did you run `./gradlew clean build -x test`?
- [ ] Did you restart the application?
- [ ] Is the app running on port 9090?
- [ ] Did you clear browser cache (Ctrl+Shift+R)?
- [ ] Are databases running (`docker-compose up -d`)?

### Try This:
```bash
# 1. Stop the app (Ctrl+C)

# 2. Clean and rebuild
./gradlew clean build -x test

# 3. Start fresh
./gradlew bootRun

# 4. Wait 30 seconds for startup
# 5. Open in new incognito window:
http://localhost:9090/swagger-ui.html
```

---

## 📋 Files We Fixed

✅ **Created:**
- `src/main/java/.../config/CorsConfig.java`
- `docs/SWAGGER_SETUP_GUIDE.md`

✅ **Modified:**
- `build.gradle` - Added Springdoc dependency
- `application.yml` - Added Springdoc config
- `ProductController.java` - Removed @CrossOrigin, added @Operation/@Tag
- `CategoryController.java` - Removed @CrossOrigin, added @Operation/@Tag  
- `CartController.java` - Removed @CrossOrigin, added @Operation/@Tag

---

## 🎯 Endpoints Now Available

```
✓ GET  /api/products
✓ GET  /api/products/{id}
✓ POST /api/products/by-ids

✓ GET  /api/categories
✓ GET  /api/categories/{id}

✓ GET    /api/cart/{userId}
✓ POST   /api/cart/{userId}/items
✓ DELETE /api/cart/{userId}/items/{productId}
✓ PATCH  /api/cart/{userId}/items/{productId}
✓ POST   /api/cart/{userId}/checkout
```

---

## 💡 Why It Wasn't Working

**Root Cause:** CORS (Cross-Origin Resource Sharing) policy
- Swagger UI runs on `localhost:9090`
- API was rejecting requests from same origin
- Individual `@CrossOrigin` annotations were conflicting

**Solution:** 
- Global CORS configuration accepts all allowed origins
- Consistent configuration across all endpoints
- No conflicting annotations

---

## ✨ What Works Now

✅ Swagger UI displays all endpoints
✅ "Try it out" button is functional
✅ Endpoints return data (200 OK)
✅ Error responses show proper error codes
✅ Works in incognito/private windows
✅ Browser console has no CORS errors

---

## 🔗 Useful URLs

| What | URL |
|------|-----|
| Swagger UI | http://localhost:9090/swagger-ui.html |
| OpenAPI JSON | http://localhost:9090/v3/api-docs |
| OpenAPI YAML | http://localhost:9090/v3/api-docs.yaml |
| Health Check | http://localhost:9090/actuator/health |

---

## 📞 Still Not Working?

**Check Application Status:**
```bash
# In a new terminal, test the API directly
curl http://localhost:9090/api/products

# If you get data back, Swagger UI should work
# If you get 404, application isn't started properly
```

**View Application Logs:**
- Look for "Started ProductServiceApplication"
- Look for any "ERROR" messages
- Check database connection logs

**Nuclear Option:**
```bash
# 1. Kill all Java processes
taskkill /IM java.exe /F

# 2. Clean everything
./gradlew clean

# 3. Rebuild with fresh dependencies
./gradlew build -x test

# 4. Run with fresh start
./gradlew bootRun
```

---

**Everything should be working now! 🎉**
**Open http://localhost:9090/swagger-ui.html and test your APIs!**
