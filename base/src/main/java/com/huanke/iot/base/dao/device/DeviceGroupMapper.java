package com.huanke.iot.base.dao.device;

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

    void updateGroupItemStatus(@Param("deviceId") Integer dId, @Param("userId") Integer userId, @Param("status") Integer status);

    List<DeviceGroupItemPo> queryGroupItems(DeviceGroupItemPo queryDeviceGroupItem);

    Integer queryGroupCount(@Param("userId") Integer userId, @Param("groupName") String groupName);

    Integer updateGroupStatus(@Param("userId") Integer userId, @Param("groupId") Integer groupId, @Param("status") Integer status);

    int updateDeviceGroupItem(@Param("userId") Integer userId,
                              @Param("currentGroupId") Integer currentGroupId,
                              @Param("newGroupId") int newGroupId);
    int updateDeviceGroupId(@Param("userId") Integer userId,
                            @Param("newGroupId") int newGroupId,
                            @Param("deviceId") Integer deviceId);

    DeviceGroupPo queryByName(DeviceGroupPo deviceGroupPo);

    DeviceGroupPo selectById(DeviceGroupItemPo deviceGroupItemPo);

    Integer deleteDeviceGroupItem(@Param("deviceId") Integer deviceId, @Param("userId") Integer userId);

    Integer deleteGroupItemByDeviceId(Integer deviceId);
}
