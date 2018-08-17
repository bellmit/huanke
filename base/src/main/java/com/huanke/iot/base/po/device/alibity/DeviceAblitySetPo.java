package com.huanke.iot.base.po.device.alibity;

import lombok.Data;

/**
 * @author Caik
 * @date 2018/8/14 19:19
 */
@Data
public class DeviceAblitySetPo {

    private Integer id;
    private String name;
    private Integer status;
    private String remark;
    private Long createTime;
    private Long lastUpdateTime;
}
