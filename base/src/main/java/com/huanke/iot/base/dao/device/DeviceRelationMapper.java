package com.huanke.iot.base.dao.device;

import com.huanke.iot.base.dao.BaseMapper;
import com.huanke.iot.base.po.device.DeviceRelationPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * @author haoshijing
 * @version 2018年04月23日 15:04
 **/
public interface DeviceRelationMapper extends BaseMapper<DeviceRelationPo> {
    Integer updateStatus(DeviceRelationPo deviceRelationPo);

    Integer deleteByDeviceId(Integer deviceId);

    Integer deleteRelationForJoinId(@Param("deviceId") Integer deviceId, @Param("userId") Integer userId);
}