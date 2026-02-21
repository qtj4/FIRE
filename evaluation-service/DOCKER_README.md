# Evaluation Service - Docker Deployment

## 🐳 Docker Compose Setup

Полная инфраструктура для Evaluation Service с мониторингом и управлением.

---

## 📋 Состав инфраструктуры

### 🗄️ **База данных**
- **PostgreSQL 15**: Основная база данных (порт 1111)
- **Redis 7**: Кэш и счетчики нагрузки (порт 2222)

### 📨 **Мессенджер**
- **Apache Kafka**: Очереди сообщений (порт 4444)
- **Zookeeper**: Координация Kafka
- **Kafka UI**: Веб-интерфейс для Kafka (порт 8090)

### 🚀 **Приложение**
- **Evaluation Service**: Основной сервис (порт 8081)
- **Health Checks**: `/actuator/health`
- **Metrics**: `/actuator/prometheus`

### 📊 **Мониторинг**
- **Prometheus**: Сбор метрик (порт 9091)
- **Grafana**: Визуализация метрик (порт 3000)

---

## 🚀 Быстрый старт

### 1. Запуск всех сервисов
```bash
./start.sh start
```

### 2. Проверка статуса
```bash
./start.sh status
```

### 3. Просмотр логов
```bash
./start.sh logs
```

---

## 🌐 Доступные сервисы

### Приложение
- **Evaluation Service**: http://localhost:8081
- **Health Check**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/prometheus

### Мониторинг
- **Prometheus**: http://localhost:9091
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Kafka UI**: http://localhost:8090

### Базы данных
- **PostgreSQL**: localhost:1111 (postgres/postgres)
- **Redis**: localhost:2222 (password: fire123)
- **Kafka**: localhost:4444

---

## ⚙️ Управление

### Запуск
```bash
./start.sh start
```

### Остановка
```bash
./start.sh stop
```

### Перезапуск
```bash
./start.sh restart
```

### Просмотр логов
```bash
./start.sh logs
```

### Полная очистка
```bash
./start.sh cleanup
```

---

## 📊 Мониторинг

### Grafana Дашборды

1. Откройте Grafana: http://localhost:3000
2. Войдите: admin/admin123
3. Добавьте DataSource Prometheus (http://prometheus:9090)
4. Импортируйте дашборды или создайте свои

### Ключевые метрики

- `tickets_distributed_total{region, priority}`
- `jvm_memory_used_bytes`
- `kafka_consumer_records_consumed_total`
- `redis_commands_total`

---

## 🔧 Конфигурация

### Environment Variables

Основные переменные окружения:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=docker

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fire
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Redis
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=fire123

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

### Custom Properties

```yaml
app:
  kafka:
    topics:
      incoming: incoming_tickets
      outgoing: final_distribution
      dlt: incoming_tickets_dlt
  redis:
    load-prefix: manager:load:
  matching:
    fallback-enabled: true
    max-retry-attempts: 3
```

---

## 🏗️ Структура проекта

```
evaluation-service/
├── Dockerfile                  # Сборка приложения
├── docker-compose.yml          # Все сервисы
├── prometheus.yml             # Конфигурация Prometheus
├── init.sql                   # Инициализация БД
├── start.sh                   # Скрипт управления
├── grafana/
│   └── provisioning/          # Конфигурация Grafana
├── src/main/resources/
│   ├── application.properties  # Основные свойства
│   ├── application-docker.yml # Docker профиль
│   ├── application-dev.yml    # Dev профиль
│   └── application-prod.yml   # Prod профиль
└── .dockerignore              # Исключения для Docker
```

---

## 🐛 Траблшутинг

### Если сервисы не запускаются

1. **Проверьте Docker:**
   ```bash
   docker info
   ```

2. **Проверьте порты:**
   ```bash
   netstat -tulpn | grep -E ':(8081|1111|2222|4444|9091|3000|8090)'
   ```

3. **Просмотрите логи:**
   ```bash
   ./start.sh logs
   ```

### Если не работает база данных

1. **Подключитесь к PostgreSQL:**
   ```bash
   docker exec -it evaluation-postgres psql -U postgres -d fire
   ```

2. **Проверьте таблицы:**
   ```sql
   \dt
   SELECT * FROM managers LIMIT 5;
   ```

### Если не работает Redis

1. **Подключитесь к Redis:**
   ```bash
   docker exec -it evaluation-redis redis-cli -a fire123
   ```

2. **Проверьте данные:**
   ```bash
   KEYS manager:load:*
   ```

### Если не работает Kafka

1. **Проверьте топики:**
   ```bash
   docker exec evaluation-kafka kafka-topics --bootstrap-server localhost:9092 --list
   ```

2. **Отправьте тестовое сообщение:**
   ```bash
   docker exec evaluation-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic incoming_tickets
   ```

---

## 🔄 Обновление

### Обновление приложения
```bash
# Пересобрать образ
docker-compose build evaluation-service

# Перезапустить сервис
docker-compose up -d evaluation-service
```

### Обновление всех сервисов
```bash
# Выкачать новые образы
docker-compose pull

# Перезапустить все
docker-compose up -d
```

---

## 📈 Масштабирование

### Масштабирование Evaluation Service
```bash
# Запустить 3 экземпляра
docker-compose up -d --scale evaluation-service=3
```

### Балансировка нагрузки
Для production используйте внешний load balancer (nginx/traefik) перед сервисами.

---

## 🔒 Безопасность

### Изменение паролей

1. **PostgreSQL:** Измените `POSTGRES_PASSWORD` в docker-compose.yml
2. **Redis:** Измените `requirepass` в docker-compose.yml
3. **Grafana:** Измените `GF_SECURITY_ADMIN_PASSWORD` в docker-compose.yml

### Сети

Все сервисы работают в изолированной сети `evaluation-network` (172.20.0.0/16).

---

## 📝 Логи и отладка

### Просмотр логов конкретного сервиса
```bash
# Evaluation Service
docker-compose logs -f evaluation-service

# PostgreSQL
docker-compose logs -f postgres

# Kafka
docker-compose logs -f kafka
```

### Отладка в контейнере
```bash
# Зайти в контейнер
docker exec -it evaluation-service bash

# Просмотреть переменные окружения
docker exec evaluation-service env | grep -E '(SPRING|KAFKA|REDIS)'
```

---

## 🎯 Production рекомендации

1. **Ресурсы:** Минимум 4GB RAM, 2 CPU cores
2. **Хранилище:** Используйте external volumes для данных
3. **Бэкапы:** Настройте бэкапы PostgreSQL
4. **Мониторинг:** Настройте алерты в Prometheus/Grafana
5. **Логи:** Используйте centralized logging (ELK stack)
6. **Безопасность:** Используйте HTTPS, измените пароли

---

**Готово к production развертыванию!** 🚀
