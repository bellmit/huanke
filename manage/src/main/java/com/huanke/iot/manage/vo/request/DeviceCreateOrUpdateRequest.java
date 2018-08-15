package com.huanke.iot.manage.vo.request;

import lombok.Data;

@Data
public class DeviceCreateOrUpdateRequest {
    /**
     * 数据库中的id
     */
    private Integer id;
    /**
     * 设备名称
     */
    private String name;
    /**
     * 设备类型id
     */
    private Integer deviceTypeId;
    /**
     * 设备mac地址
     */
    private String mac;
    /**
     * 生产日期
     */
    private Long createTime;

}
