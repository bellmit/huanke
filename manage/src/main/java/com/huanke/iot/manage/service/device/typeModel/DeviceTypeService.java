package com.huanke.iot.manage.service.device.typeModel;

import com.huanke.iot.base.api.ApiResponse;
import com.huanke.iot.base.constant.CommonConstant;
import com.huanke.iot.base.constant.DeviceConstant;
import com.huanke.iot.base.constant.RetCode;
import com.huanke.iot.base.dao.device.DeviceMapper;
import com.huanke.iot.base.dao.device.ability.DeviceAbilityMapper;
import com.huanke.iot.base.dao.device.ability.DeviceAbilityOptionMapper;
import com.huanke.iot.base.dao.device.ability.DeviceAbilitySetMapper;
import com.huanke.iot.base.dao.device.ability.DeviceTypeAbilitysMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceTypeAbilitySetMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceTypeMapper;
import com.huanke.iot.base.dao.user.UserManagerMapper;
import com.huanke.iot.base.po.device.DevicePo;
import com.huanke.iot.base.po.device.ability.DeviceAbilityOptionPo;
import com.huanke.iot.base.po.device.ability.DeviceAbilitySetPo;
import com.huanke.iot.base.po.device.ability.DeviceTypeAbilitysPo;
import com.huanke.iot.base.po.device.typeModel.DeviceModelPo;
import com.huanke.iot.base.po.device.typeModel.DeviceTypeAbilitySetPo;
import com.huanke.iot.base.po.device.typeModel.DeviceTypePo;
import com.huanke.iot.base.po.user.User;
import com.huanke.iot.base.util.UniNoCreateUtils;
import com.huanke.iot.manage.service.user.UserService;
import com.huanke.iot.manage.vo.request.device.ability.DeviceTypeAbilitysCreateRequest;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceTypeAbilitySetCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceTypeCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceTypeQueryRequest;
import com.huanke.iot.manage.vo.response.device.ability.DeviceAbilityOptionVo;
import com.huanke.iot.manage.vo.response.device.ability.DeviceTypeAbilitysVo;
import com.huanke.iot.manage.vo.response.device.typeModel.DeviceTypeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//import com.huanke.iot.base.dao.device.ability.DeviceAbilitySetMapper;

/**
 * ???????????? service
 */
@Repository
@Slf4j
public class DeviceTypeService {

    @Autowired
    private DeviceTypeMapper deviceTypeMapper;

    @Autowired
    private DeviceTypeAbilitySetMapper deviceTypeAbilitySetMapper;

    @Autowired
    private DeviceTypeAbilitysMapper deviceTypeAbilitysMapper;

    @Autowired
    private DeviceAbilityMapper deviceAbilityMapper;

    @Autowired
    private DeviceAbilityOptionMapper deviceAbilityOptionMapper;

    @Autowired
    private DeviceAbilitySetMapper deviceAbilitySetMapper;

    @Autowired
    private DeviceModelMapper deviceModelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserManagerMapper userManagerMapper;



    @Value("${accessKeyId}")
    private String accessKeyId;

    @Value("${accessKeySecret}")
    private String accessKeySecret;

    @Value("${bucketUrl}")
    private String bucketUrl;

    @Value("${bucketName}")
    private String bucketName;


