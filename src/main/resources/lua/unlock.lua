-- unlock.lua
-- KEYS[1]: 鎖的 key
-- ARGV[1]: 鎖的 value (只有相等才解鎖)
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1])
else
    return 0
end
