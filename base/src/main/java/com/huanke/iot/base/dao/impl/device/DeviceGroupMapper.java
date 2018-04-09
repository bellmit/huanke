package com.huanke.iot.base.dao.impl.device;

import com.huanke.iot.base.dao.BaseMapper;
import com.huanke.iot.base.po.device.DeviceGroupItemPo;
import com.huanke.iot.base.po.device.DeviceGroupPo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author haoshijing
 * @version 2018年04月08日 10:20
 **/
public interface DeviceGroupMapper extends BaseMapper<DeviceGroupPo> {

    /**
     *
     * @param deeviceGroupItemPo
     * @return
     */
    int insertGroupItem(DeviceGroupItemPo deeviceGroupItemPo);

    Integer queryItemCount(DeviceGroupItemPo queryItemPo);

    void updateGroupItemStatus(@Param("deviceId") Integer dId, @Param("userId") Integer userId);
}
