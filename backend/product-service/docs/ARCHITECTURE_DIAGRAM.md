# API Documentation Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT APPLICATIONS                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────────┐    ┌──────────────────┐    ┌────────────────┐ │
│  │  Swagger UI      │    │   Postman        │    │   Frontend     │ │
│  │  (Browser)       │    │   (Desktop)      │    │   (React/Vue)  │ │
│  │                  │    │                  │    │                │ │
│  │ localhost:9090   │    │ Any Tool         │    │ localhost:5173 │ │
│  └────────┬─────────┘    └────────┬─────────┘    └────────┬────────┘ │
│           │                       │                       │           │
└───────────┼───────────────────────┼───────────────────────┼───────────┘
            │                       │                       │
            │        CORS           │        CORS           │
            │     Configuration     │     Configuration     │
            └───────────┬───────────┴───────────┬──────────┘
                        │ HTTP Requests         │
                        ▼                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT APPLICATION                            │
│                    (Product Service)                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              CORS Configuration (CorsConfig.java)              │ │
│  │  ✓ Allows requests from all registered origins                │ │
│  │  ✓ Supports GET, POST, PUT, DELETE, PATCH, OPTIONS           │ │
│  │  ✓ Allows all headers and credentials                        │ │
│  │  ✓ 3600 second preflight cache                               │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                              ▲                                        │
│                              │                                        │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │            API Controllers with Documentation                   │ │
│  │  (ProductController, CategoryController, CartController)       │ │
│  │                                                                 │ │
│  │  ┌──────────────────┬──────────────────┬──────────────────┐  │ │
│  │  │     Products     │    Categories    │  Shopping Cart   │  │ │
│  │  │   Management     │   Management     │   Management     │  │ │
│  │  │                  │                  │                  │  │ │
│  │  │  ✓ GET /all     │  ✓ GET /all     │  ✓ GET cart     │  │ │
│  │  │  ✓ GET /id      │  ✓ GET /id      │  ✓ POST item    │  │ │
│  │  │  ✓ POST /batch  │  ✓ Descriptions │  ✓ DELETE item  │  │ │
│  │  │  ✓ Documented   │  ✓ Examples     │  ✓ PATCH qty    │  │ │
│  │  │  ✓ Examples     │  ✓ Error codes  │  ✓ Checkout     │  │ │
│  │  └──────────────────┴──────────────────┴──────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │         OpenAPI Configuration (OpenAPIConfig.java)             │ │
│  │  ✓ API title, version, description                           │ │
│  │  ✓ Contact information                                        │ │
│  │  ✓ License details                                           │ │
│  │  ✓ Server configurations (dev, prod)                        │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │          Springdoc-OpenAPI (Swagger UI 3.0)                    │ │
│  │  ✓ Interactive documentation generation                       │ │
│  │  ✓ Auto-scans controllers for annotations                    │ │
│  │  ✓ Generates OpenAPI specification (JSON/YAML)              │ │
│  │  ✓ Provides Swagger UI at /swagger-ui.html                  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                       │
└───────────────┬──────────────────────┬──────────────────────────────┘
                │                      │
            HTTP Response           OpenAPI Spec
         (200 OK, Error)      (JSON, YAML, etc.)
                │                      │
┌───────────────┼──────────────────────┼──────────────────────────────┐
│               │                      │                              │
│  ┌────────────▼────────────┐  ┌────────────▼────────────┐          │
│  │  Database Access        │  │  Other Tools           │          │
│  │                         │  │  (ReDoc, Postman, etc.)│          │
│  │  ✓ PostgreSQL (Products,│  │                        │          │
│  │    Categories, Cart)    │  │  Can import OpenAPI    │          │
│  │  ✓ MongoDB (Metadata)   │  │  specification and     │          │
│  │  ✓ Redis (Caching)      │  │  generate client code  │          │
│  │  ✓ Kafka (Events)       │  │  or additional docs    │          │
│  │                         │  │                        │          │
│  └─────────────────────────┘  └────────────────────────┘          │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Request/Response Flow

