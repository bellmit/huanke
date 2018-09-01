package com.huanke.iot.base.po.device.team;

import lombok.Data;

/**
 * @author onlymark
 * @version 2018年08月20日
 **/
@Data
public class DeviceTeamItemPo {
    /**
     * 设备编组id
     */
    private Integer id;

    /**
     * 设备id
     */
    private Integer deviceId;

    private Integer userId;

    /**
     * 设备组id
     */
    private Integer teamId;

    //设备的联动状态，1-联动，2-不联动
    private Integer linkAgeStatus;

    private Integer status;

    private Long createTime;

    private Long lastUpdateTime;

}