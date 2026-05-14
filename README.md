# orchestration-scheduler

Сервис планировщика оркестрации: REST для настроек `orchestration.scheduler`, оперативный кэш и периодический тик (раз в 10 минут), публикация `async-job.collection-batch-begin` в Kafka для `COLLECTION_BATCH`.

## Сборка

1. Поднимите PostgreSQL и примените миграции образом Flyway из compose-репозитория `infra` (тег образа задаётся переменной **`FLYWAY_IMAGE_TAG`**, по умолчанию `latest`):

   ```bash
   cd ../infra
   docker compose up -d postgres ensure-db
   docker compose run --rm flyway migrate
   ```

2. Сгенерируйте jOOQ (при необходимости задайте `JOOQ_DB_URL`, `JOOQ_DB_USER`, `JOOQ_DB_PASSWORD`):

   ```bash
   cd ../orchestration-scheduler
   ./gradlew openApiGenerate generateJooq
   ```

3. Сборка и проверки:

   ```bash
   ./gradlew check
   ```

OpenAPI-модели и jOOQ генерируются в `build/generated/…`; в JaCoCo они исключены из порога покрытия.

## Конфигурация

- `SERVER_PORT` — порт HTTP (в OpenAPI для локали указан `8083`).
- `SPRING_DATASOURCE_*` — подключение к БД `joposcragent`, схема `orchestration`. В URL JDBC для PostgreSQL рекомендуется параметр `stringtype=unspecified`, чтобы строковые параметры jOOQ корректно сопоставлялись с типом-перечислением `orchestration.scheduler_jobs` в колонке `job_type` (см. `application.yaml` и сервис в `infra/docker-compose.yaml`).
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` — брокеры Kafka.
- `scheduler.tick-interval-ms` — интервал тика в миллисекундах (по умолчанию `600000`).

## Спецификация

Источник правды по поведению: репозиторий `specifications`, каталог `services/orchestration-scheduler` и миграции `database-schema/orchestration`.
