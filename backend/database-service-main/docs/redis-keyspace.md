# MapMyTour Redis Keyspace Design

## Instances

| Instance | Port | Purpose | Eviction Policy |
|---|---|---|---|
| redis-prod | 6379 | Sessions, search cache, rate limits | allkeys-lru |
| redis-dev | 6380 | Development | allkeys-lru |
| redis-fares-prod | 6381 | Fare holds (NEVER evict) | noeviction |

## Key Naming Convention

All keys follow: `{service}:{entity}:{identifier}[:{sub}]`

## redis-fares-prod (port 6381) — CRITICAL DATA

| Key Pattern | TTL | Description |
|---|---|---|
| `fare:hold:{booking_ref}` | 1200s (20 min) | Fare hold data as JSON |
| `fare:lock:{flight_id}:{date}:{seat_count}` | 1200s | Distributed lock for inventory |
| `fare:pnr:{gds_pnr}` | 1200s | GDS PNR reference |

Example fare hold value (JSON):
```json
{
  "booking_ref": "MMT-2024-ABC123",
  "user_id": "uuid",
  "flight_ids": ["uuid1", "uuid2"],
  "total_fare": 15420.00,
  "currency": "INR",
  "held_at": "2024-01-15T10:30:00Z",
  "expires_at": "2024-01-15T10:50:00Z"
}
```

## redis-prod (port 6379) — General cache

| Key Pattern | TTL | Description |
|---|---|---|
| `session:{user_id}` | 1800s (30 min) | User session data |
| `session:agent:{agent_id}` | 3600s (1 hour) | B2B agent session |
| `search:cache:{sha256_of_params}` | 300s (5 min) | Search results cache |
| `avail:{flight_id}:{date}` | 120s (2 min) | Seat availability |
| `avail:{hotel_id}:{date}` | 120s (2 min) | Room availability |
| `rate:{from_currency}:{to_currency}` | 3600s (1 hour) | Exchange rate cache |
| `ratelimit:{ip}:{endpoint}` | 60s | API rate limiting (sliding window) |
| `lock:booking:{resource_id}` | 30s | Distributed lock (Redlock pattern) |
| `airport:search:{query_hash}` | 86400s (24h) | Airport autocomplete cache |
| `user:profile:{user_id}` | 900s (15 min) | User profile cache |
| `agent:profile:{agent_id}` | 900s (15 min) | Agent profile cache |

## Distributed Locking Pattern (Lua script)

Use this pattern for concurrent booking prevention. Include in booking-service:

```lua
-- acquire_lock.lua
local key = KEYS[1]
local token = ARGV[1]
local ttl = tonumber(ARGV[2])
local result = redis.call('SET', key, token, 'NX', 'PX', ttl)
if result then return 1 else return 0 end
```

```lua
-- release_lock.lua  
local key = KEYS[1]
local token = ARGV[1]
local current = redis.call('GET', key)
if current == token then
    redis.call('DEL', key)
    return 1
else
    return 0
end
```
