package com.huanke.iot.manage.service.format;

import com.huanke.iot.base.api.ApiResponse;
import com.huanke.iot.base.constant.CommonConstant;
import com.huanke.iot.base.dao.device.ablity.DeviceAblityMapper;
import com.huanke.iot.base.dao.device.ablity.DeviceAblityOptionMapper;
import com.huanke.iot.base.dao.format.WxFormatItemMapper;
import com.huanke.iot.base.dao.format.WxFormatMapper;
import com.huanke.iot.base.po.device.alibity.DeviceAblityOptionPo;
import com.huanke.iot.base.po.device.alibity.DeviceAblityPo;
import com.huanke.iot.base.po.format.WxFormatPo;
import com.huanke.iot.manage.vo.request.device.ablity.DeviceAblityCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.request.device.ablity.DeviceAblityOptionCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.request.device.ablity.DeviceAblityQueryRequest;
import com.huanke.iot.manage.vo.response.device.ablity.DeviceAblityOptionVo;
import com.huanke.iot.manage.vo.response.device.ablity.DeviceAblityVo;
import com.huanke.iot.manage.vo.response.format.WxFormatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class WxFormatService {

    @Autowired
    private WxFormatMapper wxFormatMapper;

    @Autowired
    private WxFormatItemMapper wxFormatItemMapper;


    /**
     * 新增 功能
     *
     * @param wxFormatVo
     * @return
     */
    public ApiResponse<Integer> createOrUpdate(WxFormatVo wxFormatVo) {

        int effectCount = 0;
        Boolean ret = false;
        WxFormatPo wxFormatPo = new WxFormatPo();
        BeanUtils.copyProperties(wxFormatVo, wxFormatPo);
        //如果有id则为更新 否则为新增
        if (wxFormatVo.getId() != null && wxFormatVo.getId() > 0) {
            wxFormatPo.setLastUpdateTime(System.currentTimeMillis());
            ret = wxFormatMapper.updateById(wxFormatPo) > 0;
        } else {
            wxFormatPo.setCreateTime(System.currentTimeMillis());
            ret = wxFormatMapper.insert(wxFormatPo) > 0;
        }
        //判断 该功能里的选项是否为空，若不为空则进行保存
        if (wxFormatVo.getWxFormatItemVos() != null && wxFormatVo.getWxFormatItemVos().size() > 0) {

            for (DeviceAblityOptionCreateOrUpdateRequest deviceAblityOptionRequest : ablityRequest.getDeviceAblityOptions()) {
                DeviceAblityOptionPo deviceAblityOptionPo = new DeviceAblityOptionPo();
                deviceAblityOptionPo.setOptionName(deviceAblityOptionRequest.getOptionName());
                deviceAblityOptionPo.setOptionValue(deviceAblityOptionRequest.getOptionValue());
                deviceAblityOptionPo.setAblityId(deviceAblityPo.getId());
                deviceAblityOptionPo.setStatus(CommonConstant.STATUS_YES);
                //如果 该选项有id 则为更新 ，否则为新增
                if(deviceAblityOptionRequest.getId()!=null&&deviceAblityOptionRequest.getId()>0){
                    deviceAblityOptionPo.setId(deviceAblityOptionRequest.getId());
                    deviceAblityOptionPo.setLastUpdateTime(System.currentTimeMillis());

                    if(CommonConstant.STATUS_DEL.equals(deviceAblityOptionRequest.getStatus())){
                        deviceAblityOptionPo.setStatus(CommonConstant.STATUS_DEL);
                    }
                    deviceAblityOptionMapper.updateById(deviceAblityOptionPo);
                }else{

                    deviceAblityOptionPo.setCreateTime(System.currentTimeMillis());
                    deviceAblityOptionMapper.insert(deviceAblityOptionPo);
                }


            }
        }

        return new ApiResponse<>(deviceAblityPo.getId());
    }

    /**
     * 查询功能列表
     *
     * @param request
     * @return
     */
    public List<DeviceAblityVo> selectList(DeviceAblityQueryRequest request) {

        DeviceAblityPo queryDeviceAblityPo = new DeviceAblityPo();
        queryDeviceAblityPo.setAblityName(request.getAblityName());
        queryDeviceAblityPo.setDirValue(request.getDirValue());
        queryDeviceAblityPo.setWriteStatus(request.getWriteStatus());
        queryDeviceAblityPo.setReadStatus(request.getReadStatus());
        queryDeviceAblityPo.setRunStatus(request.getRunStatus());
        queryDeviceAblityPo.setConfigType(request.getConfigType());
        queryDeviceAblityPo.setAblityType(request.getAblityType());

        Integer offset = (request.getPage() - 1) * request.getLimit();
        Integer limit = request.getLimit();

        //查询 功能列表
        List<DeviceAblityPo> deviceAblityPos = deviceAblityMapper.selectList(queryDeviceAblityPo, limit, offset);
        List<DeviceAblityVo> deviceAblityVos = deviceAblityPos.stream().map(deviceAblityPo -> {
            DeviceAblityVo deviceAblityVo = new DeviceAblityVo();
            deviceAblityVo.setAblityName(deviceAblityPo.getAblityName());
            deviceAblityVo.setDirValue(deviceAblityPo.getDirValue());
            deviceAblityVo.setWriteStatus(deviceAblityPo.getWriteStatus());
            deviceAblityVo.setReadStatus(deviceAblityPo.getReadStatus());
            deviceAblityVo.setRunStatus(deviceAblityPo.getRunStatus());
            deviceAblityVo.setConfigType(deviceAblityPo.getConfigType());
            deviceAblityVo.setAblityType(deviceAblityPo.getAblityType());
            deviceAblityVo.setRemark(deviceAblityPo.getRemark());
            deviceAblityVo.setId(deviceAblityPo.getId());

            //根据功能主键 查询该功能下的 选项列表
            List<DeviceAblityOptionPo> deviceAblityOptionpos = deviceAblityOptionMapper.selectOptionsByAblityId(deviceAblityPo.getId());
            List<DeviceAblityOptionVo> deviceAblityOptionVos = deviceAblityOptionpos.stream().map(deviceAblityOptionPo -> {
                DeviceAblityOptionVo deviceAblityOptionVo = new DeviceAblityOptionVo();
                deviceAblityOptionVo.setOptionValue(deviceAblityOptionPo.getOptionValue());
                deviceAblityOptionVo.setOptionName(deviceAblityOptionPo.getOptionName());
                deviceAblityOptionVo.setId(deviceAblityOptionPo.getId());
                deviceAblityOptionVo.setStatus(deviceAblityOptionPo.getStatus());
                return deviceAblityOptionVo;
            }).collect(Collectors.toList());

            deviceAblityVo.setDeviceAblityOptions(deviceAblityOptionVos);
            return deviceAblityVo;
        }).collect(Collectors.toList());

        return deviceAblityVos;
    }


    /**
     * 根据主键查询 功能
     *
     * @param typeId
     * @return
     */
    public DeviceAblityVo selectById(Integer typeId) {

        DeviceAblityPo deviceAblityPo = deviceAblityMapper.selectById(typeId);

        DeviceAblityVo deviceAblityVo = new DeviceAblityVo();
        if(deviceAblityPo!=null){
            deviceAblityVo.setAblityName(deviceAblityPo.getAblityName());
            deviceAblityVo.setDirValue(deviceAblityPo.getDirValue());
            deviceAblityVo.setWriteStatus(deviceAblityPo.getWriteStatus());
            deviceAblityVo.setReadStatus(deviceAblityPo.getReadStatus());
            deviceAblityVo.setRunStatus(deviceAblityPo.getRunStatus());
            deviceAblityVo.setConfigType(deviceAblityPo.getConfigType());
            deviceAblityVo.setAblityType(deviceAblityPo.getAblityType());
            deviceAblityVo.setRemark(deviceAblityPo.getRemark());
            deviceAblityVo.setId(deviceAblityPo.getId());

            //根据功能主键 查询该功能下的 选项列表
            List<DeviceAblityOptionPo> deviceAblityOptionpos = deviceAblityOptionMapper.selectOptionsByAblityId(deviceAblityPo.getId());
            List<DeviceAblityOptionVo> deviceAblityOptionVos = deviceAblityOptionpos.stream().map(deviceAblityOptionPo -> {
                DeviceAblityOptionVo deviceAblityOptionVo = new DeviceAblityOptionVo();
                deviceAblityOptionVo.setOptionValue(deviceAblityOptionPo.getOptionValue());
                deviceAblityOptionVo.setOptionName(deviceAblityOptionPo.getOptionName());
                deviceAblityOptionVo.setId(deviceAblityOptionPo.getId());
                deviceAblityOptionVo.setStatus(deviceAblityOptionPo.getStatus());
                return deviceAblityOptionVo;
            }).collect(Collectors.toList());

            deviceAblityVo.setDeviceAblityOptions(deviceAblityOptionVos);
        }
        return deviceAblityVo;
    }
    /**
     * 删除 该功能
     * 并同时删除该功能下 所有的选项
     *
     * @param ablityId
     * @return
     */
    public Boolean deleteAblity(Integer ablityId) {

        Boolean ret = false;
        //先删除 该 功能
        ret = deviceAblityMapper.deleteById(ablityId) > 0;
        //再删除 选项表中 的选项
        ret = ret && deviceAblityMapper.deleteOptionByAblityId(ablityId) > 0;
        return ret;
    }


//    public Integer selectCount(DeviceTypeQueryRequest queryRequest) {
//        DeviceTypePo queryTypePo = new DeviceTypePo();
//        queryTypePo.setName(queryRequest.getName());
//        return deviceTypeMapper.selectCount(queryTypePo);
//    }
}