    /**
     * ?????????????????????
     *
     * @param typeRequest
     * @return
     */
    public ApiResponse<Integer> createOrUpdate(DeviceTypeCreateOrUpdateRequest typeRequest) {

        User user = userService.getCurrentUser();
        //????????? ??????????????????
        DeviceTypePo deviceTypePo = new DeviceTypePo();
        if (typeRequest != null) {
            BeanUtils.copyProperties(typeRequest, deviceTypePo);
        }
        if (typeRequest.getId() != null && typeRequest.getId() > 0) {
            if (CommonConstant.STATUS_DEL.equals(deviceTypePo.getStatus())) {
                deviceTypePo.setStatus(CommonConstant.STATUS_DEL);
            } else {
                deviceTypePo.setStatus(CommonConstant.STATUS_YES);
            }
            deviceTypePo.setLastUpdateTime(System.currentTimeMillis());
            deviceTypePo.setLastUpdateUser(user.getId());
            deviceTypeMapper.updateById(deviceTypePo);
        } else {
            deviceTypePo.setStatus(CommonConstant.STATUS_YES);
            deviceTypePo.setTypeNo(UniNoCreateUtils.createNo(DeviceConstant.DEVICE_UNI_NO_TYPE));
            deviceTypePo.setCreateTime(System.currentTimeMillis());
            deviceTypePo.setCreateUser(user.getId());
            deviceTypeMapper.insert(deviceTypePo);
        }

        //????????? ????????????????????? ???????????? ??????????????????
        deviceTypeAbilitysMapper.deleteByTypeId(deviceTypePo.getId());

        List<DeviceTypeAbilitysCreateRequest> deviceTypeAbilitysCreateRequests = typeRequest.getDeviceTypeAbilitys();
        if (deviceTypeAbilitysCreateRequests != null && deviceTypeAbilitysCreateRequests.size() > 0) {
            //???????????????
            deviceTypeAbilitysCreateRequests.stream().forEach(deviceTypeAbilitysCreateRequest -> {
                DeviceTypeAbilitysPo deviceTypeAbilitysPo = new DeviceTypeAbilitysPo();
                deviceTypeAbilitysPo.setAbilityId(deviceTypeAbilitysCreateRequest.getAbilityId());
                deviceTypeAbilitysPo.setTypeId(deviceTypePo.getId());

                deviceTypeAbilitysMapper.insert(deviceTypeAbilitysPo);

            });

        }
        return new ApiResponse<>(deviceTypePo.getId());

    }


    /**
     * ?????? ????????????????????????  ???????????????????????????????????????????????????????????????????????????
     *
     * @param typeId
     * @return
     */
    public Boolean destoryDeviceType(Integer typeId) {

        DeviceTypePo deviceTypePo = new DeviceTypePo();
        deviceTypePo.setId(typeId);
        Boolean ret = false;
        //????????? ??????id????????????
        //????????? ??? ??????
        ret = deviceTypeMapper.deleteById(typeId) > 0;
        return ret;
    }

    /**
     * ?????? ????????????????????????  ???????????????????????????????????????????????????????????????????????????
     *
     * @param typeId
     * @return
     */
    public ApiResponse<Boolean> deleteDeviceType(Integer typeId) {

        Boolean ret = false;
        User user = userService.getCurrentUser();
        List<DevicePo> devicePos = deviceMapper.selectByTypeId(typeId);
        if (devicePos != null && devicePos.size() > 0) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????????????????????????????");
        }

