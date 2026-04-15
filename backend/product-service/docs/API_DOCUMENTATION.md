# Springdoc-OpenAPI Swagger UI Documentation

## Overview

This document explains how to access and use the interactive API documentation for the Funkart Product Service using **Springdoc-OpenAPI with Swagger UI 3.0**.

## Quick Start

### 1. Access Swagger UI
Once the application is running, open your browser and navigate to:
```
http://localhost:9090/swagger-ui.html
```

### 2. View OpenAPI Specification
The raw OpenAPI specification in JSON format is available at:
```
http://localhost:9090/v3/api-docs
```

The YAML version is available at:
```
http://localhost:9090/v3/api-docs.yaml
```

## Features

### Interactive Documentation
✅ **Browse APIs** - Organized by tags (Products, Categories, Shopping Cart)
✅ **Try It Out** - Test endpoints directly from the UI
✅ **Request/Response Examples** - See actual request and response formats
✅ **Parameter Documentation** - Understand each parameter with descriptions and examples
✅ **Error Codes** - See possible error responses and their meanings

### Swagger UI Features
- **Search** - Search for specific endpoints
- **Authorize** - Configure API security (if applicable)
- **Models** - View data structure schemas
- **Operations Sorter** - Sort endpoints by method (GET, POST, etc.)
- **Tags Sorter** - Alphabetical ordering of API groups

## API Endpoints

### Products API (`/api/products`)

#### 1. Get All Products
```
GET /api/products
```
**Description:** Retrieve all available products

**Response:** 200 OK
```json
[
  {
    "id": 1,
    "name": "Product Name",
    "description": "Product Description",
    "price": 99.99,
    "stock": 100,
    "categoryId": 1,
    "active": true,
    "images": []
  }
]
```

#### 2. Get Product by ID
```
GET /api/products/{id}
```
**Description:** Get detailed information about a specific product

**Parameters:**
- `id` (path, required): Product ID (e.g., 1)

**Response:** 200 OK
```json
{
  "id": 1,
  "name": "Product Name",
  "description": "Product Description",
  "price": 99.99,
  "stock": 100,
  "categoryId": 1,
  "active": true,
  "images": [
    {
      "id": 1,
      "url": "image-url",
      "isPrimary": true
    }
  ]
}
```

**Error Responses:**
- `404 Not Found` - Product doesn't exist

#### 3. Get Products by Multiple IDs
```
POST /api/products/by-ids
```
**Description:** Batch retrieve products by providing a list of IDs

**Request Body:**
```json
[1, 2, 3, 4, 5]
```

**Response:** 200 OK
```json
{
  "products": [
    {
      "id": 1,
      "name": "Product Name",
      "price": 99.99,
      ...
    }
  ],
  "totalCount": 5
}
```

---

### Categories API (`/api/categories`)

#### 1. Get All Categories
```
GET /api/categories
```
**Description:** Retrieve all product categories

**Response:** 200 OK
```json
[
  {
    "id": 1,
    "name": "Electronics",
    "description": "Electronic devices and gadgets"
  },
  {
    "id": 2,
    "name": "Clothing",
    "description": "Apparel and fashion items"
  }
]
```

#### 2. Get Category by ID
```
GET /api/categories/{id}
```
**Description:** Get detailed information about a specific category

**Parameters:**
- `id` (path, required): Category ID (e.g., 1)

**Response:** 200 OK
```json
{
  "id": 1,
  "name": "Electronics",
  "description": "Electronic devices and gadgets",
  "productCount": 45
}
```

---

### Shopping Cart API (`/api/cart`)

#### 1. Get User's Cart
```
GET /api/cart/{userId}
```
**Description:** Retrieve the user's shopping cart with all items

**Parameters:**
- `userId` (path, required): User ID (e.g., 1)

