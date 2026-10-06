local online, connections = KEYS[1], KEYS[2]
local member, leaseUntil, leaseMillis = ARGV[1], ARGV[2], ARGV[3]

for i = 4, #ARGV do
    redis.call('ZADD', connections, leaseUntil, ARGV[i])
end
redis.call('PEXPIRE', connections, leaseMillis)
redis.call('ZADD', online, 'GT', leaseUntil, member)

return 0
