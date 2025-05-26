local stock = redis.call('GET', KEYS[1])

--如果redis 沒有初始化
if not stock then
    stock = ARGV[2]
    redis.call('SET', KEYS[1], stock)
end

local quantity = tonumber(ARGV[1])

if tonumber(stock) < quantity then
    return -1
end

return redis.call('DECRBY', KEYS[1], quantity)