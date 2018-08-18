package com.huanke.iot.manage.service.device.typeModel;

import com.huanke.iot.base.api.ApiResponse;
import com.huanke.iot.base.constant.RetCode;
import com.huanke.iot.base.dao.device.ablity.DeviceAblitySetMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceTypeAblitySetMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceTypeMapper;
import com.huanke.iot.base.po.device.alibity.DeviceAblitySetPo;
import com.huanke.iot.base.po.device.typeModel.DeviceTypeAblitySetPo;
import com.huanke.iot.base.po.device.typeModel.DeviceTypePo;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceTypeAblitySetCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceTypeCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceTypeQueryRequest;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceTypeCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.response.device.typeModel.DeviceTypeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备类型 service
 */
@Repository
@Slf4j
public class DeviceTypeService {

    @Autowired
    private DeviceTypeMapper deviceTypeMapper;

    @Autowired
    private DeviceAblitySetMapper deviceAblitySetMapper;

    @Autowired
    private DeviceTypeAblitySetMapper deviceTypeAblitySetMapper;

    @Value("${accessKeyId}")
    private String accessKeyId;

    @Value("${accessKeySecret}")
    private String accessKeySecret;

    @Value("${bucketUrl}")
    private String bucketUrl;

    @Value("${bucketName}")
    private String bucketName;


    /**
     * 创建或修改类型
     * @param typeRequest
     * @return
     */
    public Boolean createOrUpdate(DeviceTypeCreateOrUpdateRequest typeRequest) {

        int effectCount = 0;
        DeviceTypePo deviceTypePo = new DeviceTypePo();
        BeanUtils.copyProperties(typeRequest,deviceTypePo);
        if(typeRequest.getId() != null && typeRequest.getId() > 0){
            deviceTypePo.setLastUpdateTime(System.currentTimeMillis());
            effectCount = deviceTypeMapper.updateById(deviceTypePo);
        }else{
            deviceTypePo.setCreateTime(System.currentTimeMillis());
            effectCount =  deviceTypeMapper.insert(deviceTypePo);
        }
        return effectCount > 0;
    }



    /**
     * 删除 类型  该类型下的型号如何操作？？？？？？？？？？？？？？
     * @param typeRequest
     * @return
     */
    public Boolean deleteDeviceType(DeviceTypeCreateOrUpdateRequest typeRequest) {

        Boolean ret  =false;
        //判断当 类型id不为空时
        if( typeRequest.getId()!=null){
            //先删除 该 功能
            ret = deviceTypeMapper.deleteById(typeRequest.getId()) > 0;
        }else{
            log.error("类型主键不可为空");
            return false;
        }
        return ret;
    }


    /**
     * 创建 或修改 类型的功能集
     * @param request
     * @return
     */
    public ApiResponse<Boolean> createOrUpdateDeviceTypeAblitySet(DeviceTypeAblitySetCreateOrUpdateRequest request) {

        int effectCount = 0;
        Boolean ret = false;
        // 主键 不能为空
        if(request.getAblitySetId()==null||request.getAblitySetId()<=0){
            return new ApiResponse<>(RetCode.PARAM_ERROR,"该功能集不存在");
        }
        if(request.getTypeId()==null||request.getTypeId()<=0){
            return new ApiResponse<>(RetCode.PARAM_ERROR,"该类型不存在");
        }

        //保存前 先进行 验证 该类型 和该 功能集是否存在
        DeviceTypePo queryDeviceTypePo = deviceTypeMapper.selectById(request.getTypeId());
        DeviceAblitySetPo queryDeviceAblitySetPo = deviceAblitySetMapper.selectById(request.getAblitySetId());

        if(null==queryDeviceTypePo){
            return new ApiResponse<>(RetCode.PARAM_ERROR,"该类型不存在");
        }

        if(null==queryDeviceAblitySetPo){
            return new ApiResponse<>(RetCode.PARAM_ERROR,"该功能集不存在");
        }

        //只有 类型与功能集都存在才会进行保存
        DeviceTypeAblitySetPo deviceTypeAblitySetPo = new DeviceTypeAblitySetPo();
        deviceTypeAblitySetPo.setAblitySetId(request.getAblitySetId());
        deviceTypeAblitySetPo.setTypeId(request.getTypeId());

        //当主键存在的时候 说明是更新  否则是新增
        if(request.getId()!=null&&request.getId()>0){
            deviceTypeAblitySetPo.setId(request.getId());
            deviceTypeAblitySetPo.setLastUpdateTime(System.currentTimeMillis());
        }else{
            deviceTypeAblitySetPo.setCreateTime(System.currentTimeMillis());
        }

        ret = deviceTypeAblitySetMapper.insert(deviceTypeAblitySetPo)>0;

        return new ApiResponse<>(ret);
    }



    /**
     * 查询类型列表
     * @param request
     * @return
     */
    public List<DeviceTypeVo> selectList(DeviceTypeQueryRequest request) {

        DeviceTypePo queryDeviceTypePo = new DeviceTypePo();
        queryDeviceTypePo.setName(request.getName());
        queryDeviceTypePo.setTypeNo(request.getTypeNo());

        Integer offset = (request.getPage() - 1)*request.getLimit();
        Integer limit = request.getLimit();

        List<DeviceTypePo> deviceTypePos = deviceTypeMapper.selectList(queryDeviceTypePo,limit,offset);
        return deviceTypePos.stream().map(deviceTypePo -> {
            DeviceTypeVo deviceTypeVo = new DeviceTypeVo();
            deviceTypeVo.setName(deviceTypePo.getName());
            deviceTypeVo.setTypeNo(deviceTypePo.getTypeNo());
            deviceTypeVo.setIcon("https://"+bucketUrl+"/"+deviceTypeVo.getIcon());
            deviceTypeVo.setRemark(deviceTypePo.getRemark());
            deviceTypeVo.setId(deviceTypePo.getId());
            return deviceTypeVo;
        }).collect(Collectors.toList());
    }


    /**
     * 根据主键查询类型
     * @param request
     * @return
     */
    public DeviceTypeVo selectById(DeviceTypeQueryRequest request) {

        DeviceTypePo queryDeviceTypePo = new DeviceTypePo();
        queryDeviceTypePo.setId(request.getId());

        DeviceTypePo deviceTypePo = deviceTypeMapper.selectById(queryDeviceTypePo);

        DeviceTypeVo deviceTypeVo = new DeviceTypeVo();
        deviceTypeVo.setName(deviceTypePo.getName());
        deviceTypeVo.setTypeNo(deviceTypePo.getTypeNo());
        deviceTypeVo.setIcon("https://"+bucketUrl+"/"+deviceTypeVo.getIcon());
        deviceTypeVo.setRemark(deviceTypePo.getRemark());
        deviceTypeVo.setId(deviceTypePo.getId());

        return deviceTypeVo;
    }
//    public Integer selectCount(DeviceTypeQueryRequest queryRequest) {
//        DeviceTypePo queryTypePo = new DeviceTypePo();
//        queryTypePo.setName(queryRequest.getName());
//        return deviceTypeMapper.selectCount(queryTypePo);
//    }
}
