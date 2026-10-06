local online, connections = KEYS[1], KEYS[2]
local member, connection, now, leaseUntil, leaseMillis = ARGV[1], ARGV[2], tonumber(ARGV[3]), ARGV[4], ARGV[5]

local onlineUntil = redis.call('ZSCORE', online, member)
local wasOnline = onlineUntil ~= false and tonumber(onlineUntil) > now

redis.call('ZREMRANGEBYSCORE', connections, '-inf', now)
redis.call('ZADD', connections, leaseUntil, connection)
redis.call('PEXPIRE', connections, leaseMillis)
redis.call('ZADD', online, 'GT', leaseUntil, member)

if wasOnline then
    return 1
end
return 0
