local online, connections = KEYS[1], KEYS[2]
local member, connection, now, graceUntil = ARGV[1], ARGV[2], ARGV[3], ARGV[4]

redis.call('ZREM', connections, connection)

if redis.call('ZCOUNT', connections, '(' .. now, '+inf') == 0 then
    redis.call('ZADD', online, 'LT', graceUntil, member)
end

return 0
