# Evaluation Service - Production Ready

## 🚀 Развертывание

### 🐳 **Docker Compose (Рекомендуется)**
```bash
# Быстрый запуск всей инфраструктуры
./start.sh start

# Проверить статус
./start.sh status

# Просмотреть логи
./start.sh logs
```

**Доступные сервисы:**
- 🌐 **Evaluation Service**: http://localhost:8081
- 📊 **Grafana**: http://localhost:3000 (admin/admin123)
- 📈 **Prometheus**: http://localhost:9091
- 🔧 **Kafka UI**: http://localhost:8090

Подробная инструкция в [DOCKER_README.md](DOCKER_README.md)

---

### 🏗️ **Локальная разработка**
```bash
# Использовать Java 21
sdk use java 21.0.2-open

# Запустить с dev профилем
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 🚨 ВАЖНО: Проблема компиляции

Текущая ошибка `java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN` связана с конфликтом версий Java 25 и Maven compiler plugin.

### ✅ Решение:

1. **Использовать Java 21:**
   ```bash
   # Если установлен SDKMAN
   sdk use java 21.0.2-open
   
   # Или установить JAVA_HOME вручную
   export JAVA_HOME=/path/to/java21
   ```

2. **Очистить и пересобрать:**
   ```bash
   mvn clean compile
   ```

---

## 📋 Обновления Production Ready

### ✨ **Новые функции:**

#### 1. **Fault Tolerance**
- **Fallback логика** в AIService: при ошибках возвращает "Требуется ручной анализ"
- **Graceful degradation** вместо Resilience4j (упрощено для стабильности)

#### 2. **Kafka Reliability**
- **Dead Letter Topic**: `incoming_tickets_dlt` для неудачных сообщений
- **Error Handling**: Автоматическая отправка в DLT при ошибках
- **Validation**: `@Valid` на входящих DTO

#### 3. **Observability**
- **Prometheus metrics**: `/actuator/prometheus`
- **Custom Counter**: `tickets_distributed_total{region, priority}`
- **Health checks**: `/actuator/health`

#### 4. **Advanced Matching Logic**
- **Только активные менеджеры**: `isActive = true`
- **Fallback алгоритм**: если нет менеджера с навыками → любой свободный
- **Улучшенные запросы**: сортировка и подсчет менеджеров

#### 5. **Validation & Security**
- **@NotBlank**: text и location не могут быть пустыми
- **@Validated**: на уровне сервиса
- **Producer reliability**: `acks=all`, `retries=3`

---

## 🏗️ Архитектура

```
Kafka incoming_tickets → Validation → CompletableFuture
                                    ├── AI Analysis (with fallback)
                                    └── Manager Search (active only)
                                    ↓
                              Matching Algorithm
                                    ↓
                              Redis INCR (load)
                                    ↓
                        Kafka final_distribution + Metrics
```

---

## 🚀 Запуск

### Docker Compose (Production):
```bash
./start.sh start
```

### Dev окружение (localhost):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Prod окружение (2.133.130.153):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 📊 Monitoring

### Метрики:
```bash
curl http://localhost:8081/actuator/prometheus
```

### Health:
```bash
curl http://localhost:8081/actuator/health
```

### Пример метрик:
```
tickets_distributed_total{region="Almaty",priority="HIGH"} 15.0
tickets_distributed_total{region="Astana",priority="MEDIUM"} 8.0
```

---

## 🔧 Конфигурация

### Docker Environment:
- PostgreSQL: postgres:5432
- Redis: redis:6379
- Kafka: kafka:9092

### Dev (localhost):
- PostgreSQL: localhost:1111
- Redis: localhost:2222
- Kafka: localhost:4444

### Prod (2.133.130.153):
- PostgreSQL: 2.133.130.153:1111
- Redis: 2.133.130.153:2222
- Kafka: 2.133.130.153:4444

---

## 🛠️ Управление Docker

### Команды:
```bash
./start.sh start     # Запустить все сервисы
./start.sh stop      # Остановить все сервисы
./start.sh restart   # Перезапустить все сервисы
./start.sh logs      # Просмотреть логи
./start.sh cleanup   # Полная очистка
./start.sh status    # Показать статус
```

### Сервисы:
- **evaluation-service**: Основное приложение
- **postgres**: База данных PostgreSQL
- **redis**: Кэш Redis
- **kafka**: Message broker
- **zookeeper**: Координация Kafka
- **prometheus**: Сбор метрик
- **grafana**: Визуализация метрик
- **kafka-ui**: Веб интерфейс Kafka

---

## 🛠️ Устранение неполадок

### Если не компилируется:
1. Убедитесь что используется Java 21
2. Очистите Maven кэш: `mvn dependency:purge-local-repository`
3. Пересоберите: `mvn clean compile`

### Если Docker не работает:
1. Проверьте Docker: `docker info`
2. Проверьте порты: `netstat -tulpn | grep -E ':(8081|1111|2222|4444)'`
3. Просмотрите логи: `./start.sh logs`

---

## 🎯 Ключевые улучшения

- ✅ **Production Ready**: Fault tolerance, monitoring, validation
- ✅ **Docker Infrastructure**: Полная среда с мониторингом
- ✅ **Graceful Degradation**: Фоллбэки при ошибках
- ✅ **Observability**: Метрики и health checks
- ✅ **Reliability**: DLT и обработка ошибок
- ✅ **Performance**: CompletableFuture + Redis
- ✅ **Security**: Валидация входных данных
- ✅ **Easy Deployment**: One-command setup

**Сервис полностью готов к продакшен развертыванию!** 🚀

---
**Документация:**
- [DOCKER_README.md](DOCKER_README.md) - Подробное Docker руководство
- [application.properties](src/main/resources/application.properties) - Конфигурация
