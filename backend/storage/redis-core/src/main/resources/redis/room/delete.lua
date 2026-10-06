local room, roomIds = KEYS[1], KEYS[2]
local roomId, expectedRevision, memberKeyPrefix = ARGV[1], ARGV[2], ARGV[3]

if redis.call('HGET', room, 'revision') ~= expectedRevision then
    return 0
end

redis.call('DEL', room)
redis.call('SREM', roomIds, roomId)
for i = 4, #ARGV do
    local memberKey = memberKeyPrefix .. ARGV[i]
    if redis.call('GET', memberKey) == roomId then
        redis.call('DEL', memberKey)
    end
end

return 1
