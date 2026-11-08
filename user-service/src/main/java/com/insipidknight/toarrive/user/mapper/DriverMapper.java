package com.insipidknight.toarrive.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insipidknight.toarrive.user.entity.Driver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DriverMapper extends BaseMapper<Driver> {

    @Select("SELECT * FROM driver WHERE driver_id = #{driverId} AND delete_at = '1970-01-01 00:00:00'")
    Driver selectByDriverId(Long driverId);

    /**
     * 待审核的入驻申请（先到先审）
     */
    @Select("SELECT * FROM driver WHERE verify_status = 1 AND delete_at = '1970-01-01 00:00:00' " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<Driver> selectPending(@Param("limit") int limit);

    /**
     * 审核：仅允许从「待审核」流转（原子）
     */
    @Update("UPDATE driver SET verify_status = #{verifyStatus}, verify_at = NOW(), reject_reason = #{rejectReason} " +
            "WHERE driver_id = #{driverId} AND verify_status = 1 " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int review(@Param("driverId") Long driverId,
               @Param("verifyStatus") Integer verifyStatus,
               @Param("rejectReason") String rejectReason);

    /**
     * 出车/收车：仅审核通过（verify_status = 2）的司机可变更工作状态
     */
    @Update("UPDATE driver SET work_status = #{workStatus} " +
            "WHERE driver_id = #{driverId} AND verify_status = 2 " +
            "AND delete_at = '1970-01-01 00:00:00'")
    int updateWorkStatus(@Param("driverId") Long driverId,
                         @Param("workStatus") Integer workStatus);
}
