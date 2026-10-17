package com.insipidknight.toarrive.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insipidknight.toarrive.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 按订单号查询
     */
    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} AND delete_at = '1970-01-01 00:00:00'")
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询乘客进行中的订单（防重复下单）
     */
    @Select("SELECT COUNT(*) FROM orders " +
            "WHERE passenger_id = #{passengerId} " +
            "AND status IN (1, 2, 3, 4) " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int countActiveByPassenger(Long userId);

    /**
     * 司机接单：绑定司机 + 改状态 + 回填接单时间（原子）
     */
    @Update("UPDATE orders SET driver_id = #{driverId}, status = 2, " +
            "accepted_at = NOW() " +
            "WHERE id = #{orderId} AND status = 1 " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int acceptOrder(@Param("orderId") Long orderId,
                    @Param("driverId") Long driverId);

    /**
     * 司机到达上车点：status 2 → 3
     */
    @Update("UPDATE orders SET status = 3, arrived_at = NOW() " +
            "WHERE id = #{orderId} AND driver_id = #{driverId} AND status = 2 " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int arriveOrder(@Param("orderId") Long orderId,
                    @Param("driverId") Long driverId);

    /**
     * 开始行程：status 3 → 4
     */
    @Update("UPDATE orders SET status = 4, started_at = NOW() " +
            "WHERE id = #{orderId} AND driver_id = #{driverId} AND status = 3 " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int startOrder(@Param("orderId") Long orderId,
                   @Param("driverId") Long driverId);

    /**
     * 完成行程：status 4 → 5，并落库结算出的里程 / 时长 / 实付金额（原子）
     */
    @Update("UPDATE orders SET status = 5, finished_at = NOW(), " +
            "distance = #{distance}, duration = #{duration}, actual_price = #{actualPrice} " +
            "WHERE id = #{orderId} AND driver_id = #{driverId} AND status = 4 " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int finishOrder(@Param("orderId") Long orderId,
                    @Param("driverId") Long driverId,
                    @Param("distance") BigDecimal distance,
                    @Param("duration") Integer duration,
                    @Param("actualPrice") BigDecimal actualPrice);

    /**
     * 标记支付：仅订单所属乘客、已完成且未支付时可标记（原子），返回 0 行表示不可支付
     */
    @Update("UPDATE orders SET pay_status = 1, paid_at = NOW() " +
            "WHERE id = #{orderId} AND passenger_id = #{passengerId} AND status = 5 " +
            "AND pay_status = 0 AND delete_at = '1970-01-01 00:00:00'")
    int markPaid(@Param("orderId") Long orderId,
                 @Param("passengerId") Long passengerId);

    /**
     * 取消订单：status IN (1,2,3) → 6，仅乘客或所属司机可取消（原子）
     */
    @Update("UPDATE orders SET status = 6, cancelled_at = NOW(), " +
            "cancel_by = #{cancelBy}, cancel_reason = #{reason} " +
            "WHERE id = #{orderId} AND status IN (1, 2, 3) " +
            "AND (passenger_id = #{userId} OR driver_id = #{userId}) " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int cancelOrder(@Param("orderId") Long orderId,
                    @Param("userId") Long userId,
                    @Param("cancelBy") Integer cancelBy,
                    @Param("reason") String reason);

    /**
     * 司机进行中订单数（防一人多单）
     */
    @Select("SELECT COUNT(*) FROM orders " +
            "WHERE driver_id = #{driverId} " +
            "AND status IN (2, 3, 4) " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int countActiveByDriver(@Param("driverId") Long driverId);

    /**
     * 乘客的订单列表（按创建时间倒序）
     */
    @Select("SELECT * FROM orders " +
            "WHERE passenger_id = #{passengerId} " +
            "AND delete_at = '1970-01-01 00:00:00' " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<Order> selectByPassengerId(@Param("passengerId") Long passengerId,
                                    @Param("limit") int limit);

    /**
     * 司机的订单列表（按创建时间倒序）
     */
    @Select("SELECT * FROM orders " +
            "WHERE driver_id = #{driverId} " +
            "AND delete_at = '1970-01-01 00:00:00' " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<Order> selectByDriverId(@Param("driverId") Long driverId,
                                 @Param("limit") int limit);

    /**
     * 待接单列表（抢单池，按创建时间倒序）
     */
    @Select("SELECT * FROM orders " +
            "WHERE status = 1 " +
            "AND delete_at = '1970-01-01 00:00:00' " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<Order> selectPending(@Param("limit") int limit);

    /**
     * 待接单且已超时的订单（供系统超时取消扫描），按创建时间升序
     */
    @Select("SELECT * FROM orders " +
            "WHERE status = 1 AND created_at < #{cutoff} " +
            "AND delete_at = '1970-01-01 00:00:00' " +
            "ORDER BY created_at ASC LIMIT #{limit}")
    List<Order> selectTimeoutPending(@Param("cutoff") LocalDateTime cutoff,
                                     @Param("limit") int limit);

    /**
     * 系统超时取消：仅待接单可转已取消（原子），返回 0 行表示已被他人抢先（接单/取消）
     */
    @Update("UPDATE orders SET status = 6, cancelled_at = NOW(), cancel_by = 3 " +
            "WHERE id = #{orderId} AND status = 1 " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int cancelBySystem(@Param("orderId") Long orderId);
}
