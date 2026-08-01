package com.fujian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fujian.pojo.ShopDailyCounter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface ShopDailyCounterMapper extends BaseMapper<ShopDailyCounter> {

    @Insert("""
            INSERT INTO shop_daily_counters (shop_id, biz_date, order_seq, pickup_seq)
            VALUES (#{shopId}, #{bizDate}, 0, 0)
            ON DUPLICATE KEY UPDATE shop_id = shop_id
            """)
    int ensureDailyCounterRow(@Param("shopId") Long shopId, @Param("bizDate") LocalDate bizDate);

    @Select("""
            SELECT id, shop_id, biz_date, order_seq, pickup_seq, create_time, update_time
            FROM shop_daily_counters
            WHERE shop_id = #{shopId} AND biz_date = #{bizDate}
            FOR UPDATE
            """)
    ShopDailyCounter selectForUpdate(@Param("shopId") Long shopId, @Param("bizDate") LocalDate bizDate);
}
