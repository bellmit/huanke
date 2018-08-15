package com.huanke.iot.base.po.customer;

import lombok.Data;

/**
 * @author caikun
 * @version 2018年08月13日 16:10
 **/
@Data
public class CustomerUserRelationPo {
    private Integer id;
    private Integer customerId;
    private String openId;
    private String parentOpenId;
    private String deviceId;
    private Integer status;
    private Long createTime;
    private Long lastUpdateTime;
}