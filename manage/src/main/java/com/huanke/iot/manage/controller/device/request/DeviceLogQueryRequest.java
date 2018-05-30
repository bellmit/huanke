package com.huanke.iot.manage.controller.device.request;

import lombok.Data;

/**
 * @author haoshijing
 * @version 2018年05月30日 13:22
 **/
@Data
public class DeviceLogQueryRequest {
    private Integer deviceId;
    private Integer page = 1;
    private Integer limit = 20;
    private String nickname;
}