```
1. CLIENT REQUEST
   ┌─────────────────────────────────────────┐
   │  GET http://localhost:9090/api/products │
   │  Accept: application/json               │
   │  Origin: http://localhost:9090          │
   └──────────────────┬──────────────────────┘
                      │
                      ▼
2. CORS PREFLIGHT (if needed)
   ┌─────────────────────────────────────────┐
   │  OPTIONS /api/products                  │
   │  (Browser checks if origin is allowed)  │
   └──────────────────┬──────────────────────┘
                      │
                      ▼
3. CORS FILTER (CorsConfig.java)
   ┌─────────────────────────────────────────┐
   │  ✓ Check if origin in allowed list      │
   │  ✓ Check if method is allowed           │
   │  ✓ Check if headers are allowed         │
   │  ✓ Add CORS headers to response         │
   └──────────────────┬──────────────────────┘
                      │
                      ▼
4. CONTROLLER MAPPING
   ┌─────────────────────────────────────────┐
   │  @GetMapping                            │
   │  public ResponseEntity<List<...>>       │
   │    getAllProducts()                     │
   └──────────────────┬──────────────────────┘
                      │
                      ▼
5. SERVICE LAYER
   ┌─────────────────────────────────────────┐
   │  ProductService.getAllProducts()        │
   │  - Business logic                       │
   │  - Data validation                      │
   │  - Caching                              │
   └──────────────────┬──────────────────────┘
                      │
                      ▼
6. REPOSITORY LAYER
   ┌─────────────────────────────────────────┐
   │  ProductRepository.findAll()            │
   │  - Database query                       │
   │  - Returns entities                     │
   └──────────────────┬──────────────────────┘
                      │
                      ▼
7. DATABASE
   ┌─────────────────────────────────────────┐
   │  PostgreSQL / MongoDB                   │
   │  Returns data                           │
   └──────────────────┬──────────────────────┘
                      │
                      ▼
8. RESPONSE BUILDING
   ┌─────────────────────────────────────────┐
   │  Build ProductResponse objects          │
   │  Convert to JSON                        │
   │  Set HTTP headers                       │
   └──────────────────┬──────────────────────┘
                      │
                      ▼
9. CORS RESPONSE HEADERS
   ┌─────────────────────────────────────────┐
   │  Access-Control-Allow-Origin: *         │
   │  Access-Control-Allow-Methods: *        │
   │  Access-Control-Allow-Headers: *        │
   └──────────────────┬──────────────────────┘
                      │
                      ▼
10. CLIENT RECEIVES
   ┌─────────────────────────────────────────┐
   │  HTTP 200 OK                            │
   │  Content-Type: application/json         │
   │  Body: [...]                            │
   │  CORS headers allow access              │
   └─────────────────────────────────────────┘
```

## API Documentation Generation Flow

```
                CODE ANNOTATIONS
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
    @Tag           @Operation        @ApiResponse
    (Group)      (Endpoint Info)    (Response Info)
        │               │               │
        └───────────────┼───────────────┘
                        │
                        ▼
        ┌─────────────────────────────────┐
        │  Springdoc-OpenAPI Scanner      │
        │  - Scans controllers at startup │
        │  - Extracts annotations         │
        │  - Builds OpenAPI model         │
        └────────────────┬────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
    /v3/api-docs    /v3/api-docs.yaml  Swagger UI
    (JSON)          (YAML)              (/swagger-ui.html)
        │                │                │
        ▼                ▼                ▼
  Raw Spec         Raw Spec        Interactive UI
  (for tools)     (for tools)      (for humans)
        │                │                │
        ▼                ▼                ▼
  Postman         ReDoc Tools      Browser Testing
  Code Gen        Integration      "Try It Out"
  Integration                      Features
```

## Documentation Layers

```
┌──────────────────────────────────────────────────────────────┐
│                    USER/DEVELOPER                             │
│            (Uses Swagger UI in Browser)                       │
└─────────────────────────┬──────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────────┐
│            SWAGGER UI (Interactive)                           │
│  - Browse endpoints                                           │
│  - View parameters & examples                                │
│  - Try It Out feature                                        │
│  - View responses                                            │
└─────────────────────────┬──────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────────┐
│         SPRINGDOC-OPENAPI (Auto-Generator)                   │
│  - Scans @Tag, @Operation, @ApiResponse                     │
│  - Builds OpenAPI specification                             │
│  - Generates JSON/YAML specs                                │
└─────────────────────────┬──────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────────┐
│           CONTROLLER ANNOTATIONS                              │
│  - @Tag (groups endpoints)                                   │
│  - @Operation (describes endpoint)                          │
│  - @Parameter (documents params)                           │
│  - @ApiResponse (documents responses)                       │
│  - @ApiResponses (multiple responses)                       │
└─────────────────────────┬──────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────────┐
│            ACTUAL API CODE                                    │
│  - Controllers (routing)                                     │
│  - Services (business logic)                                │
│  - Repositories (data access)                              │
│  - Entities (data models)                                  │
└──────────────────────────────────────────────────────────────┘
```

## Key Components

### 1. CorsConfig.java
```
Purpose: Allow cross-origin requests
Location: config/CorsConfig.java
Function: Implements WebMvcConfigurer
Methods: addCorsMappings()
Registers: Global CORS configuration
```

### 2. OpenAPIConfig.java
```
Purpose: Configure OpenAPI/Swagger UI
Location: config/OpenAPIConfig.java
Function: Provides OpenAPI bean
Methods: customOpenAPI()
Returns: Configured OpenAPI object
```

### 3. Controllers with Annotations
```
Purpose: Provide detailed API documentation
Location: controller/*.java
Annotations:
  - @RestController: Mark as REST API
  - @RequestMapping: Define base path
  - @Tag: Group endpoints
  - @Operation: Document endpoint
  - @Parameter: Document parameter
  - @ApiResponse: Document response
  - @ApiResponses: Multiple responses
```

### 4. Application Configuration
```
Purpose: Configure Swagger UI settings
Location: application.yml
Settings:
  - api-docs path
  - swagger-ui path
  - sort orders
  - expand depths
```

---

**This architecture ensures:**
- ✅ Clean separation of concerns
- ✅ CORS properly configured globally
- ✅ Auto-generated, always-updated documentation
- ✅ Interactive testing capabilities
- ✅ Professional API documentation
- ✅ Easy team collaboration
