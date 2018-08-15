package com.huanke.iot.manage.vo.request;

import lombok.Data;

/**
 * @author haoshijing
 * @version 2018年04月02日 15:13
 **/
@Data
public class DeviceQueryRequest {
    private String mac;
    private Integer onlineStatus;
    private Integer page = 1;
    private Integer limit = 20;
}
