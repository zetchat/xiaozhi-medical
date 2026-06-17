package com.atguigu.yygh.orders.service;

public interface RedisService {

    /**
     * 预扣减库存 (使用Lua脚本保证原子性)
     * @param key Redis的Key (如 TICKET_POOL:scheduleId)
     * @return true表示扣减成功，false表示库存不足
     */
    boolean decrementStock(String key);

    /**
     * 回滚/增加库存 (使用Lua脚本保证原子性)
     * @param key Redis的Key
     * @return 增加后的库存数量
     */
    Long incrementStock(String key);
    
    /**
     * 初始化号源库存 (供定时任务或启动时调用)
     * @param key Redis的Key
     * @param stock 初始库存量
     */
    void initStock(String key, int stock);
}
