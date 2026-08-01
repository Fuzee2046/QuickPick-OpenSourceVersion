package com.fujian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fujian.pojo.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Select("SELECT * FROM orders WHERE id = #{orderId} FOR UPDATE")
    Order selectByIdForUpdate(@Param("orderId") String orderId);
}
