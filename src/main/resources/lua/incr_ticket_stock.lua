local stock = redis.call('GET', KEYS[1])

--如果redis 沒有初始化
if not stock then
    stock = ARGV[2]
    redis.call('SET', KEYS[1], stock)
end

local quantity = tonumber(ARGV[1])

return redis.call('INCRBY', KEYS[1], quantity)