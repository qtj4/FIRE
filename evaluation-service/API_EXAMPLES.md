# Примеры запросов (evaluation-service, порт 8092)

Базовый URL: `http://localhost:8092`

---

## Офисы (Offices)

### Получить все офисы
```bash
curl -s http://localhost:8092/api/v1/offices
```

### Получить офис по id
```bash
curl -s http://localhost:8092/api/v1/offices/1
```

### Создать офис
```bash
curl -s -X POST http://localhost:8092/api/v1/offices \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ALA",
    "name": "Офис Алматы",
    "address": "ул. Абая 150",
    "latitude": 43.238949,
    "longitude": 76.945465
  }'
```
(Поле `code` опционально — при отсутствии генерируется из названия.)

### Обновить офис
```bash
curl -s -X PUT http://localhost:8092/api/v1/offices/1 \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ALA",
    "name": "Офис Алматы Центр",
    "address": "ул. Абая 150",
    "latitude": 43.238949,
    "longitude": 76.945465
  }'
```

### Удалить офис
```bash
curl -s -X DELETE http://localhost:8092/api/v1/offices/1
```

---

## Менеджеры (Managers)

### Получить всех менеджеров
```bash
curl -s http://localhost:8092/api/v1/managers
```

### Получить менеджера по id
```bash
curl -s http://localhost:8092/api/v1/managers/1
```

### Создать менеджера
```bash
curl -s -X POST http://localhost:8092/api/v1/managers \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Иванов Иван Иванович",
    "position": "Старший менеджер",
    "officeName": "Офис Алматы",
    "officeCode": "ALA",
    "skills": "CRM, переговоры",
    "activeTicketsCount": 5
  }'
```
(Поле `officeCode` связывает менеджера с офисом по коду; при назначении обращений используются менеджеры только выбранного офиса.)

### Обновить менеджера
```bash
curl -s -X PUT http://localhost:8092/api/v1/managers/1 \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Иванов Иван Иванович",
    "position": "Ведущий менеджер",
    "officeName": "Офис Алматы",
    "skills": "CRM, переговоры, эскалация",
    "activeTicketsCount": 3
  }'
```

### Удалить менеджера
```bash
curl -s -X DELETE http://localhost:8092/api/v1/managers/1
```

---

## Загрузка из CSV (intake)

### Загрузить офисы из CSV
```bash
curl -s -X POST http://localhost:8092/api/v1/intake/offices \
  -F "file=@offices.csv"
```

Колонки CSV: `Офис`, `Адрес`, `Широта`, `Долгота`

### Загрузить менеджеров из CSV
```bash
curl -s -X POST http://localhost:8092/api/v1/intake/managers \
  -F "file=@managers.csv"
```

Колонки CSV: `ФИО`, `Должность`, `Офис`, `Навыки`, `Количество обращений в работе`---

## Как тестировать после загрузки тикетов (POST /intake/tickets)

Используй свой порт (у тебя 8082; в примерах ниже — `BASE=http://localhost:8082`).

### 1. Проверить, что тикеты в системе

```bash
# Список всех enriched-тикетов
curl -s http://localhost:8082/api/v1/intake/tickets

# Один тикет по id (id смотри в ответе списка или в БД)
curl -s http://localhost:8082/api/v1/intake/tickets/1
```

### 2. Убедиться, что есть офисы и менеджеры

Назначение тикета (assign) выбирает офис по гео/коду и менеджера из этого офиса. Без офисов и менеджеров assign вернёт `UNASSIGNED`.

```bash
curl -s http://localhost:8082/api/v1/offices
curl -s http://localhost:8082/api/v1/managers
```

Если пусто — создай офис и менеджера (см. разделы выше) или загрузи из CSV (`/intake/offices`, `/intake/managers`).

### 3. Назначить тикет менеджеру

Три варианта:

**По id тикета** (id из `GET /intake/tickets` или БД):

```bash
curl -s -X POST http://localhost:8082/api/v1/intake/tickets/1/assign
```

**По clientGuid тикета** (UUID клиента из тикета):

```bash
curl -s -X POST "http://localhost:8082/api/v1/intake/tickets/by-client/550e8400-e29b-41d4-a716-446655440000/assign"
```
(подставь свой `clientGuid` из тикета.)

В ответе будет что-то вроде:
- `enrichedTicketId`, `assignedManagerId`, `assignedManagerName`, `assignedOfficeId`, `assignedOfficeCode`, `assignedOfficeName`, `status` (`ASSIGNED` / `UNASSIGNED`).

### 4. Проверить тикет после назначения

У того же тикета появятся `assignedOfficeId`, `assignedManagerId` и т.д.:

```bash
curl -s http://localhost:8082/api/v1/intake/tickets/1
```

Кратко: **загрузил тикеты → проверил GET /intake/tickets → добавил офисы/менеджеров → дергаешь assign по id или по clientGuid → снова смотришь тикет**.
