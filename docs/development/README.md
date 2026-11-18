# Development guide

## Local infrastructure (PostgreSQL + RabbitMQ)

### 1. Create your local configuration

```bash
cp .env.example .env      # PowerShell: Copy-Item .env.example .env
```

Fill in every value in `.env` (letters and digits only). This file is ignored by Git.

### 2. Start the infrastructure

From the repository root:

```bash
docker compose --env-file .env -f infra/docker/docker-compose.yml up -d
docker compose --env-file .env -f infra/docker/docker-compose.yml ps
```

Both services should become `healthy`.

### 3. Endpoints

| Component              | Address (from the host) |
| ---------------------- | ----------------------- |
| PostgreSQL             | `localhost:5434`        |
| RabbitMQ (AMQP)        | `localhost:5672`        |
| RabbitMQ management UI | http://localhost:15672  |

### Databases

| Database           | Owner                |
| ------------------ | -------------------- |
| `catalog_db`       | `catalog_user`       |
| `orders_db`        | `order_user`         |
| `inventory_db`     | `inventory_user`     |
| `payment_db`       | `payment_user`       |
| `shipping_db`      | `shipping_user`      |
| `resiliencelab_db` | `resiliencelab_user` |

Each user can connect only to its own database.

### Stop and reset

```bash
# stop, keep data
docker compose --env-file .env -f infra/docker/docker-compose.yml down

# stop and DELETE all data (init scripts run again on next start)
docker compose --env-file .env -f infra/docker/docker-compose.yml down -v
```