        List<DeviceModelPo> deviceModelPos = deviceModelMapper.selectByTypeId(typeId);
        if (deviceModelPos != null && deviceModelPos.size() > 0) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????????????????????????????");
        }

        DeviceTypePo deviceTypePo = deviceTypeMapper.selectById(typeId);
        deviceTypePo.setStatus(CommonConstant.STATUS_DEL);
        deviceTypePo.setLastUpdateUser(user.getId());
        deviceTypePo.setLastUpdateTime(System.currentTimeMillis());
        ret = deviceTypeMapper.updateStatusById(deviceTypePo) > 0;
        if (ret) {
            return new ApiResponse<>(RetCode.OK, "????????????", ret);
        } else {
            return new ApiResponse<>(RetCode.ERROR, "????????????", ret);
        }
    }

    /**
     * ?????? ????????? ??????????????????
     *
     * @param request
     * @return
     */
    public ApiResponse<Boolean> createOrUpdateDeviceTypeAbilitySet(DeviceTypeAbilitySetCreateOrUpdateRequest request) {

        int effectCount = 0;
        Boolean ret = false;
        // ?????? ????????????
        if (request.getAbilitySetId() == null || request.getAbilitySetId() <= 0) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
        }
        if (request.getTypeId() == null || request.getTypeId() <= 0) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????");
        }

        //????????? ????????? ?????? ????????? ?????? ?????????????????????
        DeviceTypePo queryDeviceTypePo = deviceTypeMapper.selectById(request.getTypeId());
        DeviceAbilitySetPo queryDeviceAbilitySetPo = deviceAbilitySetMapper.selectById(request.getAbilitySetId());

        if (null == queryDeviceTypePo) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????");
        }

        if (null == queryDeviceAbilitySetPo) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
        }


        //?????? ?????????????????????????????????????????????
        DeviceTypeAbilitySetPo deviceTypeAbilitySetPo = new DeviceTypeAbilitySetPo();
        deviceTypeAbilitySetPo.setAbilitySetId(request.getAbilitySetId());
        deviceTypeAbilitySetPo.setTypeId(request.getTypeId());

        //???????????????????????? ???????????????  ???????????????
        if (request.getId() != null && request.getId() > 0) {
            deviceTypeAbilitySetPo.setId(request.getId());
            deviceTypeAbilitySetPo.setLastUpdateTime(System.currentTimeMillis());
        } else {
            //?????????????????? ?????? ??????????????????????????????????????????????????????????????????
            DeviceTypeAbilitySetPo queryTypeAbilitySetPo = deviceTypeAbilitySetMapper.selectByTypeId(request.getTypeId());
            if (queryTypeAbilitySetPo != null) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "????????????????????????????????????????????????");
            }

            deviceTypeAbilitySetPo.setCreateTime(System.currentTimeMillis());
        }

        ret = deviceTypeAbilitySetMapper.insert(deviceTypeAbilitySetPo) > 0;

        return new ApiResponse<>(ret);
    }


    /**
     * ??????????????????
     *
     * @param request
     * @return
     */
    public List<DeviceTypeVo> selectList(DeviceTypeQueryRequest request) {

        DeviceTypePo queryDeviceTypePo = new DeviceTypePo();
        queryDeviceTypePo.setName(request.getName());
        queryDeviceTypePo.setTypeNo(request.getTypeNo());

        queryDeviceTypePo.setStatus(request.getStatus());

        Integer offset = (request.getPage() - 1) * request.getLimit();
        Integer limit = request.getLimit();

        //?????? ????????????
        List<DeviceTypePo> deviceTypePos = deviceTypeMapper.selectList(queryDeviceTypePo, limit, offset);
        return deviceTypePos.stream().map(deviceTypePo -> {
            DeviceTypeVo deviceTypeVo = new DeviceTypeVo();
            if (deviceTypePo != null) {
                deviceTypeVo.setName(deviceTypePo.getName());
                deviceTypeVo.setTypeNo(deviceTypePo.getTypeNo());
                deviceTypeVo.setIcon(deviceTypePo.getIcon());
                deviceTypeVo.setStopWatch(deviceTypePo.getStopWatch());
                deviceTypeVo.setSource(deviceTypePo.getSource());
                deviceTypeVo.setRemark(deviceTypePo.getRemark());
                deviceTypeVo.setId(deviceTypePo.getId());
                deviceTypeVo.setCreateTime(deviceTypePo.getCreateTime());
                deviceTypeVo.setCreateUser(deviceTypePo.getCreateUser());
                deviceTypeVo.setCreateUserName(this.userService.getUserName(deviceTypePo.getCreateUser()));
                deviceTypeVo.setLastUpdateTime(deviceTypePo.getLastUpdateTime());
                deviceTypeVo.setLastUpdateUser(deviceTypePo.getLastUpdateUser());
                deviceTypeVo.setLastUpdateUserName(this.userService.getUserName(deviceTypePo.getLastUpdateUser()));
            }

            //????????? ????????? ????????????
            List<DeviceTypeAbilitysVo> deviceTypeAbilitysVos = selectAbilitysByTypeId(deviceTypePo.getId());

            deviceTypeVo.setDeviceTypeAbilitys(deviceTypeAbilitysVos);
            return deviceTypeVo;
        }).collect(Collectors.toList());
    }

    public ApiResponse<Integer> selectCount(Integer status) throws Exception {
        DeviceTypePo deviceTypePo = new DeviceTypePo();
        deviceTypePo.setStatus(status);
        return new ApiResponse<>(RetCode.OK, "????????????????????????", deviceTypeMapper.selectCount(deviceTypePo));
    }

    /**
     * ????????????????????????????????????????????????????????????
     *
     * @param typeIds
     * @return
     */
    public List<DeviceTypeVo> selectListByTypeIds(String typeIds) {

        List<String> typeIdList = Arrays.asList(typeIds.split(","));
        //?????? ????????????
        List<DeviceTypePo> deviceTypePos = deviceTypeMapper.selectListByTypeIds(typeIdList);
        return deviceTypePos.stream().map(deviceTypePo -> {
            DeviceTypeVo deviceTypeVo = new DeviceTypeVo();
            if (deviceTypePo != null) {
                deviceTypeVo.setName(deviceTypePo.getName());
                deviceTypeVo.setTypeNo(deviceTypePo.getTypeNo());
                deviceTypeVo.setIcon(deviceTypePo.getIcon());
                deviceTypeVo.setStopWatch(deviceTypePo.getStopWatch());
                deviceTypeVo.setSource(deviceTypePo.getSource());
                deviceTypeVo.setRemark(deviceTypePo.getRemark());
                deviceTypeVo.setId(deviceTypePo.getId());
            }

            //????????? ????????? ????????????
            List<DeviceTypeAbilitysVo> deviceTypeAbilitysVos = selectAbilitysByTypeId(deviceTypePo.getId());

            deviceTypeVo.setDeviceTypeAbilitys(deviceTypeAbilitysVos);
            return deviceTypeVo;
        }).collect(Collectors.toList());
    }


    /**
     * ???????????????????????????
     *
     * @return
     */
    public List<DeviceTypeVo> selectAllTypes() {

        //?????? ????????????
        List<DeviceTypePo> deviceTypePos = deviceTypeMapper.selectAllTypes();
        return deviceTypePos.stream().map(deviceTypePo -> {
            DeviceTypeVo deviceTypeVo = new DeviceTypeVo();
            if (deviceTypePo != null) {
                deviceTypeVo.setName(deviceTypePo.getName());
                deviceTypeVo.setTypeNo(deviceTypePo.getTypeNo());
                deviceTypeVo.setIcon(deviceTypePo.getIcon());
                deviceTypeVo.setStopWatch(deviceTypePo.getStopWatch());
                deviceTypeVo.setSource(deviceTypePo.getSource());
                deviceTypeVo.setRemark(deviceTypePo.getRemark());
                deviceTypeVo.setId(deviceTypePo.getId());
            }

            //????????? ????????? ????????????
            List<DeviceTypeAbilitysVo> deviceTypeAbilitysVos = selectAbilitysByTypeId(deviceTypePo.getId());

            deviceTypeVo.setDeviceTypeAbilitys(deviceTypeAbilitysVos);
            return deviceTypeVo;
        }).collect(Collectors.toList());
    }

    /**
     * ????????????????????????
     *
     * @param typeId
     * @return
     */
    public DeviceTypeVo selectById(Integer typeId) {

        DeviceTypePo deviceTypePo = deviceTypeMapper.selectById(typeId);

        DeviceTypeVo deviceTypeVo = new DeviceTypeVo();
        if (deviceTypePo != null) {
            deviceTypeVo.setName(deviceTypePo.getName());
            deviceTypeVo.setTypeNo(deviceTypePo.getTypeNo());
            deviceTypeVo.setIcon(deviceTypePo.getIcon());
            deviceTypeVo.setStopWatch(deviceTypePo.getStopWatch());
            deviceTypeVo.setSource(deviceTypePo.getSource());
            deviceTypeVo.setRemark(deviceTypePo.getRemark());
            deviceTypeVo.setId(deviceTypePo.getId());

            deviceTypeVo.setCreateTime(deviceTypePo.getCreateTime());
            deviceTypeVo.setCreateUser(deviceTypePo.getCreateUser());
            deviceTypeVo.setLastUpdateTime(deviceTypePo.getLastUpdateTime());
            deviceTypeVo.setLastUpdateUser(deviceTypePo.getLastUpdateUser());

            deviceTypeVo.setCreateUserName(userService.getUserName(deviceTypePo.getCreateUser()));
            deviceTypeVo.setLastUpdateUserName(userService.getUserName(deviceTypePo.getLastUpdateUser()));

            List<DeviceTypeAbilitysVo> deviceTypeAbilitysVos = selectAbilitysByTypeId(deviceTypePo.getId());
            deviceTypeVo.setDeviceTypeAbilitys(deviceTypeAbilitysVos);
        }
        return deviceTypeVo;
    }


    /**
     * ?????????????????? ?????? ????????????????????? ????????????
     *
     * @param typeId
     * @return
     */

    public List<DeviceTypeAbilitysVo> selectAbilitysByTypeId(Integer typeId) {


        /*???????????????????????? ??????????????????????????????????????????*/
        List<DeviceTypeAbilitysPo> deviceTypeAbilitysPos = deviceAbilityMapper.selectAbilityListByTypeId(typeId);
        List<DeviceTypeAbilitysVo> deviceTypeAbilitysVos = deviceTypeAbilitysPos.stream().map(deviceTypeAbilitysPo -> {

            DeviceTypeAbilitysVo deviceTypeAbilitysVo = new DeviceTypeAbilitysVo();
            if (deviceTypeAbilitysPo != null) {
                deviceTypeAbilitysVo.setAbilityId(deviceTypeAbilitysPo.getAbilityId());
                deviceTypeAbilitysVo.setAbilityName(deviceTypeAbilitysPo.getAbilityName());
                deviceTypeAbilitysVo.setId(deviceTypeAbilitysPo.getId());
                deviceTypeAbilitysVo.setTypeId(deviceTypeAbilitysPo.getTypeId());
                deviceTypeAbilitysVo.setAbilityType(deviceTypeAbilitysPo.getAbilityType());

                List<DeviceAbilityOptionPo> deviceAbilityOptionPos = deviceAbilityOptionMapper.selectOptionsByAbilityId(deviceTypeAbilitysPo.getAbilityId());
                if (deviceAbilityOptionPos != null && deviceAbilityOptionPos.size() > 0) {
                    List<DeviceAbilityOptionVo> deviceAbilityOptionVos = deviceAbilityOptionPos.stream().map(deviceAbilityOptionPo -> {
                        DeviceAbilityOptionVo deviceAbilityOptionVo = new DeviceAbilityOptionVo();

                        deviceAbilityOptionVo.setId(deviceAbilityOptionPo.getId());
                        deviceAbilityOptionVo.setOptionName(deviceAbilityOptionPo.getOptionName());
                        deviceAbilityOptionVo.setOptionValue(deviceAbilityOptionPo.getOptionValue());
                        deviceAbilityOptionVo.setStatus(deviceAbilityOptionPo.getStatus());
                        return deviceAbilityOptionVo;
                    }).collect(Collectors.toList());

                    deviceTypeAbilitysVo.setDeviceAbilityOptions(deviceAbilityOptionVos);
                }

            }

            return deviceTypeAbilitysVo;
        }).collect(Collectors.toList());


        return deviceTypeAbilitysVos;

    }


//    public Integer selectCount(DeviceTypeQueryRequest queryRequest) {
//        DeviceTypePo queryTypePo = new DeviceTypePo();
//        queryTypePo.setName(queryRequest.getName());
//        return deviceTypeMapper.selectCount(queryTypePo);
//    }
}
