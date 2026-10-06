local room, roomIds = KEYS[1], KEYS[2]
local roomId, body, memberKeyPrefix = ARGV[1], ARGV[2], ARGV[3]

if redis.call('EXISTS', room) == 1 then
    return 0
end

redis.call('HSET', room, 'body', body, 'revision', 1)
redis.call('SADD', roomIds, roomId)
for i = 4, #ARGV do
    redis.call('SET', memberKeyPrefix .. ARGV[i], roomId)
end

return 1
