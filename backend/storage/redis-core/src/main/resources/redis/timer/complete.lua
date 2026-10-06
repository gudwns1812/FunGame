local due = KEYS[1]
local taskKey, leaseUntil = ARGV[1], ARGV[2]

local score = redis.call('ZSCORE', due, taskKey)
if score and tonumber(score) == tonumber(leaseUntil) then
    redis.call('ZREM', due, taskKey)
    return 1
end

return 0
