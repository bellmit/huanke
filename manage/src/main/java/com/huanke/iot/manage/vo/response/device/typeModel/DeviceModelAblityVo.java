package com.huanke.iot.manage.vo.response.device.typeModel;

import lombok.Data;

import java.util.List;

/**
 * @author caikun
 * @date 2018/8/19 下午6:10
 **/
@Data
public class DeviceModelAblityVo {
    private Integer id;
    private Integer modelId;
    private Integer ablityId;
    private String definedName;
    private Integer status;
    private Long createTime;
    private Long lastUpdateTime;

    private List<DeviceModelAblityOptionVo> deviceModelAblityOptions;

    @Data
    public static class DeviceModelAblityOptionVo {

        private Integer id;
        private Integer ablityOptionId;
        private String definedName;
        private Integer status;

    }
}
