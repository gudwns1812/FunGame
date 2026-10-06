local room = KEYS[1]
local roomId, expectedRevision, body, memberKeyPrefix, joinedCount = ARGV[1], ARGV[2], ARGV[3], ARGV[4], tonumber(ARGV[5])

if redis.call('HGET', room, 'revision') ~= expectedRevision then
    return 0
end

redis.call('HSET', room, 'body', body)
redis.call('HINCRBY', room, 'revision', 1)

local firstJoined, firstLeft = 6, 6 + joinedCount
for i = firstJoined, firstLeft - 1 do
    redis.call('SET', memberKeyPrefix .. ARGV[i], roomId)
end
for i = firstLeft, #ARGV do
    local memberKey = memberKeyPrefix .. ARGV[i]
    if redis.call('GET', memberKey) == roomId then
        redis.call('DEL', memberKey)
    end
end

return 1
