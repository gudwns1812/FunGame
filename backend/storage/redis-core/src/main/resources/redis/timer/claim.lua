local due = KEYS[1]
local now, leaseUntil, limit = ARGV[1], ARGV[2], tonumber(ARGV[3])

local entries = redis.call('ZRANGEBYSCORE', due, '-inf', now, 'WITHSCORES', 'LIMIT', 0, limit)
for i = 1, #entries, 2 do
    redis.call('ZADD', due, leaseUntil, entries[i])
end

return entries
