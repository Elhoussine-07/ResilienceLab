# API documentation

## Product Catalog Service

Base URL (local): `http://localhost:8081/api/v1/products`

| Method | Path | Description | Success | Errors |
|---|---|---|---|---|
| POST | `/api/v1/products` | Create a product | `201` + `Location` header | `400` invalid payload, `409` duplicate SKU |
| GET | `/api/v1/products?page=0&size=20` | List products (paged, default sort by name, max size 100) | `200` | |
| GET | `/api/v1/products/{id}` | Get a product | `200` | `404` |
| PUT | `/api/v1/products/{id}` | Replace a product | `200` | `400`, `404`, `409` |
| DELETE | `/api/v1/products/{id}` | Delete a product | `204` | `404` |

### Request body (POST, PUT)

```json
{
  "sku": "KEYB-001",
  "name": "Mechanical keyboard",
  "description": "Tenkeyless, brown switches",
  "price": 79.90
}
```

| Field | Rules |
|---|---|
| `sku` | required, not blank, max 64 characters, unique |
| `name` | required, not blank, max 255 characters |
| `description` | optional, max 1000 characters |
| `price` | required, >= 0, at most 2 decimals |

### Errors

Errors follow the RFC 9457 Problem Details format. Validation errors add an `errors` map:

```json
{
  "title": "Invalid request",
  "status": 400,
  "detail": "Validation failed",
  "errors": { "name": "must not be blank" }
}
```

RabbitMQ event contracts will be documented here when the messaging layer is implemented.