**Response:** 200 OK
```json
{
  "id": 1,
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "productName": "Product Name",
      "price": 99.99,
      "quantity": 2,
      "subtotal": 199.98
    }
  ],
  "totalPrice": 199.98,
  "itemCount": 2,
  "createdAt": "2026-04-13T10:00:00Z",
  "updatedAt": "2026-04-13T10:30:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Cart doesn't exist for user

#### 2. Add Item to Cart
```
POST /api/cart/{userId}/items
```
**Description:** Add a product to the user's shopping cart

**Parameters:**
- `userId` (path, required): User ID

**Request Body:**
```json
{
  "productId": 5,
  "quantity": 2
}
```

**Response:** 200 OK - Returns updated cart

**Error Responses:**
- `400 Bad Request` - Invalid product ID or quantity
- `404 Not Found` - Product or user not found

#### 3. Remove Item from Cart
```
DELETE /api/cart/{userId}/items/{productId}
```
**Description:** Remove a product from the user's shopping cart

**Parameters:**
- `userId` (path, required): User ID
- `productId` (path, required): Product ID to remove

**Response:** 200 OK - Returns updated cart

**Error Responses:**
- `404 Not Found` - Product not in cart

#### 4. Update Item Quantity
```
PATCH /api/cart/{userId}/items/{productId}
```
**Description:** Update the quantity of a product in the cart

**Parameters:**
- `userId` (path, required): User ID
- `productId` (path, required): Product ID

**Request Body:**
```json
{
  "quantity": 5
}
```

**Response:** 200 OK - Returns updated cart

**Error Responses:**
- `400 Bad Request` - Invalid quantity (must be > 0)
- `404 Not Found` - Product not in cart

#### 5. Checkout
```
POST /api/cart/{userId}/checkout
```
**Description:** Process the order and clear the cart

**Parameters:**
- `userId` (path, required): User ID

**Response:** 200 OK
```json
"Order processed and cart cleared"
```

**Side Effects:**
- Order event published to Kafka
- Cart cleared for user

**Error Responses:**
- `400 Bad Request` - Cart is empty
- `404 Not Found` - User or cart not found

---

## Testing in Swagger UI

### Step 1: Locate Endpoint
Browse through the API sections (Products, Categories, Shopping Cart) or search for the endpoint.

### Step 2: View Documentation
Click on the endpoint to expand it. You'll see:
- Full description
- Parameters with examples
- Expected responses
- Error codes

### Step 3: Try It Out
Click the **"Try it out"** button to enable the request editor.

### Step 4: Fill Parameters
- Enter values for path parameters
- Enter request body if required
- Click "Execute"

### Step 4: View Response
- **Response Body** - The actual response from the server
- **Response Code** - HTTP status code (200, 400, 404, etc.)
- **Response Headers** - HTTP headers from the response

## Common Use Cases

### Get All Products and Display
```bash
curl -X GET "http://localhost:9090/api/products"
```

### Get Specific Product Details
```bash
curl -X GET "http://localhost:9090/api/products/1"
```

### Create Cart and Add Items
```bash
# Get user's cart
curl -X GET "http://localhost:9090/api/cart/1"

# Add item to cart
curl -X POST "http://localhost:9090/api/cart/1/items" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 5,
    "quantity": 2
  }'
```

### Checkout
```bash
curl -X POST "http://localhost:9090/api/cart/1/checkout"
```

## Configuration

### Swagger UI Settings
The following settings are configured in `application.yml`:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method          # Sort by HTTP method
    tags-sorter: alpha                 # Sort tags alphabetically
    display-request-duration: true     # Show request duration
    default-models-expand-depth: 2     # Model expansion depth
    default-model-expand-depth: 2      # Detail expansion depth
    show-extensions: true              # Show extensions
```

### Customizing OpenAPI Info
Edit `OpenAPIConfig.java` to customize:
- API title, version, description
- Contact information
- License details
- Server configurations

## Exporting Documentation

### Export as JSON
```
GET http://localhost:9090/v3/api-docs
```

### Export as YAML
```
GET http://localhost:9090/v3/api-docs.yaml
```

### Use with Third-Party Tools
- **Postman**: Import from OpenAPI URL
- **ReDoc**: Add to your documentation site
- **CI/CD**: Validate API compliance

## Troubleshooting

### Swagger UI Not Loading
- Ensure application is running on port 9090
- Check browser console for errors
- Clear browser cache and reload

### Endpoints Not Showing
- Verify controller classes have `@RestController` and `@RequestMapping`
- Check for typos in URL patterns
- Restart the application

### Annotations Not Working
- Ensure Springdoc dependency is in build.gradle
- Verify imports are from `io.swagger.v3.oas.annotations.*`
- Check controller class is in Spring component scan path

## Best Practices

✅ **Always provide descriptions** - Explain what each endpoint does
✅ **Document parameters** - Include examples and constraints
✅ **Show examples** - Provide sample request/response bodies
✅ **List error codes** - Document all possible error responses
✅ **Keep it updated** - Annotations stay in sync with code
✅ **Use meaningful names** - Clear endpoint and parameter names
✅ **Tag endpoints** - Organize by functional groups

## References

- **Springdoc-OpenAPI**: https://springdoc.org/
- **OpenAPI Specification**: https://spec.openapis.org/
- **Swagger UI**: https://swagger.io/tools/swagger-ui/
- **Spring Boot Docs**: https://spring.io/projects/spring-boot

---

**API Documentation Auto-Generated**: This documentation is automatically generated from code annotations and stays synchronized with your API as you update the endpoints.
