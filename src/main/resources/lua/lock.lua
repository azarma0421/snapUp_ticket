-- KEYS[1]: 鎖的 key
-- ARGV[1]: 鎖的 value (唯一識別，像 UUID)
-- ARGV[2]: 過期時間（毫秒）
return redis.call("SET", KEYS[1], ARGV[1], "NX", "PX", ARGV[2])