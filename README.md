The following assumption about the task and the service has been made:

- Do not allow overlap between slots
- Security and user stuff accepted as is by "userId"(mostly to simplify the data model) which in reality usually a completely different service and this service
  makes a call or included in jwt
- Meetings includes names/emails and not relay on user
- No caching or "reservation process" in redis
- I tried to avoid adding funny constraints to a database directly since even tho this is a "toy" project adding hard
  constraints
  like the new postgress feature with no time overlaps or any other sophisticated stuff can really backfire the moment
  business logic changes in an expected way

### How to test

## Start

```sh
docker compose up -d --build  
```

## Reset the database

```sh
docker compose down -v
docker compose up -d --build
```

http://localhost:8080
Swagger is at http://localhost:8080/swagger-ui.html

Create a user

```sh
curl -X POST localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"name": "Alice", "email": "alice@example.com"}'

curl localhost:8080/api/v1/users/1
```

Create slots for a user.

```sh
for slot in \
  '{"startTime": "2026-11-02T09:00:00Z", "durationMinutes": 60, "status": "FREE"}' \
  '{"startTime": "2026-11-02T10:00:00Z", "durationMinutes": 60, "status": "FREE"}' \
  '{"startTime": "2026-11-02T11:00:00Z", "durationMinutes": 30, "status": "BOOKED"}' \
  '{"startTime": "2026-11-02T13:00:00Z", "durationMinutes": 60, "status": "FREE"}'
do
  curl -X POST localhost:8080/api/v1/slots/1 -H 'Content-Type: application/json' -d "$slot"
done
```

List slots in a range filtered by status

```sh
curl 'localhost:8080/api/v1/slots/1?from=2026-11-02T00:00:00Z&to=2026-11-03T00:00:00Z&status=FREE&size=2'
```

Availability for a range:

```sh
curl 'localhost:8080/api/v1/slots/1/availableSlots?from=2026-11-02T08:00:00Z&to=2026-11-02T18:00:00Z'
```

Schedule a meeting on a free slot

```sh
curl -X POST localhost:8080/api/v1/meetings/1 \
  -H 'Content-Type: application/json' \
  -d '{"slotId": 1, "title": "Planning", "description": "Q1 roadmap", "participants": [{"name": "bla", "email": "test@gmail.com"}]}'

curl localhost:8080/api/v1/meetings/1/1
```

Modify/Status change/Delete slot

```sh
curl -X PUT localhost:8080/api/v1/slots/1/4 \
  -H 'Content-Type: application/json' \
  -d '{"startTime": "2026-11-02T15:00:00Z", "durationMinutes": 30}'

curl -X PUT localhost:8080/api/v1/slots/1/3/status \
  -H 'Content-Type: application/json' \
  -d '{"status": "FREE"}'

curl -X DELETE localhost:8080/api/v1/slots/1/2
```
