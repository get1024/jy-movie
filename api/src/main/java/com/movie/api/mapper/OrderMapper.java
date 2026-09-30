package com.movie.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movie.api.model.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Update("UPDATE t_order SET status = 1 WHERE status = 0 AND create_at <= #{cutoff}")
    int expireWaitingOrders(@Param("cutoff") String cutoff);

    @Update("UPDATE t_order SET status = 2, pay_at = #{paidAt} "
            + "WHERE id = #{id} AND status = 0 AND create_at > #{cutoff}")
    int payWaitingOrder(@Param("id") String id, @Param("paidAt") String paidAt, @Param("cutoff") String cutoff);

    @Update("UPDATE t_order SET status = 3 WHERE id = #{id} AND status <> 3")
    int cancelOrder(@Param("id") String id);
}
