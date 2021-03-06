package com.huanke.iot.manage.service.device.operate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.huanke.iot.base.api.ApiResponse;
import com.huanke.iot.base.constant.*;
import com.huanke.iot.base.dao.customer.CustomerMapper;
import com.huanke.iot.base.dao.customer.CustomerUserMapper;
import com.huanke.iot.base.dao.customer.WxConfigMapper;
import com.huanke.iot.base.dao.device.*;
import com.huanke.iot.base.dao.device.ability.DeviceAbilityMapper;
import com.huanke.iot.base.dao.device.ability.DeviceAbilityOptionMapper;
import com.huanke.iot.base.dao.device.ability.DeviceTypeAbilitysMapper;
import com.huanke.iot.base.dao.device.data.DeviceOperLogMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelAbilityMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelAbilityOptionMapper;
import com.huanke.iot.base.enums.SensorTypeEnums;
import com.huanke.iot.base.exception.BusinessException;
import com.huanke.iot.base.po.customer.CustomerPo;
import com.huanke.iot.base.po.customer.CustomerUserPo;
import com.huanke.iot.base.po.customer.WxConfigPo;
import com.huanke.iot.base.po.device.*;
import com.huanke.iot.base.po.device.ability.DeviceAbilityOptionPo;
import com.huanke.iot.base.po.device.ability.DeviceAbilityPo;
import com.huanke.iot.base.po.device.ability.DeviceTypeAbilitysPo;
import com.huanke.iot.base.po.device.data.DeviceOperLogPo;
import com.huanke.iot.base.po.device.group.DeviceGroupPo;
import com.huanke.iot.base.po.device.team.DeviceTeamItemPo;
import com.huanke.iot.base.po.device.team.DeviceTeamPo;
import com.huanke.iot.base.po.device.typeModel.DeviceModelAbilityOptionPo;
import com.huanke.iot.base.po.device.typeModel.DeviceModelAbilityPo;
import com.huanke.iot.base.po.user.User;
import com.huanke.iot.base.resp.device.DeviceSelectRsp;
import com.huanke.iot.base.util.LocationUtils;
import com.huanke.iot.base.util.UniNoCreateUtils;
import com.huanke.iot.manage.common.util.ExcelUtil;
import com.huanke.iot.manage.service.customer.CustomerService;
import com.huanke.iot.manage.service.gateway.MqttSendService;
import com.huanke.iot.manage.service.user.UserService;
import com.huanke.iot.manage.service.wechart.WechartUtil;
import com.huanke.iot.manage.vo.request.device.group.FuncListMessage;
import com.huanke.iot.manage.vo.request.device.operate.*;
import com.huanke.iot.manage.vo.response.device.BaseListVo;
import com.huanke.iot.manage.vo.response.device.ability.DeviceAbilityVo;
import com.huanke.iot.manage.vo.response.device.operate.*;
import com.huanke.iot.manage.vo.response.device.typeModel.DeviceModelAbilityVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.assertj.core.util.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DeviceOperateService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceGroupItemMapper deviceGroupItemMapper;

    @Autowired
    private DeviceGroupMapper deviceGroupMapper;

    @Autowired
    private DeviceCustomerRelationMapper deviceCustomerRelationMapper;

    @Autowired
    private DeviceCustomerUserRelationMapper deviceCustomerUserRelationMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerUserMapper customerUserMapper;

    @Autowired
    private DeviceIdPoolMapper deviceIdPoolMapper;

    @Autowired
    private DeviceTypeAbilitysMapper deviceTypeAbilitysMapper;

    @Autowired
    private DeviceModelAbilityMapper deviceModelAbilityMapper;

    @Autowired
    private DeviceTeamMapper deviceTeamMapper;

    @Autowired
    private DeviceAbilityMapper deviceAbilityMapper;

    @Autowired
    private DeviceAbilityOptionMapper deviceAbilityOptionMapper;

    @Autowired
    private DeviceModelAbilityOptionMapper deviceModelAbilityOptionMapper;

    @Autowired
    private DeviceTeamItemMapper deviceTeamItemMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Autowired
    private WechartUtil wechartUtil;

    @Autowired
    private WxConfigMapper wxConfigMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private DeviceOperLogMapper deviceOperLogMapper;

    @Autowired
    private LocationUtils locationUtils;

    @Autowired
    private DeviceParamsMapper deviceParamsMapper;

    @Autowired
    private MqttSendService mqttSendService;


    private static String[] keys = {"name","mac", "customerName", "bindStatus", "enableStatus", "groupName","userName",
            "powerStatus", "onlineStatus", "assignStatus","id", "modelName","modelCode","birthTime", "lastOnlineTime",
            "createUserName", "location","manageName"};

    private static String[] texts = {"??????", "MAC", "??????", "????????????","????????????","?????????","????????????",
            "????????????","????????????","????????????","??????ID","?????????","??????","????????????", "?????????????????????",
            "?????????", "????????????","????????????"};

    /**
     * 2018-08-15
     * sixiaojun
     * ???????????????????????????
     *
     * @param deviceLists
     * @return
     */
    public ApiResponse<List<DeviceAddSuccessVo>> createDevice(List<DeviceCreateOrUpdateRequest.DeviceUpdateList> deviceLists) throws Exception {
        User user = userService.getCurrentUser();
        List<DeviceAddSuccessVo> deviceAddSuccessVoList = new ArrayList<>();
        List<DevicePo> devicePoList = deviceLists.stream().map(device -> {
            DevicePo insertPo = new DevicePo();
            insertPo.setName(device.getName());
            //??????????????????????????????????????? ????????????
            insertPo.setManageName(StringUtils.isNotBlank(device.getManageName())?device.getManageName():device.getName());
            insertPo.setTypeId(device.getTypeId());
            insertPo.setMac(device.getMac());
            //??????????????????????????????
            insertPo.setBindStatus(DeviceConstant.BIND_STATUS_NO);
            //??????????????????????????????
            insertPo.setAssignStatus(DeviceConstant.ASSIGN_STATUS_NO);
            //???????????????????????????
            insertPo.setWorkStatus(DeviceConstant.WORKING_STATUS_NO);
            //???????????????????????????
            insertPo.setPowerStatus(DeviceConstant.POWER_STATUS__NO);
            insertPo.setStatus(CommonConstant.STATUS_YES);
            //???????????????????????????
            insertPo.setOnlineStatus(DeviceConstant.ONLINE_STATUS_NO);
            //???????????????????????????
            insertPo.setEnableStatus(DeviceConstant.ENABLE_STATUS_YES);
            insertPo.setHardVersion(device.getHardVersion());
            insertPo.setBirthTime(device.getBirthTime());

            /*????????????????????????*/
            insertPo.setCreateTime(System.currentTimeMillis());
            insertPo.setCreateUser(user.getId());

            insertPo.setSaNo(UniNoCreateUtils.createNo(DeviceConstant.DEVICE_UNI_NO_DEVICE));
            return insertPo;
        }).collect(Collectors.toList());
        //????????????
        Boolean ret = deviceMapper.insertBatch(devicePoList) > 0;
        if (ret) {
            devicePoList.stream().forEach(devicePo -> {
                DeviceAddSuccessVo deviceAddSuccessVo = new DeviceAddSuccessVo();
                deviceAddSuccessVo.setDeviceId(devicePo.getId());
                deviceAddSuccessVoList.add(deviceAddSuccessVo);
            });
            return new ApiResponse<>(RetCode.OK, "????????????", deviceAddSuccessVoList);
        } else {
            return new ApiResponse<>(RetCode.OK, "????????????", null);
        }
    }

    /**
     * ????????????
     *
     * @param deviceUpdateRequest
     * @return
     */
    public ApiResponse<Boolean> updateDevice(DeviceUpdateRequest deviceUpdateRequest) {
        boolean ret = true;
        User user = userService.getCurrentUser();
        DevicePo devicePo = deviceMapper.selectById(deviceUpdateRequest.getId());
        if (devicePo != null) {
            BeanUtils.copyProperties(deviceUpdateRequest, devicePo);
            /*????????????????????????*/
            devicePo.setLastUpdateUser(user.getId());
            devicePo.setLastUpdateTime(System.currentTimeMillis());

            ret = deviceMapper.updateById(devicePo) > 0;

            /* ????????????????????????*/
            //sendParamFunc(deviceUpdateRequest);

        } else {
            return new ApiResponse<>(RetCode.OK, "?????????????????????", false);
        }

        return new ApiResponse<>(RetCode.OK, "????????????", ret);
    }

    /**
     * ????????????????????????
     * @param deviceUpdateRequest
     * @return
     */
    @Transactional
    public String sendParamFunc(DeviceUpdateRequest deviceUpdateRequest) {
        User user = userService.getCurrentUser();
        Map<Integer, List<String>> configMap = new HashMap<>();
        List<DeviceUpdateRequest.ParamConfig> paramConfigList = deviceUpdateRequest.getParamConfigList();
        for (DeviceUpdateRequest.ParamConfig paramConfig : paramConfigList) {
            Integer sort = paramConfig.getSort();
            List<String> valuesList = paramConfig.getValuesList();
            String values = String.join(",", valuesList);
            DeviceParamsPo deviceParamsPo = new DeviceParamsPo();
            DeviceAbilityPo deviceAbilityPo = deviceAbilityMapper.selectByDirValue(deviceUpdateRequest.getAbilityDirValue() + "." + sort);
            deviceParamsPo.setDeviceId(deviceUpdateRequest.getId());
            deviceParamsPo.setAbilityId(deviceAbilityPo.getId());
            deviceParamsPo.setTypeName(deviceUpdateRequest.getAbilityDirValue());
            deviceParamsPo.setStatus(CommonConstant.STATUS_YES);
            deviceParamsPo.setSort(sort);


            DeviceParamsPo oldDeviceParamsPo = deviceParamsMapper.selectList(deviceParamsPo);
            if(oldDeviceParamsPo == null){
                deviceParamsPo.setValue(values);
                deviceParamsPo.setUpdateWay(DeviceConstant.DEVICE_OPERATE_SYS_BACKEND);//???????????????3-??????
                deviceParamsPo.setCreateUserId(user.getId());
                deviceParamsPo.setCreateTime(System.currentTimeMillis());

                deviceParamsMapper.insert(deviceParamsPo);
            }else{
                oldDeviceParamsPo.setValue(values);
                oldDeviceParamsPo.setUpdateWay(DeviceConstant.DEVICE_OPERATE_SYS_BACKEND);//???????????????3-??????

                deviceParamsPo.setUpdateUserId(user.getId());
                deviceParamsPo.setLastUpdateTime(System.currentTimeMillis());
                deviceParamsMapper.updateById(oldDeviceParamsPo);
            }
            configMap.put(sort, valuesList);
        }
        //?????????????????????
        String requestId = sendFuncToDevice(user.getId(), deviceUpdateRequest.getId(), deviceUpdateRequest.getAbilityDirValue(), configMap, 3);
        return requestId;
    }

    private String sendFuncToDevice(Integer userId, Integer deviceId, String abilityDirValue, Map<Integer, List<String>> configMap, int operType) {
        List<DeviceUpdateRequest.ConfigFuncMessage> configFuncMessages = new ArrayList<>();
        String topic = "/down2/cfgC/" + deviceId;
        String requestId = UUID.randomUUID().toString().replace("-", "");
        DeviceOperLogPo deviceOperLogPo = new DeviceOperLogPo();
        deviceOperLogPo.setFuncId(abilityDirValue);
        deviceOperLogPo.setDeviceId(deviceId);
        deviceOperLogPo.setOperType(operType);
        deviceOperLogPo.setOperUserId(userId);
        deviceOperLogPo.setFuncValue(configMap.toString());
        deviceOperLogPo.setRequestId(requestId);
        deviceOperLogPo.setCreateTime(System.currentTimeMillis());
        deviceOperLogMapper.insert(deviceOperLogPo);
        //????????????
        for (Map.Entry<Integer, List<String>> entry : configMap.entrySet()) {
            Integer sort = entry.getKey();
            List<String> values = entry.getValue();
            DeviceUpdateRequest.ConfigFuncMessage configFuncMessage = new DeviceUpdateRequest.ConfigFuncMessage();
            configFuncMessage.setType(abilityDirValue + "." + sort);
            configFuncMessage.setValue(values);
            configFuncMessages.add(configFuncMessage);
        }
        Map<String, List> req = new HashMap<String, List>();
        req.put("datas", configFuncMessages);
        mqttSendService.sendMessage(topic, JSON.toJSONString(req));
        return requestId;
    }

    /**
     * 2018-08-15
     * sixiaojun
     * ??????????????????????????????
     *
     * @param deviceId
     * @return DeviceListVo
     */
    public DeviceListVo queryDeviceById(Integer deviceId) throws Exception {

        //???????????????????????????????????????DevicePo????????????null???????????????????????????DevicePo
        //???????????????????????????

        DevicePo devicePo = deviceMapper.selectById(deviceId);
        if (devicePo == null) {
            return null;
        }

        DeviceCustomerRelationPo deviceCustomerRelationPo;
        DeviceListVo deviceQueryVo = new DeviceListVo();
        deviceQueryVo.setName(devicePo.getName());
        deviceQueryVo.setManageName(devicePo.getManageName());
        deviceQueryVo.setMac(devicePo.getMac());
        deviceQueryVo.setSaNo(devicePo.getSaNo());
        deviceQueryVo.setWxDeviceId(devicePo.getWxDeviceId());

        deviceQueryVo.setTypeId(devicePo.getTypeId());
        deviceQueryVo.setDeviceType(devicePo.getTypeName());
        deviceQueryVo.setTypeNo(devicePo.getTypeNo());
        deviceQueryVo.setModelId(devicePo.getModelId());
        deviceQueryVo.setModelName(devicePo.getModelName());
        deviceQueryVo.setModelCode(devicePo.getModelCode());
        deviceQueryVo.setModelNo(devicePo.getModelNo());

        /*??????????????????????????????*/
        deviceCustomerRelationPo = deviceCustomerRelationMapper.selectByDeviceId(devicePo.getId());
        if (null != deviceCustomerRelationPo) {
            deviceQueryVo.setCustomerId(deviceCustomerRelationPo.getCustomerId());
            deviceQueryVo.setCustomerName(deviceCustomerRelationPo.getCustomerName());
            deviceQueryVo.setSLD(deviceCustomerRelationPo.getSLD());
        }

        deviceQueryVo.setIp(devicePo.getIp());
        deviceQueryVo.setMapGps(devicePo.getMapGps());
        deviceQueryVo.setLocation(devicePo.getLocation());
        deviceQueryVo.setAssignStatus(devicePo.getAssignStatus());
        deviceQueryVo.setBindStatus(devicePo.getBindStatus());
        deviceQueryVo.setEnableStatus(devicePo.getEnableStatus());
        deviceQueryVo.setWorkStatus(devicePo.getWorkStatus());
        deviceQueryVo.setOnlineStatus(devicePo.getOnlineStatus());
        deviceQueryVo.setStatus(devicePo.getStatus());
        //???????????????????????????
        deviceQueryVo.setHostStatus(devicePo.getHostStatus());
        Integer childCount = this.deviceMapper.queryChildDeviceCount(devicePo.getId());
        deviceQueryVo.setChildCount(childCount);
        //??????????????????
        DeviceGroupPo queryDeviceGroup = this.deviceGroupMapper.selectByDeviceId(devicePo.getId());
        if (null != queryDeviceGroup) {
            deviceQueryVo.setGroupId(queryDeviceGroup.getId());
            deviceQueryVo.setGroupName(queryDeviceGroup.getName());
        } else {
            deviceQueryVo.setGroupId(-1);
            deviceQueryVo.setGroupName("?????????");
        }
        deviceQueryVo.setId(devicePo.getId());
        deviceQueryVo.setBirthTime(devicePo.getBirthTime());
        /*???????????????????????????????????????????????????*/
        deviceQueryVo.setCreateTime(devicePo.getCreateTime());
        deviceQueryVo.setLastUpdateTime(devicePo.getLastUpdateTime());
        deviceQueryVo.setCreateUser(devicePo.getCreateUser());
        deviceQueryVo.setCreateUserName(userService.getUserName(devicePo.getCreateUser()));
        deviceQueryVo.setLastUpdateUser(devicePo.getLastUpdateUser());
        deviceQueryVo.setLastUpdateUserName(userService.getUserName(devicePo.getLastUpdateUser()));
        //??????????????????
        DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = this.deviceCustomerUserRelationMapper.selectByDeviceId(devicePo.getId());
        if (null != deviceCustomerUserRelationPo) {
            deviceQueryVo.setUserOpenId(deviceCustomerUserRelationPo.getOpenId());
            deviceQueryVo.setUserName(deviceCustomerUserRelationPo.getNickname());
        }
        deviceQueryVo.setPowerStatus(devicePo.getPowerStatus());
        deviceQueryVo.setLastOnlineTime(devicePo.getLastOnlineTime());
        deviceQueryVo.setLocation(devicePo.getLocation());

        /*??????????????? ??????????????????????????????*/
        List<DeviceModelAbilityVo> deviceModelAbilityVos = getModelVo(deviceId);
        return deviceQueryVo;
    }

    /**
     * 2018-08-15
     * sixiaojun
     * ??????????????????????????????????????????
     *
     * @param deviceListQueryRequest
     * @return list
     */
    public ApiResponse<List<DeviceListVo>> queryDeviceByPage(DeviceListQueryRequest deviceListQueryRequest) throws Exception {
        //todo ???????????????
//        Subject subject = SecurityUtils.getSubject();
//        UserPo user = (UserPo) subject.getSession().getAttribute("user");
        Integer offset = (deviceListQueryRequest.getPage() - 1) * deviceListQueryRequest.getLimit();
        Integer limit = deviceListQueryRequest.getLimit();
        Integer customerId = customerService.obtainCustomerId(false);
        log.info("???????????????id: =",customerId);
        //???????????????????????????????????????DevicePo????????????null???????????????????????????DevicePo
        //???????????????????????????
        DevicePo queryPo = new DevicePo();
        if (deviceListQueryRequest != null) {
            BeanUtils.copyProperties(deviceListQueryRequest, queryPo);
        }
        if (deviceListQueryRequest.getCustomerId() == null) {
            queryPo.setCustomerId(customerId);
        }
        List<DevicePo> devicePos = deviceMapper.selectList(queryPo, limit, offset);
        if (null == devicePos || 0 == devicePos.size()) {
            return new ApiResponse<>(RetCode.OK, "????????????", null);
        }
        List<DeviceListVo> deviceQueryVos = devicePos.stream().map(devicePo -> {
            DeviceListVo deviceQueryVo = new DeviceListVo();
            deviceQueryVo.setName(devicePo.getName());
            deviceQueryVo.setManageName(devicePo.getManageName());
            deviceQueryVo.setMac(devicePo.getMac());
            deviceQueryVo.setSaNo(devicePo.getSaNo());

            deviceQueryVo.setTypeId(devicePo.getTypeId());
            deviceQueryVo.setTypeNo(devicePo.getTypeNo());
            deviceQueryVo.setModelId(devicePo.getModelId());
            deviceQueryVo.setModelName(devicePo.getModelName());
            deviceQueryVo.setModelCode(devicePo.getModelCode());
            deviceQueryVo.setModelNo(devicePo.getModelNo());
            //??????????????????
//            DeviceTeamItemPo deviceTeamItemPo = this.deviceTeamItemMapper.selectByDeviceId(devicePo.getId());
//            if(null != deviceTeamItemPo){
//                deviceQueryVo.setManageName(deviceTeamItemPo.getManageName());
//                log.info("??????????????????????????????={}",deviceQueryVo.getModelName());
//            }
            deviceQueryVo.setCustomerId(devicePo.getCustomerId());
            deviceQueryVo.setCustomerName(devicePo.getCustomerName());

            deviceQueryVo.setLocation(devicePo.getLocation());
            deviceQueryVo.setAssignStatus(devicePo.getAssignStatus());
            deviceQueryVo.setBindStatus(devicePo.getBindStatus());
            deviceQueryVo.setEnableStatus(devicePo.getEnableStatus());
            deviceQueryVo.setWorkStatus(devicePo.getWorkStatus());
            deviceQueryVo.setPowerStatus(devicePo.getPowerStatus());
            deviceQueryVo.setOnlineStatus(devicePo.getOnlineStatus());
            deviceQueryVo.setStatus(devicePo.getStatus());
            //??????????????????
            deviceQueryVo.setLastOnlineTime(devicePo.getLastOnlineTime());
            //???????????????????????????
            deviceQueryVo.setHostStatus(devicePo.getHostStatus());
            Integer childCount = this.deviceMapper.queryChildDeviceCount(devicePo.getId());
            deviceQueryVo.setChildCount(childCount);
            //??????????????????
            DeviceGroupPo queryDeviceGroup = this.deviceGroupMapper.selectByDeviceId(devicePo.getId());
//            DeviceGroupItemPo queryDeviceGroupItemPo = this.deviceGroupItemMapper.selectByDeviceId(devicePo.getId());
            if (null != queryDeviceGroup) {
                deviceQueryVo.setGroupId(queryDeviceGroup.getId());
                deviceQueryVo.setGroupName(queryDeviceGroup.getName());
            } else {
                deviceQueryVo.setGroupId(-1);
                deviceQueryVo.setGroupName("?????????");
            }
            deviceQueryVo.setId(devicePo.getId());
            deviceQueryVo.setBirthTime(devicePo.getBirthTime());
            /*???????????????????????????????????????????????????*/
            deviceQueryVo.setCreateTime(devicePo.getCreateTime());
            deviceQueryVo.setLastUpdateTime(devicePo.getLastUpdateTime());
            deviceQueryVo.setCreateUser(devicePo.getCreateUser());
//            deviceQueryVo.setCreateUserName(userService.getUserName(devicePo.getCreateUser()));
            deviceQueryVo.setLastUpdateUser(devicePo.getLastUpdateUser());
//            deviceQueryVo.setLastUpdateUserName(userService.getUserName(devicePo.getLastUpdateUser()));
            //??????????????????
            DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = this.deviceCustomerUserRelationMapper.selectByDeviceId(devicePo.getId());
            if (null != deviceCustomerUserRelationPo) {
                deviceQueryVo.setUserOpenId(deviceCustomerUserRelationPo.getOpenId());
                deviceQueryVo.setUserName(deviceCustomerUserRelationPo.getNickname());
            }
            deviceQueryVo.setLocation(devicePo.getLocation());
            //todo ???????????????
            return deviceQueryVo;
        }).collect(Collectors.toList());

        return new ApiResponse<>(RetCode.OK, "????????????", deviceQueryVos);
    }


    /**
     * 2018-08-15
     * sixiaojun
     * ??????????????????????????????????????????
     *
     * @param deviceListQueryRequest
     * @return list
     */
    public ApiResponse<BaseListVo> queryDeviceList(DeviceListQueryRequest deviceListQueryRequest) throws Exception {

        BaseListVo baseListVo = new BaseListVo();

        Integer customerId = customerService.obtainCustomerId(false);
        //???????????????????????????????????????DevicePo????????????null???????????????????????????DevicePo
        //???????????????????????????
        DevicePo queryPo = new DevicePo();
        if (deviceListQueryRequest != null) {
            BeanUtils.copyProperties(deviceListQueryRequest, queryPo);
        }

        if (deviceListQueryRequest.getCustomerId() == null) {
            queryPo.setCustomerId(customerId);
        }
        ApiResponse<List<DeviceListVo>> deviceQueryRtn = queryDeviceByPage(deviceListQueryRequest);
        if (deviceQueryRtn != null && deviceQueryRtn.getCode() != RetCode.OK) {
            return new ApiResponse<>(RetCode.ERROR, deviceQueryRtn.getMsg());
        }
        Integer totalCount = selectCount(queryPo);

        baseListVo.setDataList(deviceQueryRtn.getData());
        baseListVo.setTotalCount(totalCount);

        return new ApiResponse<>(RetCode.OK, "????????????", baseListVo);
    }

    /**
     * 2018-08-18
     * sixiaojun
     * ??????????????????
     *
     * @param
     * @return
     */
    public Integer selectCount(DevicePo devicePo) throws Exception {

        return deviceMapper.selectCount(devicePo);
    }

    /**
     * ??????????????????
     *
     * @param response
     * @param deviceListExportRequest
     * @return
     * @throws Exception
     */
    public ApiResponse<Boolean> exportDeviceList(HttpServletResponse response, DeviceListExportRequest deviceListExportRequest) throws Exception {
        //????????????map
        Map<String, String> titleMap = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            titleMap.put(keys[i], texts[i]);
        }
        //??????????????????excel??????
        Class cls = deviceListExportRequest.getClass();
        Field[] fields = cls.getDeclaredFields();
        List<String> titleKeys = new ArrayList<>();
        List<String> titleNames = new ArrayList<>();
        Map<String, String> filterMap = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            String getMethodName = "get" + field.getName().substring(0, 1).toUpperCase()
                    + field.getName().substring(1);
            Method getMethod = cls.getMethod(getMethodName, new Class[]{});
            Object value = getMethod.invoke(deviceListExportRequest, new Object[]{});
            if (value instanceof Boolean) {
                Boolean result = (Boolean) field.get(deviceListExportRequest);
                if (result) {
                    titleKeys.add(field.getName());
                    titleNames.add(titleMap.get(field.getName()));
                    filterMap.put(field.getName(),titleMap.get(field.getName()));
                }
            }
        }
        //??????????????????????????????device??????
        ApiResponse<List<DeviceListVo>> result = this.queryDeviceByPage(deviceListExportRequest.getDeviceListQueryRequest());
//        log.info("????????????????????????????????????= {}",result.getCode());
//        log.info("????????????????????????????????????= {}",result.getData());
        if(null == result.getData() || RetCode.OK != result.getCode()){
            return new ApiResponse<>(RetCode.PARAM_ERROR,"???????????????????????????????????????");
        }
        List<DeviceListVo> deviceListVoList = result.getData();
        //??????????????????
        List<DeviceExportVo> deviceExportVoList = new ArrayList<>();
        deviceListVoList.stream().forEach(eachPo ->{
            DeviceExportVo deviceExportVo = new DeviceExportVo();
            BeanUtils.copyProperties(eachPo,deviceExportVo);
            deviceExportVo.setAssignStatus( DeviceConstant.ASSIGN_STATUS_YES.equals(eachPo.getAssignStatus())?"?????????":"?????????");
            deviceExportVo.setBindStatus( DeviceConstant.BIND_STATUS_YES.equals(eachPo.getBindStatus())?"?????????":"?????????");
            deviceExportVo.setEnableStatus( DeviceConstant.ENABLE_STATUS_YES.equals(eachPo.getEnableStatus())?"??????":"??????");
            deviceExportVo.setOnlineStatus(DeviceConstant.ONLINE_STATUS_YES.equals(eachPo.getOnlineStatus())?"??????":"??????");
            deviceExportVo.setPowerStatus(DeviceConstant.POWER_STATUS_YES.equals(eachPo.getPowerStatus())?"??????":"??????");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if(null != eachPo.getLastOnlineTime()) {
                deviceExportVo.setLastOnlineTime(sdf.format(new Date(eachPo.getLastOnlineTime())));
            }
            deviceExportVo.setBirthTime(sdf.format(new Date(eachPo.getBirthTime())));
            deviceExportVoList.add(deviceExportVo);
        });
        String[] titles = new String[titleNames.size()];
        log.info("???????????????????????????????????????????????????{}",titles);
        titleNames.toArray(titles);
        ExcelUtil<DeviceExportVo> deviceListVoExcelUtil = new ExcelUtil<>();
        deviceListVoExcelUtil.exportExcel(deviceListExportRequest.getFileName(), response, deviceListExportRequest.getSheetTitle(), titles, deviceExportVoList, filterMap, deviceListVoExcelUtil.EXCEl_FILE_2007);
        return new ApiResponse<>(RetCode.OK, "ss");
    }

//    /**
//     * ???????????????????????????????????????
//     * ??????????????? ????????? ??????????????????????????????????????????????????????????????????????????????????????????
//     * 2018-08-21
//     * sixiaojun
//     *
//     * @param deviceLists
//     * @return
//     */
//    public ApiResponse<Integer> deleteDevice(List<DeviceCreateOrUpdateRequest.DeviceUpdateList> deviceLists) throws Exception{
//        deviceLists.stream().forEach(device -> {
//            //???????????????????????????mac???????????????
//            DevicePo devicePo = deviceMapper.selectByMac(device.getMac());
//            if (deviceMapper.deleteDevice(devicePo) > 0) {
//                deviceGroupItemMapper.deleteDeviceById(devicePo.getId());
//                //???????????????????????????????????????
//                if (null != deviceCustomerRelationMapper.selectByDeviceId(devicePo.getId())) {
//                    //?????????????????????????????????
//                    deviceCustomerRelationMapper.deleteDeviceById(devicePo.getId());
//                }
//            }
//        });
//        //?????????????????????????????????
//        return new ApiResponse<>(RetCode.OK,"????????????",deviceLists.size());
//    }


    /**
     * ???????????????????????????????????????
     * ??????????????? ????????? ??????????????????????????????????????????????????????????????????????????????????????????
     * 2018-08-21
     * sixiaojun
     *
     * @param deviceVo
     * @return
     */
    public ApiResponse<DevicePo> deleteDevice(DeviceUnbindRequest.deviceVo deviceVo) throws Exception {
        Integer deviceId = deviceVo.deviceId;
        String mac = deviceVo.mac;
        //?????? ??????????????????????????????
        ApiResponse<DevicePo> result = untieOneDeviceToUser(deviceVo);
        //??????????????????????????? ??????????????? ??? ???????????? ???????????????
        if (RetCode.OK == result.getCode()) {
            DevicePo devicePo = result.getData();
            //???????????????????????????????????????
            if (DeviceConstant.ASSIGN_STATUS_YES.equals(devicePo.getAssignStatus())) {
                DeviceIdPoolPo deviceIdPoolPo = this.deviceIdPoolMapper.selectByWxDeviceId(devicePo.getWxDeviceId());

                if (deviceIdPoolPo != null) {
                    deviceIdPoolPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);
                    deviceIdPoolPo.setLastUpdateTime(System.currentTimeMillis());
                    deviceIdPoolMapper.updateById(deviceIdPoolPo);
                }

            }


            //?????? ???????????????????????????
            this.deviceGroupItemMapper.deleteItemsByDeviceId(deviceId);
            //????????????????????????
            deviceCustomerRelationMapper.deleteDeviceById(deviceId);

            deviceMapper.deleteDeviceById(deviceId);

        } else {
            return result;
        }
        return new ApiResponse<>(RetCode.OK, "????????????");
    }

    /**
     * ???????????????????????????????????????
     * ??????????????? ????????? ??????????????????????????????????????????????????????????????????????????????????????????
     * 2018-08-21
     * sixiaojun
     *
     * @param deviceVo
     * @return
     */
    public ApiResponse<DevicePo> deleteOneDevice(DeviceUnbindRequest.deviceVo deviceVo) throws Exception {
        User user = userService.getCurrentUser();
        Integer deviceId = deviceVo.deviceId;
        String mac = deviceVo.mac;
        //?????? ??????????????????????????????
        ApiResponse<DevicePo> result = untieOneDeviceToUser(deviceVo);
        //??????????????????????????? ??????????????? ??? ???????????? ???????????????
        if (RetCode.OK == result.getCode()) {


            //?????? ????????????
            DevicePo queryDevicePo = deviceMapper.selectById(deviceId);
            if (queryDevicePo != null) {

                //???????????????????????????????????????
                if (DeviceConstant.ASSIGN_STATUS_YES.equals(queryDevicePo.getAssignStatus())) {
                    //?????????????????????????????????
                    DeviceIdPoolPo deviceIdPoolPo = this.deviceIdPoolMapper.selectByWxDeviceId(queryDevicePo.getWxDeviceId());

                    if (deviceIdPoolPo != null) {
                        deviceIdPoolPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);
                        deviceIdPoolPo.setLastUpdateTime(System.currentTimeMillis());
                        deviceIdPoolMapper.updateById(deviceIdPoolPo);
                    }
                    //????????????????????????
                    deviceCustomerRelationMapper.deleteDeviceById(deviceId);

                }
                //?????? ???????????????????????????
                this.deviceGroupItemMapper.deleteItemsByDeviceId(deviceId);


                // ?????????????????? ??????deviceid
                queryDevicePo.setWxDeviceId(null);
                queryDevicePo.setWxDevicelicence(null);
                queryDevicePo.setWxQrticket(null);
                //???????????????????????????
                queryDevicePo.setWorkStatus(DeviceConstant.WORKING_STATUS_NO);
                //???????????????????????????
                queryDevicePo.setOnlineStatus(DeviceConstant.ONLINE_STATUS_NO);
                //???????????????????????????
                queryDevicePo.setEnableStatus(DeviceConstant.ENABLE_STATUS_NO);

                queryDevicePo.setAssignStatus(DeviceConstant.ASSIGN_STATUS_NO);

                queryDevicePo.setAssignTime(null);

                queryDevicePo.setStatus(CommonConstant.STATUS_DEL);

                queryDevicePo.setLastUpdateTime(System.currentTimeMillis());

                queryDevicePo.setLastUpdateUser(user.getId());

                deviceMapper.updateById(queryDevicePo);
            }

        } else {
            return result;
        }
        return new ApiResponse<>(RetCode.OK, "????????????");
    }

    /**
     * ????????????????????????
     *
     * @param deviceVo
     * @return
     */
    public ApiResponse<Boolean> recoverDevice(DeviceUnbindRequest.deviceVo deviceVo) {
        try {

            User user = userService.getCurrentUser();
            Integer deviceId = deviceVo.deviceId;
            String mac = deviceVo.mac;
            if (null == deviceId || deviceId <= 0 || StringUtils.isBlank(mac)) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????");
            }

            //????????????????????? ????????????
            DevicePo queryDevicePo = deviceMapper.selectById(deviceId);

            if (null == queryDevicePo) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
            } else if (!mac.equals(queryDevicePo.getMac())) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????mac???????????????");
            }
            if (CommonConstant.STATUS_YES.equals(queryDevicePo.getStatus()) ) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????????????????");
            }

            //??????????????????????????????
            queryDevicePo.setBindStatus(DeviceConstant.BIND_STATUS_NO);
            //???????????????????????????
            queryDevicePo.setWorkStatus(DeviceConstant.WORKING_STATUS_NO);
            queryDevicePo.setStatus(CommonConstant.STATUS_YES);
            //???????????????????????????
            queryDevicePo.setOnlineStatus(DeviceConstant.ONLINE_STATUS_NO);
            //???????????????????????????
            queryDevicePo.setEnableStatus(DeviceConstant.ENABLE_STATUS_NO);
//            queryDevicePo.setCreateTime(System.currentTimeMillis());
            queryDevicePo.setLastUpdateTime(System.currentTimeMillis());
            queryDevicePo.setLastUpdateUser(user.getId());

            deviceMapper.updateById(queryDevicePo);
            return new ApiResponse<>(RetCode.OK, "??????????????????");

        } catch (Exception e) {
            log.error("??????????????????-{}", e);
            return new ApiResponse<>(RetCode.ERROR, "??????????????????");
        }
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????
     * 2018-08-21
     * sixiaojun
     *
     * @param deviceAssignToCustomerRequest
     * @return
     */
    public ApiResponse<Boolean> assignDeviceToCustomer(DeviceAssignToCustomerRequest deviceAssignToCustomerRequest) throws Exception {
        Boolean ret = true;
        User user = userService.getCurrentUser();
        //??????????????????
        List<DeviceQueryRequest.DeviceQueryList> deviceList = deviceAssignToCustomerRequest.getDeviceQueryRequest().getDeviceList();
        if (deviceList != null && deviceList.size() > 0) {
            //????????????device_pool?????? ???????????????????????? ???????????????????????????device_id???device_license
            DeviceIdPoolPo deviceIdPoolPo = new DeviceIdPoolPo();
            deviceIdPoolPo.setCustomerId(deviceAssignToCustomerRequest.getCustomerId());
            deviceIdPoolPo.setProductId(deviceAssignToCustomerRequest.getProductId());
            deviceIdPoolPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);
            Integer devicePoolCount = deviceIdPoolMapper.selectCount(deviceIdPoolPo);
            log.info("devicePoolCount = {}", devicePoolCount);
            //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (deviceList.size() > devicePoolCount) {
                Integer addCount = deviceList.size() - devicePoolCount;
                //????????????
                ApiResponse<Integer> result = createWxDeviceIdPools(deviceAssignToCustomerRequest.getCustomerId(), deviceAssignToCustomerRequest.getProductId(), addCount);
                if (result == null || RetCode.PARAM_ERROR == result.getCode() || RetCode.ERROR == result.getCode()) {
                    return new ApiResponse<>(RetCode.PARAM_ERROR, result.getMsg(), false);
                }
            }
            //???pool???????????????????????????????????????
            Integer offset = 0;
            List<DeviceCustomerRelationPo> deviceCustomerRelationPoList = new ArrayList<>();
            List<DevicePo> devicePoList = new ArrayList<>();
            List<DeviceIdPoolPo> deviceIdPoolPoList = new ArrayList<>();
            for (DeviceQueryRequest.DeviceQueryList device : deviceList) {
                DeviceCustomerRelationPo deviceCustomerRelationPo = new DeviceCustomerRelationPo();
                deviceCustomerRelationPo.setCustomerId(deviceAssignToCustomerRequest.getCustomerId());
                deviceCustomerRelationPo.setDeviceId(deviceMapper.selectByMac(device.getMac()).getId());
                deviceCustomerRelationPo.setCreateTime(System.currentTimeMillis());
                deviceCustomerRelationPo.setLastUpdateTime(System.currentTimeMillis());
                //??????????????????????????????????????????????????????
                deviceCustomerRelationPoList.add(deviceCustomerRelationPo);
                //???pool???????????????id?????????
                DeviceIdPoolPo queryPoolPo = new DeviceIdPoolPo();
                queryPoolPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);
                queryPoolPo.setCustomerId(deviceAssignToCustomerRequest.getCustomerId());
                queryPoolPo.setProductId(deviceAssignToCustomerRequest.getProductId());
                //??????????????????????????????????????????license
                List<DeviceIdPoolPo> poolPoList = deviceIdPoolMapper.selectList(queryPoolPo, 1, offset);
                if (poolPoList != null && poolPoList.size() > 0) {
                    DeviceIdPoolPo resultPo = poolPoList.get(0);

                    //?????????????????????pool??????????????????
                    resultPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_YES);
                    deviceIdPoolPoList.add(resultPo);
                    offset++;
                    //?????????????????????deviceModelId??????????????????????????????????????????
                    DevicePo devicePo = deviceMapper.selectByMac(device.getMac());
                    devicePo.setModelId(deviceAssignToCustomerRequest.getModelId());
                    devicePo.setStatus(CommonConstant.STATUS_YES);
                    devicePo.setAssignStatus(DeviceConstant.ASSIGN_STATUS_YES);
                    devicePo.setAssignTime(System.currentTimeMillis());
                    devicePo.setProductId(deviceAssignToCustomerRequest.getProductId());
                    devicePo.setWxDeviceId(resultPo.getWxDeviceId());
                    devicePo.setWxDevicelicence(resultPo.getWxDeviceLicence());
                    devicePo.setWxQrticket(resultPo.getWxQrticket());
                    //????????????????????????
                    devicePo.setLastUpdateTime(System.currentTimeMillis());
                    devicePo.setLastUpdateUser(user.getId());

                    //?????????????????????????????????
                    devicePoList.add(devicePo);
                }
            }
            //device_customer_relation????????????????????????
            this.deviceCustomerRelationMapper.insertBatch(deviceCustomerRelationPoList);
            //?????????????????????
            this.deviceMapper.updateBatch(devicePoList);
            //????????????????????????????????????????????????????????????pool
            this.deviceIdPoolMapper.updateBatch(deviceIdPoolPoList);
            return new ApiResponse<>(ret);
        } else {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????????????????");
        }
    }

    /**
     * ??????????????????????????????????????????????????????
     * 2018-08-21
     * sixiaojun
     *
     * @param deviceQueryRequests
     * @return
     */
    public ApiResponse<Boolean> callBackDeviceFromCustomer(List<DeviceQueryRequest.DeviceQueryList> deviceQueryRequests) throws Exception {
        List<DeviceCustomerRelationPo> deviceCustomerRelationPoList = new ArrayList<>();
        List<DevicePo> devicePoList = new ArrayList<>();
        List<DeviceIdPoolPo> deviceIdPoolPoList = new ArrayList<>();

        User user = userService.getCurrentUser();

        if (deviceQueryRequests != null && deviceQueryRequests.size() > 0) {
            deviceQueryRequests.stream().forEach(device -> {
                        DevicePo devicePo = this.deviceMapper.selectByMac(device.getMac());
                        DeviceCustomerRelationPo deviceCustomerRelationPo = this.deviceCustomerRelationMapper.selectByDeviceMac(device.getMac());
                        //???????????????????????????????????????
                        deviceCustomerRelationPoList.add(deviceCustomerRelationPo);
                        //?????????????????????????????????
                        DeviceIdPoolPo deviceIdPoolPo = this.deviceIdPoolMapper.selectByWxDeviceId(devicePo.getWxDeviceId());
                        deviceIdPoolPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);
                        deviceIdPoolPo.setLastUpdateTime(System.currentTimeMillis());
                        deviceIdPoolPoList.add(deviceIdPoolPo);
                        DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = this.deviceCustomerUserRelationMapper.selectByDeviceId(devicePo.getId());

                        //???????????????????????????????????????
                        if (null != deviceCustomerUserRelationPo) {
                            this.deviceCustomerUserRelationMapper.deleteRelationByDeviceId(devicePo.getId());
                            this.deviceTeamItemMapper.deleteItemsByDeviceId(devicePo.getId());
                            devicePo.setBindStatus(DeviceConstant.BIND_STATUS_NO);
                            devicePo.setBindTime(null);
                        }
                        //??????????????????????????????
                        devicePo.setModelId(null);
                        devicePo.setWxDeviceId(null);
                        devicePo.setWxDevicelicence(null);
                        devicePo.setWxQrticket(null);
                        devicePo.setProductId(null);

                        devicePo.setAssignStatus(DeviceConstant.ASSIGN_STATUS_NO);
                        devicePo.setAssignTime(null);
                        devicePo.setLastUpdateTime(System.currentTimeMillis());
                        devicePo.setLastUpdateUser(user.getId());
                        devicePoList.add(devicePo);
                    }
            );
        }

        this.deviceMapper.updateBatch(devicePoList);
        this.deviceIdPoolMapper.updateBatch(deviceIdPoolPoList);
        Boolean ret = this.deviceCustomerRelationMapper.deleteBatch(deviceCustomerRelationPoList) > 0;
        if (ret) {
            return new ApiResponse<>(RetCode.OK, "??????????????????", true);
        } else {
            return new ApiResponse<>(RetCode.OK, "??????????????????", false);
        }
    }


    /**
     * ?????? ???????????????????????????
     *
     * @param devicePos
     * @return
     * @throws Exception
     */
    public ApiResponse<Boolean> callBackDeviceList(List<DevicePo> devicePos) throws Exception {

        User user = userService.getCurrentUser();
        //????????????????????????
        List<DeviceCustomerRelationPo> deviceCustomerRelationPoList = new ArrayList<>();
        //????????????
        List<DevicePo> devicePoList = new ArrayList<>();
        // wxDeviceId??????
        List<DeviceIdPoolPo> deviceIdPoolPoList = new ArrayList<>();

        if (devicePos != null && devicePos.size() > 0) {

            devicePos.stream().forEach(devicePo -> {

                DeviceCustomerRelationPo deviceCustomerRelationPo = this.deviceCustomerRelationMapper.selectByDeviceMac(devicePo.getMac());
                //???????????????????????????????????????
                deviceCustomerRelationPoList.add(deviceCustomerRelationPo);
                //?????????????????????????????????
                DeviceIdPoolPo deviceIdPoolPo = this.deviceIdPoolMapper.selectByWxDeviceId(devicePo.getWxDeviceId());
                deviceIdPoolPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);
                deviceIdPoolPo.setLastUpdateTime(System.currentTimeMillis());
                deviceIdPoolPoList.add(deviceIdPoolPo);
                DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = this.deviceCustomerUserRelationMapper.selectByDeviceId(devicePo.getId());

                //???????????????????????????????????????
                if (null != deviceCustomerUserRelationPo) {
                    this.deviceCustomerUserRelationMapper.deleteRelationByDeviceId(devicePo.getId());
                    this.deviceTeamItemMapper.deleteItemsByDeviceId(devicePo.getId());
                    devicePo.setBindStatus(DeviceConstant.BIND_STATUS_NO);
                    devicePo.setBindTime(null);
                }
                //??????????????????????????????
                devicePo.setModelId(null);
                devicePo.setWxDeviceId(null);
                devicePo.setWxDevicelicence(null);
                devicePo.setWxQrticket(null);
                devicePo.setProductId(null);

                devicePo.setAssignStatus(DeviceConstant.ASSIGN_STATUS_NO);
                devicePo.setAssignTime(null);
                devicePo.setLastUpdateTime(System.currentTimeMillis());
                devicePo.setLastUpdateUser(user.getId());
                devicePoList.add(devicePo);
            });

            this.deviceMapper.updateBatch(devicePoList);
            this.deviceIdPoolMapper.updateBatch(deviceIdPoolPoList);
            Boolean ret = this.deviceCustomerRelationMapper.deleteBatch(deviceCustomerRelationPoList) > 0;
            if (ret) {
                return new ApiResponse<>(RetCode.OK, "??????????????????", true);
            } else {
                return new ApiResponse<>(RetCode.ERROR, "??????????????????", false);
            }

        } else {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????????????????", false);
        }
    }

    /**
     * ????????????
     *
     * @param deviceBindToUserRequest
     * @return
     */
    public ApiResponse<Boolean> bindDeviceToUser(DeviceBindToUserRequest deviceBindToUserRequest) throws Exception {
        Integer customerId = this.customerService.obtainCustomerId(false);
        User user = userService.getCurrentUser();
        List<DeviceTeamItemPo> deviceTeamItemPoList = new ArrayList<>();
        List<DevicePo> devicePoList = new ArrayList<>();
        List<DeviceCustomerUserRelationPo> deviceCustomerUserRelationPoList = new ArrayList<>();
        DeviceTeamPo deviceTeamPo = new DeviceTeamPo();
        CustomerUserPo customerUserPo = customerUserMapper.selectByOpenId(deviceBindToUserRequest.getOpenId());
        if (null == customerUserPo) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????");
        }
        //???????????????????????????????????????????????????
        if(null != customerId && customerUserPo.getCustomerId() != customerId){
            return new ApiResponse<>(RetCode.PARAM_ERROR,"???????????????????????????????????????");
        }
        //?????????????????????id???-1 ???????????? ???????????????????????????
        if (DeviceConstant.HAS_TEAM_NO.equals(deviceBindToUserRequest.getTeamId())) {

            deviceTeamPo.setName(deviceBindToUserRequest.getTeamName());
            deviceTeamPo.setMasterUserId(customerUserPo.getId());
            deviceTeamPo.setStatus(CommonConstant.STATUS_YES);
            deviceTeamPo.setCreateTime(System.currentTimeMillis());
            //??????????????????????????????
            deviceTeamPo.setTeamStatus(DeviceTeamConstants.DEVICE_TEAM_STATUS_TERMINAL);
            deviceTeamPo.setTeamType(DeviceTeamConstants.DEVICE_TEAM_TYPE_USER);
            deviceTeamPo.setCreateTime(System.currentTimeMillis());

            deviceTeamPo.setCreateUserId(customerUserPo.getId());
            deviceTeamPo.setCustomerId(customerUserPo.getCustomerId());
            deviceTeamMapper.insert(deviceTeamPo);
        } else {
            deviceTeamPo = this.deviceTeamMapper.selectById(deviceBindToUserRequest.getTeamId());
        }

        Integer deviceTeamId = deviceTeamPo.getId();
        List<DeviceQueryRequest.DeviceQueryList> bindDeviceList = deviceBindToUserRequest.getDeviceQueryRequest().getDeviceList();

        if (bindDeviceList != null && bindDeviceList.size() > 0) {
            for (int m = 0; m < bindDeviceList.size(); m++) {
                DeviceQueryRequest.DeviceQueryList bindDevice = bindDeviceList.get(m);

                DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = new DeviceCustomerUserRelationPo();
                DeviceTeamItemPo deviceTeamItemPo = new DeviceTeamItemPo();

                //?????? ??????????????????
                DevicePo devicePo = this.deviceMapper.selectByMac(bindDevice.getMac());
                if (devicePo != null) {
                    //?????? ???????????????????????????????????? ???????????????????????????
                    DeviceCustomerRelationPo queryDeviceCustomerRelationPo = deviceCustomerRelationMapper.selectByDeviceId(devicePo.getId());
                    if (queryDeviceCustomerRelationPo != null) {
                        //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                        devicePo.setBindStatus(DeviceConstant.BIND_STATUS_YES);
                        //??????????????????
                        devicePo.setBindTime(System.currentTimeMillis());

                        devicePo.setLastUpdateTime(System.currentTimeMillis());
                        devicePo.setLastUpdateUser(user.getId());

                        deviceTeamItemPo.setDeviceId(devicePo.getId());
                        deviceTeamItemPo.setTeamId(deviceTeamId);
                        deviceTeamItemPo.setUserId(customerUserPo.getId());
                        deviceTeamItemPo.setStatus(CommonConstant.STATUS_YES);
                        deviceTeamItemPo.setCreateTime(System.currentTimeMillis());
                        deviceTeamItemPo.setLastUpdateTime(System.currentTimeMillis());
                        deviceCustomerUserRelationPo.setDeviceId(devicePo.getId());
                        deviceCustomerUserRelationPo.setCustomerId(customerUserPo.getId());
                        deviceCustomerUserRelationPo.setOpenId(deviceBindToUserRequest.getOpenId());
                        deviceCustomerUserRelationPo.setStatus(CommonConstant.STATUS_YES);
                        deviceCustomerUserRelationPo.setCreateTime(System.currentTimeMillis());
                        deviceCustomerUserRelationPo.setLastUpdateTime(System.currentTimeMillis());
                        devicePoList.add(devicePo);
                        deviceTeamItemPoList.add(deviceTeamItemPo);
                        deviceCustomerUserRelationPoList.add(deviceCustomerUserRelationPo);
                    } else {
                        return new ApiResponse<>(RetCode.PARAM_ERROR, "MAC???" + bindDevice.getMac() + " ??????????????????????????????", true);
                    }

                } else {
                    return new ApiResponse<>(RetCode.PARAM_ERROR, "MAC???" + bindDevice.getMac() + " ???????????????????????????", true);
                }

            }
        }

        //?????????????????????????????????
        this.deviceMapper.updateBatch(devicePoList);
        //?????????????????????????????????????????????
        this.deviceCustomerUserRelationMapper.insertBatch(deviceCustomerUserRelationPoList);
        //???????????????????????????
        Boolean ret = this.deviceTeamItemMapper.insertBatch(deviceTeamItemPoList) > 0;
        if (ret) {
            return new ApiResponse<>(RetCode.OK, "????????????", true);
        } else {
            return new ApiResponse<>(RetCode.ERROR, "????????????", false);
        }
    }

    /**
     * ????????????(??????)
     *
     * @return
     */
    public ApiResponse<DevicePo> untieOneDeviceToUser(DeviceUnbindRequest.deviceVo deviceVo) {//???????????? todo

        try {
            User user = userService.getCurrentUser();
            Integer deviceId = deviceVo.deviceId;
            String mac = deviceVo.mac;
            if (null == deviceId || deviceId <= 0 || StringUtils.isBlank(mac)) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????");
            }

            //????????????????????? ????????????
            DevicePo queryDevicePo = deviceMapper.selectById(deviceId);

            if (null == queryDevicePo) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
            } else if (!mac.equals(queryDevicePo.getMac())) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????mac???????????????");
            }


            //?????? ??????????????????????????????
            deviceCustomerUserRelationMapper.deleteRealationByDeviceId(deviceId);

            //?????? ???????????? ?????????
            deviceTeamItemMapper.deleteItemsByDeviceId(deviceId);

            //?????? ????????????????????? ??? ?????????
            DevicePo unbindDevicePo = new DevicePo();
            unbindDevicePo.setId(deviceId);
            unbindDevicePo.setBindStatus(DeviceConstant.BIND_STATUS_NO);
            unbindDevicePo.setBindTime(null);
            unbindDevicePo.setLastUpdateTime(System.currentTimeMillis());
            unbindDevicePo.setLastUpdateUser(user.getId());

            deviceMapper.updateById(unbindDevicePo);

            return new ApiResponse<>(RetCode.OK, "????????????", queryDevicePo);
        } catch (Exception e) {
            log.error("??????????????????-{}", e);
            return new ApiResponse<>(RetCode.ERROR, "??????????????????");
        }

    }

    /**
     * ????????????(??????)
     *
     * @return
     */
    public ApiResponse<Boolean> untieDeviceToUser(DeviceUnbindRequest unbindRequest) {//???????????? todo

        try {
            User user = userService.getCurrentUser();
            List<DeviceUnbindRequest.deviceVo> deviceVos = unbindRequest.deviceVos;
            if (deviceVos != null && deviceVos.size() > 0) {
                deviceVos.stream().forEach(deviceVo -> {
                    Integer deviceId = deviceVo.deviceId;
                    String mac = deviceVo.mac;
//                    DevicePo queryDevicePo = deviceMapper.selectById(deviceId);
                    //???????????????????????????????????? ??????????????????

                    deviceCustomerUserRelationMapper.deleteRealationByDeviceId(deviceId);
//                    List<DeviceCustomerUserRelationPo> customerUserRelationPos = deviceCustomerUserRelationMapper.selectByDeviceId(deviceId);
//                    if (customerUserRelationPos != null && customerUserRelationPos.size() > 0) {
//                        customerUserRelationPos.stream().forEach(deviceCustomerUserRelationPo -> {
//                            deviceCustomerUserRelationMapper.deleteRelationByJoinId(deviceCustomerUserRelationPo.getOpenId(), deviceId);
//                        });
//                    }

                    //?????? ????????????????????? ????????? ????????????

                    deviceTeamItemMapper.deleteItemsByDeviceId(deviceId);
//                    List<DeviceTeamItemPo> deviceTeamItemPos = deviceTeamItemMapper.selectItemsByDeviceId(deviceId);
//                    if(deviceTeamItemPos!=null&&deviceTeamItemPos.size()>0){
//                        deviceTeamItemPos.stream().forEach(deviceTeamItemPo -> {
//                            deviceTeamItemMapper.deleteById(deviceTeamItemPo.getId());
//                        });
//                    }

//
                    //?????? ????????????????????? ??? ?????????
                    DevicePo unbindDevicePo = new DevicePo();
                    unbindDevicePo.setId(deviceId);
                    unbindDevicePo.setBindStatus(DeviceConstant.BIND_STATUS_NO);
                    unbindDevicePo.setBindTime(null);
                    unbindDevicePo.setLastUpdateTime(System.currentTimeMillis());
                    unbindDevicePo.setLastUpdateUser(user.getId());

                    deviceMapper.updateById(unbindDevicePo);

                });
            }
//            deviceMapper.updateBindStatus(deviceVos);

            return new ApiResponse<>(RetCode.OK, "????????????");
        } catch (Exception e) {
            log.error("??????????????????-{}", e);
            return new ApiResponse<>(RetCode.ERROR, "??????????????????");
        }

    }

    /**
     * ????????????
     *
     * @return
     */
    public ApiResponse<Boolean> updateDeivceDisble(DeviceUnbindRequest unbindRequest) {//???????????? todo

        try {
            User user = userService.getCurrentUser();
            List<DeviceUnbindRequest.deviceVo> deviceVos = unbindRequest.deviceVos;
            if (deviceVos != null && deviceVos.size() > 0) {
                deviceVos.stream().forEach(deviceVo -> {
                    Integer deviceId = deviceVo.deviceId;
                    String mac = deviceVo.mac;
//                    DevicePo queryDevicePo = deviceMapper.selectById(deviceId);

                    //?????? ????????????????????? ??? ??????
                    DevicePo updateDevicePo = new DevicePo();
                    updateDevicePo.setId(deviceId);
                    updateDevicePo.setEnableStatus(DeviceConstant.ENABLE_STATUS_NO);
                    updateDevicePo.setLastUpdateTime(System.currentTimeMillis());
                    updateDevicePo.setLastUpdateUser(deviceVo.getLastUpdateUser());

                    deviceMapper.updateById(updateDevicePo);

                });
            }
            return new ApiResponse<>(RetCode.OK, "????????????");
        } catch (Exception e) {
            log.error("??????????????????-{}", e);
            return new ApiResponse<>(RetCode.ERROR, "??????????????????");
        }

    }

    /**
     * ????????????
     *
     * @return
     */
    public ApiResponse<Boolean> updateDeivceEnable(DeviceUnbindRequest unbindRequest) {//???????????? todo

        try {
            User user = userService.getCurrentUser();
            List<DeviceUnbindRequest.deviceVo> deviceVos = unbindRequest.deviceVos;
            if (deviceVos != null && deviceVos.size() > 0) {
                deviceVos.stream().forEach(deviceVo -> {
                    Integer deviceId = deviceVo.deviceId;
                    String mac = deviceVo.mac;
//                    DevicePo queryDevicePo = deviceMapper.selectById(deviceId);

                    //?????? ????????????????????? ??????
                    DevicePo updateDevicePo = new DevicePo();
                    updateDevicePo.setId(deviceId);
                    updateDevicePo.setEnableStatus(DeviceConstant.ENABLE_STATUS_YES);
                    updateDevicePo.setLastUpdateTime(System.currentTimeMillis());
                    updateDevicePo.setLastUpdateUser(user.getId());

                    deviceMapper.updateById(updateDevicePo);

                });
            }
            return new ApiResponse<>(RetCode.OK, "????????????");
        } catch (Exception e) {
            log.error("??????????????????-{}", e);
            return new ApiResponse<>(RetCode.ERROR, "??????????????????");
        }

    }

    public ApiResponse<List<DeviceShareListVo>> queryShareList(Integer deviceId) {
        List<DeviceShareListVo> deviceShareListVoList = new ArrayList<>();
        //???????????????????????????
        List<DeviceCustomerUserRelationPo> deviceCustomerUserRelationPoList = this.deviceCustomerUserRelationMapper.queryByDeviceId(deviceId);
        //
        if (null != deviceCustomerUserRelationPoList && 0 < deviceCustomerUserRelationPoList.size()) {
            //???????????????
            DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = deviceCustomerUserRelationPoList.get(0);
            CustomerUserPo masterUserPo = this.customerUserMapper.selectByOpenId(deviceCustomerUserRelationPo.getOpenId());
            DeviceShareListVo masterPo = new DeviceShareListVo();
            //??????????????????
            masterPo.setUserId(masterUserPo.getId());
            masterPo.setOpenId(masterUserPo.getOpenId());
            masterPo.setHeadImg(masterUserPo.getHeadimgurl());
            masterPo.setNickname(masterUserPo.getNickname());
            DeviceTeamItemPo masterTeamItemPo = this.deviceTeamItemMapper.selectByJoinId(deviceId, masterUserPo.getId());
            masterPo.setJoinTime(masterTeamItemPo.getCreateTime());
            deviceShareListVoList.add(masterPo);
            //??????????????????????????????,??????????????????
            List<DeviceTeamItemPo> deviceTeamItemPoList = this.deviceTeamItemMapper.selectItemsByDeviceId(deviceId);
            List<DeviceTeamItemPo> subUserTeamItemPoList = deviceTeamItemPoList.stream().filter(eachPo -> {
                List<String> openIdsList = deviceCustomerUserRelationPoList.stream().map(userEachPo -> userEachPo.getOpenId()).collect(Collectors.toList());
                Integer subUserId = eachPo.getUserId();
                CustomerUserPo customerUserPo = this.customerUserMapper.selectById(subUserId);
                if (!openIdsList.contains(customerUserPo.getOpenId())) {
                    return true;
                }
                return false;
            }).sorted(Comparator.comparing(DeviceTeamItemPo::getCreateTime)).collect(Collectors.toList());
            //??????????????????
            List<DeviceShareListVo> subUserVo = subUserTeamItemPoList.stream().map(eachPo -> {
                Integer subUserId = eachPo.getUserId();
                CustomerUserPo customerUserPo = this.customerUserMapper.selectByUserId(subUserId);
                DeviceShareListVo deviceShareVo = new DeviceShareListVo();
                deviceShareVo.setUserId(customerUserPo.getId());
                deviceShareVo.setNickname(customerUserPo.getNickname());
                deviceShareVo.setJoinTime(eachPo.getCreateTime());
                deviceShareVo.setOpenId(customerUserPo.getOpenId());
                deviceShareVo.setHeadImg(customerUserPo.getHeadimgurl());
                deviceShareVo.setStatus(eachPo.getStatus() == 1 ? true : false);
                return deviceShareVo;
            }).collect(Collectors.toList());
            return new ApiResponse<>(RetCode.OK, "????????????????????????", subUserVo);
        } else {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
        }
    }

    public ApiResponse<Boolean> updateRelation(UpdateShareRequest updateShareRequest) throws Exception {
        //??????????????????
        DevicePo devicePo = this.deviceMapper.selectById(updateShareRequest.getDeviceId());
        if (null == devicePo) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????????????????");
        }
        //???????????????????????????
        DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = this.deviceCustomerUserRelationMapper.selectByDeviceId(updateShareRequest.getDeviceId());
        if (null == deviceCustomerUserRelationPo) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
        }
        //????????????????????????
        CustomerUserPo customerUserPo = this.customerUserMapper.selectByOpenId(updateShareRequest.getOpenId());
        if (null == customerUserPo) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
        }
        DeviceTeamItemPo queryItemPo = new DeviceTeamItemPo();
        queryItemPo.setUserId(customerUserPo.getId());
        queryItemPo.setDeviceId(updateShareRequest.getDeviceId());
        queryItemPo.setStatus(null);
        List<DeviceTeamItemPo> deviceTeamItemPoList = deviceTeamMapper.queryTeamItems(queryItemPo);
        if (null == deviceTeamItemPoList || 0 == deviceTeamItemPoList.size()) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????????????????");
        }
        DeviceTeamItemPo deviceTeamItemPo = deviceTeamItemPoList.get(0);
        this.deviceTeamItemMapper.updateStatus(deviceTeamItemPo.getDeviceId(), deviceTeamItemPo.getUserId(), updateShareRequest.getStatus());
        return new ApiResponse<>(RetCode.OK, "????????????", true);
    }

    public ApiResponse<Boolean> updateAllRelation(UpdateShareRequest updateShareRequest) throws Exception {
        List<DeviceTeamItemPo> deviceTeamItemPoList = deviceTeamItemMapper.selectItemsByDeviceId(updateShareRequest.getDeviceId());
        if (null == deviceTeamItemPoList || 0 == deviceTeamItemPoList.size()) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????????????????");
        }
        List<DeviceCustomerUserRelationPo> deviceCustomerUserRelationPoList = deviceCustomerUserRelationMapper.queryByDeviceId(updateShareRequest.getDeviceId());
        if (null == deviceCustomerUserRelationPoList || 0 == deviceCustomerUserRelationPoList.size()) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
        }
        List<DeviceTeamItemPo> subUserTeamItemPoList = deviceTeamItemPoList.stream().filter(eachPo -> {
            List<String> openIdsList = deviceCustomerUserRelationPoList.stream().map(deviceCustomerUserRelationPo -> deviceCustomerUserRelationPo.getOpenId()).collect(Collectors.toList());
            Integer userId = eachPo.getUserId();
            CustomerUserPo customerUserPo = customerUserMapper.selectById(userId);
            if (!openIdsList.contains(customerUserPo.getOpenId())) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        //????????????????????????????????????????????????????????????
        subUserTeamItemPoList.stream().forEach(eachPo -> {
            eachPo.setStatus(updateShareRequest.getStatus());
        });
        this.deviceTeamItemMapper.updateBatch(subUserTeamItemPoList);
        return new ApiResponse<>(RetCode.OK, "??????????????????", true);
    }

    public ApiResponse<Boolean> clearRelation(UpdateShareRequest updateShareRequest) throws Exception {
        DeviceTeamItemPo queryItemPo = new DeviceTeamItemPo();
        CustomerUserPo beClearCustomerUserPo = customerUserMapper.selectByOpenId(updateShareRequest.getOpenId());
        queryItemPo.setUserId(beClearCustomerUserPo.getId());
        queryItemPo.setDeviceId(updateShareRequest.getDeviceId());
        queryItemPo.setStatus(null);
        List<DeviceTeamItemPo> deviceTeamItemPoList = deviceTeamMapper.queryTeamItems(queryItemPo);
        if (deviceTeamItemPoList.size() == 0 || null == deviceTeamItemPoList) {
            log.info("??????????????????????????????deviceId={}", updateShareRequest.getDeviceId());
            return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????????????????");
        }
        DeviceTeamItemPo deviceTeamItemPo = deviceTeamItemPoList.get(0);
        this.deviceTeamItemMapper.deleteByJoinId(deviceTeamItemPo.getDeviceId(), deviceTeamItemPo.getUserId());
        return new ApiResponse<>(RetCode.OK, "??????????????????", true);
    }

    public ApiResponse<Boolean> clearAllRelation(UpdateShareRequest updateShareRequest) throws Exception {
        DeviceTeamItemPo queryItemPo = new DeviceTeamItemPo();
        //???????????????????????????
        List<DeviceCustomerUserRelationPo> deviceCustomerUserRelationPoList = deviceCustomerUserRelationMapper.queryByDeviceId(updateShareRequest.getDeviceId());
        if (null == deviceCustomerUserRelationPoList || 0 == deviceCustomerUserRelationPoList.size()) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
        }
        queryItemPo.setDeviceId(updateShareRequest.getDeviceId());
        queryItemPo.setStatus(null);
        List<DeviceTeamItemPo> deviceTeamItemPoList = deviceTeamMapper.queryTeamItems(queryItemPo);
        if (null != deviceTeamItemPoList && 0 < deviceTeamItemPoList.size()) {
            //?????????????????????????????????
            List<DeviceTeamItemPo> subUserTeamItemPoList = deviceTeamItemPoList.stream().filter(eachPo -> {
                List<String> openIdsList = deviceCustomerUserRelationPoList.stream().map(deviceCustomerUserRelationPo -> deviceCustomerUserRelationPo.getOpenId()).collect(Collectors.toList());
                Integer userId = eachPo.getUserId();
                CustomerUserPo customerUserPo = customerUserMapper.selectById(userId);
                if (!openIdsList.contains(customerUserPo.getOpenId())) {
                    return true;
                }
                return false;
            }).collect(Collectors.toList());
            this.deviceTeamItemMapper.deleteBatch(subUserTeamItemPoList);
        } else {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????????????????");
        }
        return new ApiResponse<>(RetCode.OK, "??????????????????????????????", true);
    }

    /**
     * ??????????????????????????????
     *
     * @param deviceId
     * @return
     * @throws Exception
     */
    public ApiResponse<List<DevicePo>> queryChildDevice(Integer deviceId) throws Exception {
        List<DevicePo> devicePoList = this.deviceMapper.selectChildDeviceListByHostDeviceId(deviceId);
        if (null != devicePoList && 0 < devicePoList.size()) {
            return new ApiResponse<>(RetCode.OK, "?????????????????????", devicePoList);
        } else {
            return new ApiResponse<>(RetCode.OK, "????????????????????????", null);
        }
    }

    /**
     * ????????????
     *
     * @param customerId
     * @return
     */
    public List<CustomerUserPo> queryUser(Integer customerId) {
        List<CustomerUserPo> customerUserPoList = this.customerUserMapper.selectByCustomerId(customerId);
        return customerUserPoList;
    }


    /**
     * ??????????????????
     *
     * @param deviceId
     * @return
     * @throws Exception
     */
    public ApiResponse<DeviceLocationVo> queryDeviceLocation(Integer deviceId) throws Exception {
        DeviceLocationVo locationVo = new DeviceLocationVo();
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        if (devicePo != null) {
            if (StringUtils.isEmpty(devicePo.getLocation())) {
                JSONObject locationJson = locationUtils.getLocation(devicePo.getIp(), true);
                if (locationJson != null) {
                    if (locationJson.containsKey("content")) {
                        JSONObject content = locationJson.getJSONObject("content");
                        if (content != null) {
                            if (content.containsKey("address_detail")) {
                                JSONObject addressDetail = content.getJSONObject("address_detail");
                                if (addressDetail != null) {
                                    locationVo.setProvince(addressDetail.getString("province"));
                                    locationVo.setCity(addressDetail.getString("city"));
                                    locationVo.setArea(locationVo.getCity());
                                    locationVo.setLocation(locationVo.getProvince() + "," + locationVo.getCity());
                                }
                            }
                            if (content.containsKey("point")) {
                                JSONObject point = content.getJSONObject("point");
                                if (point != null) {
                                    locationVo.setPointX(point.getString("x"));
                                    locationVo.setPointY(point.getString("y"));
                                }
                            }

                        }
                    }
                }
            } else {
                String[] locationArray = devicePo.getLocation().split(",");
                locationVo.setArea(Joiner.on(" ").join(locationArray));
                locationVo.setLocation(devicePo.getLocation());
            }
            return new ApiResponse<>(RetCode.OK, "????????????????????????", locationVo);
        } else {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????", null);
        }

    }

    public ApiResponse<List<DeviceLocationVo>> queryDeviceLocationInGroup(List<Integer> deviceIdList) throws Exception {
        List<DeviceLocationVo> deviceLocationVoList = new ArrayList<>();
        for(Integer deviceId : deviceIdList){
            DeviceLocationVo deviceLocationVo = new DeviceLocationVo();
            ApiResponse<DeviceLocationVo> deviceLocationVoRtn = queryDeviceLocation(deviceId);
            if(RetCode.OK == deviceLocationVoRtn.getCode()){
                deviceLocationVo = deviceLocationVoRtn.getData();
            }else {
                deviceLocationVo.setArea("????????????????????????");
                deviceLocationVo.setLocation("????????????????????????");
            }
            deviceLocationVoList.add(deviceLocationVo);
        }
        return new ApiResponse<>(RetCode.OK,"?????????????????????????????????",deviceLocationVoList);
    }

    /**
     * ????????????
     *
     * @param deviceId
     * @return
     */
    public ApiResponse<DeviceWeatherVo> queryDeviceWeather(Integer deviceId) {
        DeviceWeatherVo deviceWeatherVo = new DeviceWeatherVo();
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        JSONObject weatherJson = locationUtils.getWeather(devicePo.getIp(), true);
        if (weatherJson != null) {
            if (weatherJson.containsKey("result")) {
                JSONObject result = weatherJson.getJSONObject("result");
                if (result != null) {
                    deviceWeatherVo.setOuterHum(result.getString("humidity"));
                    deviceWeatherVo.setOuterPm(result.getString("aqi"));
                    deviceWeatherVo.setOuterTem(result.getString("temperature_curr"));
                    deviceWeatherVo.setWeather(result.getString("weather_curr"));
                }
            }
        }
        return new ApiResponse<>(RetCode.OK, "????????????????????????", deviceWeatherVo);
    }


    /**
     * 2018-08-20
     * sixiaojun
     * ??????mac??????????????????????????????????????????mac????????????????????????????????????DevicePo???????????????
     *
     * @param deviceList
     * @return devicePo
     */
    public DevicePo queryDeviceByMac(List<DeviceCreateOrUpdateRequest.DeviceUpdateList> deviceList) {
        DevicePo devicePo = null;
        for (DeviceCreateOrUpdateRequest.DeviceUpdateList device : deviceList) {
            devicePo = deviceMapper.selectByMac(device.getMac());
            if (null != devicePo && CommonConstant.STATUS_DEL != devicePo.getStatus()) {
                return devicePo;
            } else {
                devicePo = null;
            }
        }
        return devicePo;
    }

    public DevicePo queryDeviceByName(List<DeviceCreateOrUpdateRequest.DeviceUpdateList> deviceList) {
        DevicePo devicePo = null;
        for (DeviceCreateOrUpdateRequest.DeviceUpdateList device : deviceList) {
            devicePo = deviceMapper.selectByName(device.getName());
            if (null != devicePo && CommonConstant.STATUS_DEL != devicePo.getStatus()) {
                return devicePo;
            } else {
                devicePo = null;
            }
        }
        return devicePo;
    }

    /**
     * 2018-08-20
     * sixiaojun
     * ??????????????????????????????mac????????????????????????????????????
     *
     * @param deviceList
     * @return
     */
    public DevicePo isDeviceHasCustomer(List<DeviceQueryRequest.DeviceQueryList> deviceList) throws Exception {
        for (DeviceQueryRequest.DeviceQueryList device : deviceList) {
            DevicePo devicePo = deviceMapper.selectDeviceCustomerRelationByMac(device.getMac());
            //?????????????????????????????????????????????
            if (null != devicePo) {
                return devicePo;
            }
        }
        return null;
    }

    /**
     * ??????????????????????????????????????????????????????????????????
     *
     * @param openId
     * @return
     * @throws Exception
     */
    public ApiResponse<List<DeviceTeamPo>> queryTeamInfoByUser(String openId) throws Exception {
        //?????????????????????????????????????????????
        //?????????????????????????????????????????????
        Integer customerId = this.customerService.obtainCustomerId(false);
        List<DeviceTeamPo> deviceTeamPoList = this.deviceTeamMapper.selectByUserOpenId(openId,customerId);
        DeviceTeamPo deviceTeamPo = new DeviceTeamPo();
        CustomerUserPo customerUserPo = this.customerUserMapper.selectByOpenId(openId);
        if (null == deviceTeamPoList || 0 == deviceTeamPoList.size()) {
            deviceTeamPoList.clear();
            //???????????????????????????????????????
            WxConfigPo wxConfigPo = this.wxConfigMapper.selectConfigByCustomerId(customerUserPo.getCustomerId());
            if (wxConfigPo != null) {
                deviceTeamPo.setName(wxConfigPo.getDefaultTeamName());
                deviceTeamPo.setId(DeviceConstant.DEFAULT_TEAM_ID);
                deviceTeamPoList.add(deviceTeamPo);
            }

        }
        return new ApiResponse<>(RetCode.OK, "?????????????????????", deviceTeamPoList);
    }


    public CustomerUserPo isUserExist(String openId) {
        return this.customerUserMapper.selectByOpenId(openId);
    }

    /**
     * ???deviceIdPool ?????????????????????
     *
     * @param customerId
     * @param productId
     * @param addCount
     * @return
     */
    public ApiResponse<Integer> createWxDeviceIdPools(Integer customerId, String productId, Integer addCount) {
        Boolean ret = true;
        CustomerPo customerPo = customerMapper.selectById(customerId);
        int correctCount = 0;
        //????????????
        if (customerPo != null) {
            String appId = customerPo.getAppid();
            String appSecret = customerPo.getAppsecret();
            if (null != addCount && addCount > 0) {
                List<DeviceIdPoolPo> deviceIdPoolPos = new ArrayList<>();

                for (int m = 0; m < addCount; m++) {
                    ApiResponse<JSONObject> result = obtainDeviceInfo(appId, appSecret, customerId.toString(), productId);
                    //????????????????????? ???????????????????????????????????????
                    if (m == 0 && RetCode.OK != result.getCode()) {
                        return new ApiResponse<>(RetCode.ERROR, result.getMsg(), correctCount);
                    }
                    JSONObject jsonObject = result.getData();
                    if (jsonObject != null) {
                        String wxDeviceId = jsonObject.getString("deviceid");
                        String wxDevicelicence = jsonObject.getString("devicelicence");
                        String wxQrticket = jsonObject.getString("qrticket");

                        if (wxDeviceId != null) {
                            DeviceIdPoolPo insertPo = new DeviceIdPoolPo();

                            insertPo.setCustomerId(customerId);
                            insertPo.setProductId(productId);
                            insertPo.setWxDeviceId(wxDeviceId);
                            insertPo.setWxDeviceLicence(wxDevicelicence);
                            insertPo.setWxQrticket(wxQrticket);
                            insertPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);
                            insertPo.setCreateTime(System.currentTimeMillis());
                            insertPo.setLastUpdateTime(System.currentTimeMillis());

                            deviceIdPoolPos.add(insertPo);

                            correctCount++;
                        } else {
                            log.error("wxDeviceId ??? wxDevicelicence?????? = {}", jsonObject);
                        }

                    } else {
                        log.error("createWxDeviceIdPool.jsonObject = {}", false);
                    }

                }

                if (deviceIdPoolPos != null && deviceIdPoolPos.size() > 0) {
                    ret = deviceIdPoolMapper.insertBatch(deviceIdPoolPos) > 0;
                } else {
                    return new ApiResponse<>(RetCode.ERROR, "????????????DeviceId??????");
                }

            } else {
                return new ApiResponse<>(RetCode.ERROR, "???????????????");
            }
        }


        return new ApiResponse<>(correctCount);
    }

    /**
     * ???deviceIdPool ??????????????????
     *
     * @param customerId
     * @param productId
     * @return
     */
    public Boolean createWxDeviceIdPool(Integer customerId, String productId) {
        CustomerPo customerPo = customerMapper.selectById(customerId);
        //????????????
        String appId = customerPo.getAppid();
        String appSecret = customerPo.getAppsecret();

        JSONObject jsonObject = obtainDeviceJson(appId, appSecret, customerId.toString(), productId).getData();
        if (jsonObject != null) {
            String wxDeviceId = jsonObject.getString("deviceid");
            String wxDevicelicence = jsonObject.getString("devicelicence");
            String wxQrticket = jsonObject.getString("qrticket");
            DeviceIdPoolPo insertPo = new DeviceIdPoolPo();

            insertPo.setCustomerId(customerId);
            insertPo.setProductId(productId);
            insertPo.setWxDeviceId(wxDeviceId);
            insertPo.setWxDeviceLicence(wxDevicelicence);
            insertPo.setWxQrticket(wxQrticket);
            insertPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);
            insertPo.setCreateTime(System.currentTimeMillis());
            insertPo.setLastUpdateTime(System.currentTimeMillis());

            int insertRet = deviceIdPoolMapper.insert(insertPo);
            log.info("createWxDeviceIdPool.insertRet = {}", insertRet);
            return true;
        } else {
            log.info("createWxDeviceIdPool.jsonObject = {}", false);
            return false;
        }

    }

    private ApiResponse<JSONObject> obtainDeviceJson(String appId, String appSecret, String customerId, String productId) {
        ApiResponse<JSONObject> result = obtainDeviceInfo(appId, appSecret, customerId, productId);
        if (RetCode.PARAM_ERROR == result.getCode()) {
            return result;
        }
        JSONObject deviceInfo = result.getData();
        if (deviceInfo == null) {
            wechartUtil.getAccessToken(appId, appSecret, customerId, true);
            deviceInfo = obtainDeviceInfo(appId, appSecret, customerId, productId).getData();
        }
        if (deviceInfo != null) {
            return new ApiResponse<>(deviceInfo);
        }
        return new ApiResponse<>();
    }

    private ApiResponse<JSONObject> obtainDeviceInfo(String appId, String appSecret, String customerId, String productId) {
        String accessToken = wechartUtil.getAccessToken(appId, appSecret, customerId, false);
        if (StringUtils.isBlank(accessToken)) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "???appId???appSecret????????????accessToken");
        }
        String url = new StringBuilder("https://api.weixin.qq.com/device/getqrcode?access_token=").append(accessToken).append("&product_id=").append(productId).toString();
        HttpGet httpGet = new HttpGet();
        try {
            httpGet.setURI(new URI(url));
            CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            log.info("result = {}", result);
            JSONObject jsonObject = JSON.parseObject(result.toString());
            //???????????? ??????id ??????
            if (jsonObject != null) {
                //?????? ?????? access_token????????????????????????access_token?????????????????????
                if (jsonObject.containsKey("errcode") && CommonConstant.ZERO != jsonObject.get("errcode")) {
                    if (RetCode.WX_RROR_40001.equals(jsonObject.get("errcode")) || RetCode.WX_RROR_40001 == jsonObject.get("errcode")) {
                        accessToken = wechartUtil.getAccessToken(appId, appSecret, customerId, true);
                        obtainDeviceInfo(appId, appSecret, customerId, productId);
                    }
                    return new ApiResponse<>(RetCode.PARAM_ERROR, result.toString(), jsonObject);
                }
                JSONObject resultObject = jsonObject.getJSONObject("base_resp");
                if (resultObject != null && resultObject.containsKey("errcode")) {
                    Integer retCode = resultObject.getInteger("errcode");
                    if (retCode != null && retCode.equals(0)) {
                        return new ApiResponse<>(RetCode.OK, "", jsonObject);
                    } else {
                        return new ApiResponse<>(RetCode.PARAM_ERROR, result.toString(), jsonObject);
                    }
                }
            }

        } catch (Exception e) {
            log.error("", e);
        }
        return new ApiResponse<>(RetCode.PARAM_ERROR, "????????????????????????", null);
    }


    /**
     * ????????????????????????
     *
     * @param abilityIds
     * @return
     */
    public List<DeviceAbilityVo.DeviceAbilitysVo> queryDetailAbilitysValue(Integer deviceId, List<Integer> abilityIds) {
        Integer modelId = deviceMapper.selectById(deviceId).getModelId();
        List<DeviceAbilityVo.DeviceAbilitysVo> deviceAbilitysVoList = new ArrayList<>();
        Map<Object, Object> datas = stringRedisTemplate.opsForHash().entries("sensor2." + deviceId);
        Map<Object, Object> controlDatas = stringRedisTemplate.opsForHash().entries("control2." + deviceId);
        //??????????????????
//        List<DeviceAbilityPo> deviceAbilityPoCaches = deviceAbilityMapper.selectList(new DeviceAbilityPo(), 10000, 0);
//        Map<Integer,DeviceAbilityPo> deviceAbilityPoMap = deviceAbilityPoCaches.stream().collect(Collectors.toMap(DeviceAbilityPo::getId, a -> a,(k1, k2)->k1));
        for (Integer abilityId : abilityIds) {
            List<DeviceModelAbilityOptionPo> deviceModelAbilityOptionPoCaches = new ArrayList<>();
            Map<Integer, DeviceModelAbilityOptionPo> deviceModelAbilityOptionPoMap = new HashMap<>();
            DeviceAbilityPo deviceabilityPo = deviceAbilityMapper.selectById(abilityId);
            String dirValue = deviceabilityPo.getDirValue();
            Integer abilityType = deviceabilityPo.getAbilityType();

            DeviceAbilityVo.DeviceAbilitysVo deviceAbilitysVo = new DeviceAbilityVo.DeviceAbilitysVo();
            deviceAbilitysVo.setAbilityName(deviceabilityPo.getAbilityName());
            deviceAbilitysVo.setId(abilityId);
            deviceAbilitysVo.setAbilityType(abilityType);
            deviceAbilitysVo.setDirValue(dirValue);
            switch (abilityType) {
                case DeviceAbilityTypeContants.ability_type_text:
                    deviceAbilitysVo.setCurrValue(getData(datas, dirValue));
                    deviceAbilitysVo.setUnit(deviceabilityPo.getRemark());
                    break;
                case DeviceAbilityTypeContants.ability_type_single:
                    List<DeviceAbilityOptionPo> deviceabilityOptionPos = deviceAbilityOptionMapper.selectActiveOptionsByAbilityId(abilityId);

                    //??????
                    deviceModelAbilityOptionPoCaches = deviceModelAbilityOptionMapper.queryByModelIdAbilityId(modelId ,abilityId );
                    deviceModelAbilityOptionPoMap = deviceModelAbilityOptionPoCaches.stream().collect(Collectors.toMap(DeviceModelAbilityOptionPo::getAbilityOptionId, a -> a, (k1, k2) -> k1));
                    String optionValue = getData(controlDatas, dirValue);
                    List<DeviceAbilityVo.abilityOption> abilityOptionList = new ArrayList<>();
                    for (DeviceAbilityOptionPo deviceabilityOptionPo : deviceabilityOptionPos) {
                        DeviceModelAbilityOptionPo deviceModelAbilityOptionPo = deviceModelAbilityOptionPoMap.get(deviceabilityOptionPo.getId());
                        if(deviceModelAbilityOptionPo == null){
                            continue;
                        }
                        DeviceAbilityVo.abilityOption abilityOption = new DeviceAbilityVo.abilityOption();
                        abilityOption.setDirValue(deviceabilityOptionPo.getOptionValue());
                        if ((StringUtils.isNotEmpty(deviceModelAbilityOptionPo.getActualOptionValue())&&optionValue.equals(deviceModelAbilityOptionPo.getActualOptionValue()))
                                ||(StringUtils.isEmpty(deviceModelAbilityOptionPo.getActualOptionValue())&&optionValue.equals(deviceabilityOptionPo.getOptionValue()))) {
                            deviceAbilitysVo.setCurrValue(deviceabilityOptionPo.getOptionValue());
                            abilityOption.setIsSelect(CommonConstant.STATUS_IS_YES);
                        } else {
                            abilityOption.setIsSelect(CommonConstant.STATUS_IS_NO);
                        }
                        abilityOptionList.add(abilityOption);
                    }
                    deviceAbilitysVo.setAbilityOptionList(abilityOptionList);
                    break;
                case DeviceAbilityTypeContants.ability_type_checkbox:
                    List<DeviceAbilityOptionPo> deviceabilityOptionPos1 = deviceAbilityOptionMapper.selectOptionsByAbilityId(abilityId);
                    //??????
                    deviceModelAbilityOptionPoCaches = deviceModelAbilityOptionMapper.queryByModelIdAbilityId(modelId ,abilityId );
                    deviceModelAbilityOptionPoMap = deviceModelAbilityOptionPoCaches.stream().collect(Collectors.toMap(DeviceModelAbilityOptionPo::getAbilityOptionId, a -> a, (k1, k2) -> k1));
                    List<DeviceAbilityVo.abilityOption> abilityOptionList1 = new ArrayList<>();
                    for (DeviceAbilityOptionPo deviceabilityOptionPo : deviceabilityOptionPos1) {
                        DeviceModelAbilityOptionPo deviceModelAbilityOptionPo = deviceModelAbilityOptionPoMap.get(deviceabilityOptionPo.getId());
                        if(deviceModelAbilityOptionPo == null){
                            continue;
                        }
                        String targetOptionValue = deviceabilityOptionPo.getOptionValue();
                        String finalOptionValue = getData(controlDatas, targetOptionValue);
                        DeviceAbilityVo.abilityOption abilityOption = new DeviceAbilityVo.abilityOption();
                        abilityOption.setDirValue(deviceabilityOptionPo.getOptionValue());
                        if (Integer.valueOf(finalOptionValue) == 1) {
                            abilityOption.setIsSelect(CommonConstant.STATUS_IS_YES);
                        } else {
                            abilityOption.setIsSelect(CommonConstant.STATUS_IS_NO);
                        }
                        abilityOptionList1.add(abilityOption);
                    }
                    deviceAbilitysVo.setAbilityOptionList(abilityOptionList1);
                    break;
                case DeviceAbilityTypeContants.ability_type_threshhold:
                    deviceAbilitysVo.setCurrValue(getData(controlDatas, dirValue));
                    deviceAbilitysVo.setUnit(deviceabilityPo.getRemark());
                    break;
                case DeviceAbilityTypeContants.ability_type_threshholdselect:
                    DeviceAbilityPo deviceAbilityPo = deviceAbilityMapper.selectById(abilityId);
                    if (deviceAbilityPo.getDirValue().equals("-1")) {//???????????????????????????????????????????????????
                        List<DeviceAbilityOptionPo> deviceabilityOptionPos5 = deviceAbilityOptionMapper.selectOptionsByAbilityId(abilityId);
                        List<DeviceAbilityVo.abilityOption> abilityOptionList5 = new ArrayList<>();
                        for (DeviceAbilityOptionPo deviceabilityOptionPo : deviceabilityOptionPos5) {
                            String optionValue5 = getData(controlDatas, deviceabilityOptionPo.getOptionValue());
                            DeviceAbilityVo.abilityOption abilityOption = new DeviceAbilityVo.abilityOption();
                            abilityOption.setDirValue(deviceabilityOptionPo.getOptionValue());
                            abilityOption.setCurrValue(optionValue5);
                            abilityOptionList5.add(abilityOption);
                        }
                        deviceAbilitysVo.setAbilityOptionList(abilityOptionList5);
                    } else {
                        List<DeviceAbilityOptionPo> deviceabilityOptionPos5 = deviceAbilityOptionMapper.selectOptionsByAbilityId(abilityId);
                        //??????
                        deviceModelAbilityOptionPoCaches = deviceModelAbilityOptionMapper.queryByModelIdAbilityId(modelId ,abilityId );
                        deviceModelAbilityOptionPoMap = deviceModelAbilityOptionPoCaches.stream().collect(Collectors.toMap(DeviceModelAbilityOptionPo::getAbilityOptionId, a -> a, (k1, k2) -> k1));
                        String optionValue5 = getData(controlDatas, dirValue);
                        List<DeviceAbilityVo.abilityOption> abilityOptionList5 = new ArrayList<>();
                        for (DeviceAbilityOptionPo deviceabilityOptionPo : deviceabilityOptionPos5) {
                            DeviceModelAbilityOptionPo deviceModelAbilityOptionPo = deviceModelAbilityOptionPoMap.get(deviceabilityOptionPo.getId());
                            if(deviceModelAbilityOptionPo == null){
                                continue;
                            }
                            DeviceAbilityVo.abilityOption abilityOption = new DeviceAbilityVo.abilityOption();
                            abilityOption.setDirValue(deviceabilityOptionPo.getOptionValue());
                            if (optionValue5.equals(deviceabilityOptionPo.getOptionValue())) {
                                abilityOption.setIsSelect(CommonConstant.STATUS_IS_YES);
                            } else {
                                abilityOption.setIsSelect(CommonConstant.STATUS_IS_NO);
                            }
                            abilityOptionList5.add(abilityOption);
                        }
                        deviceAbilitysVo.setAbilityOptionList(abilityOptionList5);
                    }
                    break;
                default:
                    break;

            }
            deviceAbilitysVoList.add(deviceAbilitysVo);
        }
//        //????????????????????????
//        if (datas.containsKey(SensorTypeEnums.PM25_IN.getCode())) {
//            DeviceAbilityVo.DeviceAbilitysVo deviceAbilitysVo = new DeviceAbilityVo.DeviceAbilitysVo();
//            deviceAbilitysVo.setDirValue("0");
//            deviceAbilitysVo.setAbilityName("????????????");
//
//            String data = getData(datas, SensorTypeEnums.PM25_IN.getCode());
//            if (StringUtils.isNotEmpty(data)) {
//                Integer diData = Integer.valueOf(data);
//                if (diData >= 0 && diData <= 35) {
//                    deviceAbilitysVo.setCurrValue("???");
//                } else if (diData > 35 && diData <= 75) {
//                    deviceAbilitysVo.setCurrValue("???");
//                } else if (diData > 75 && diData <= 150) {
//                    deviceAbilitysVo.setCurrValue("???");
//                } else {
//                    deviceAbilitysVo.setCurrValue("???");
//                }
//            } else {
//                deviceAbilitysVo.setCurrValue("???");
//            }
//            deviceAbilitysVoList.add(deviceAbilitysVo);
//        }
        return deviceAbilitysVoList;
    }

    private String getData(Map<Object, Object> map, String key) {
        if (map.containsKey(key)) {
            return (String) map.get(key);
        }
        return "0";
    }


    public ApiResponse<Boolean> sendFuncs(DeviceFuncRequest deviceFuncVo, Integer operType){
        //???????????????????????????
        DevicePo devicePo = deviceMapper.selectById(deviceFuncVo.getDeviceId());
        if(null != devicePo){
            //?????????????????????????????????
            if(devicePo.getEnableStatus() == DeviceConstant.ENABLE_STATUS_NO){
                throw new BusinessException("?????????????????????");
            }
            //???????????????????????????
            sendFunc(deviceFuncVo,operType);
            //???????????????????????????????????????
            DeviceTeamItemPo deviceTeamItemPo = this.deviceTeamItemMapper.selectByDeviceId(devicePo.getId());
            if(null != deviceTeamItemPo && deviceTeamItemPo.getLinkAgeStatus().equals(1)){
                //?????????????????????????????????
                List<DeviceTeamItemPo> deviceTeamItemPoList = this.deviceTeamItemMapper.selectLinkDevice(deviceTeamItemPo);
                for (DeviceTeamItemPo eachPo : deviceTeamItemPoList) {
                    DeviceFuncRequest deviceFuncRequest = new DeviceFuncRequest();
                    deviceFuncRequest.setDeviceId(eachPo.getDeviceId());
                    deviceFuncRequest.setFuncId(deviceFuncVo.getFuncId());
                    deviceFuncRequest.setValue(deviceFuncVo.getValue());
                    String requestId = sendFunc(deviceFuncRequest, operType);
                }
            }
            return new ApiResponse<>(RetCode.OK,"??????????????????",true);
        }
        return new ApiResponse<>(RetCode.ERROR,"??????????????????",false);
    }

    /**
     * ??????????????????
     * @param deviceFuncVo
     * @param operType
     * @return
     */
    public String sendFunc (DeviceFuncRequest deviceFuncVo, Integer operType) {

        //???????????????????????????
        User user = this.userService.getCurrentUser();

        DevicePo devicePo = deviceMapper.selectById(deviceFuncVo.getDeviceId());
        if (devicePo != null) {
            //???????????????????????????????????????
            List<DeviceAbilityPo> deviceAbilityPos = deviceModelAbilityMapper.selectActiveByModelId(devicePo.getModelId());
            List<DeviceAbilityOptionPo> deviceAbilityOptionPos = new ArrayList<>();
            List<DeviceModelAbilityOptionPo> deviceModelAbilityOptionPos = new ArrayList<>();
            deviceAbilityPos.stream().filter(temp ->{return temp.getDirValue().equals(deviceFuncVo.getFuncId());}).forEach(deviceAbilityPo-> {
                deviceAbilityOptionPos.addAll(deviceAbilityOptionMapper.selectActiveOptionsByAbilityId(deviceAbilityPo.getId()));
                deviceModelAbilityOptionPos.addAll(deviceModelAbilityOptionMapper.queryByModelIdAbilityId(devicePo.getModelId(), deviceAbilityPo.getId()));
            });
            Integer optionId = null;
            String actualValue = deviceFuncVo.getValue();
            for (DeviceAbilityOptionPo temp : deviceAbilityOptionPos){
                if (deviceFuncVo.getValue().equals(temp.getOptionValue())){
                    optionId = temp.getId();
                    break;
                }
            }
            for (DeviceModelAbilityOptionPo temp : deviceModelAbilityOptionPos){
                if (temp.getAbilityOptionId().equals(optionId)&&StringUtils.isNotEmpty(temp.getActualOptionValue())){
                    actualValue = temp.getActualOptionValue();
                    break;
                }
            }
            //????????????

            Integer hostDeviceId = devicePo.getHostDeviceId()==null||devicePo.getHostDeviceId()==0?devicePo.getId():devicePo.getHostDeviceId();
            String topic = "/down2/control/" + hostDeviceId;
            String requestId = UUID.randomUUID().toString().replace("-", "");
            /*????????????*/
            DeviceOperLogPo deviceOperLogPo = new DeviceOperLogPo();
            deviceOperLogPo.setFuncId(deviceFuncVo.getFuncId());
            deviceOperLogPo.setDeviceId(devicePo.getId());
            deviceOperLogPo.setOperType(operType);
            deviceOperLogPo.setOperUserId(user.getId());
            deviceOperLogPo.setFuncValue(deviceFuncVo.getValue());
            deviceOperLogPo.setRequestId(requestId);
            deviceOperLogPo.setCreateTime(System.currentTimeMillis());
            deviceOperLogMapper.insert(deviceOperLogPo);

            FuncListMessage funcListMessage = new FuncListMessage();
            funcListMessage.setMsg_type("control");
            funcListMessage.setMsg_id(requestId);
            FuncListMessage.FuncItemMessage funcItemMessage = new FuncListMessage.FuncItemMessage();
            funcItemMessage.setType(deviceFuncVo.getFuncId());
            funcItemMessage.setValue(actualValue);
            funcItemMessage.setChildid(devicePo.getChildId());
            funcListMessage.setDatas(Lists.newArrayList(funcItemMessage));
            mqttSendService.sendMessage(topic, JSON.toJSONString(funcListMessage));
            stringRedisTemplate.opsForHash().put("control2." + devicePo.getId(), funcItemMessage.getType(), String.valueOf(funcItemMessage.getValue()));
            return requestId;
        }else{
            return "??????????????????";
        }


    }

    /**
     * ?????? ????????? ????????????????????????????????? ????????????
     */
    public List<DeviceModelAbilityVo> getModelVo(Integer deviceId) {

        List<DeviceModelAbilityVo> deviceModelAbilityVos = new ArrayList<>();
        DevicePo devicePo = deviceMapper.selectById(deviceId);

        if (devicePo == null) {
            return null;
        }
        if (devicePo.getModelId() == null) {
            return null;
        }

        //?????? ??????????????????????????????
        List<DeviceTypeAbilitysPo> deviceTypeAbilitysPos = deviceTypeAbilitysMapper.selectByTypeId(devicePo.getTypeId());
        //?????????????????????????????? ?????????
        List<DeviceModelAbilityPo> deviceModelAbilityPos = deviceModelAbilityMapper.selectByModelId(devicePo.getModelId());

        Map<Integer, DeviceModelAbilityPo> deviceModelAbilityPoMap = deviceModelAbilityPos.stream().collect(Collectors.toMap(DeviceModelAbilityPo::getAbilityId, a -> a, (k1, k2) -> k1));

        /* ??? ??????????????????????????? ?????? ?????? ????????????????????? ????????????????????????*/
        for (DeviceTypeAbilitysPo deviceTypeAbilitysPo : deviceTypeAbilitysPos) {
            Integer abilityId = deviceTypeAbilitysPo.getAbilityId();
            DeviceModelAbilityVo modelAbilityVo = new DeviceModelAbilityVo();

            modelAbilityVo.setAbilityId(abilityId);

            /*?????? ????????? ??? ?????????*/
            if(!deviceTypeAbilitysPo.getAbilityType().equals(DeviceConstant.ABILITY_TYPE_NUMBER)&&deviceTypeAbilitysPo.getAbilityType().equals(DeviceConstant.ABILITY_TYPE_SELECT_PARAM)){
                continue;
            }

            DeviceModelAbilityPo deviceModelAbilityPo = deviceModelAbilityPoMap.get(abilityId);
            if (deviceModelAbilityPo != null) {
                modelAbilityVo.setDefinedName(deviceModelAbilityPo.getDefinedName());
            } else {
                continue;
            }

            BeanUtils.copyProperties(deviceModelAbilityPo, modelAbilityVo);
            modelAbilityVo.setAbilityName(deviceTypeAbilitysPo.getAbilityName());

            //????????????????????????
            List<DeviceModelAbilityVo.DeviceModelAbilityOptionVo> abilityOptionList = new ArrayList();

            //???????????????????????????
            //??????
            List<DeviceAbilityOptionPo> deviceAbilityOptionPos = deviceAbilityOptionMapper.selectActiveOptionsByAbilityId(abilityId);
            List<DeviceModelAbilityOptionPo> deviceModelAbilityOptionPos = deviceModelAbilityOptionMapper.getOptionsByModelAbilityId(deviceModelAbilityPo.getId());
            Map<Integer, DeviceModelAbilityOptionPo> deviceModelAbilityOptionPoMap = deviceModelAbilityOptionPos.stream().collect(Collectors.toMap(DeviceModelAbilityOptionPo::getAbilityOptionId, a -> a, (k1, k2) -> k1));

            for (DeviceAbilityOptionPo deviceabilityOptionPo : deviceAbilityOptionPos) {
                DeviceModelAbilityOptionPo deviceModelabilityOptionPo = deviceModelAbilityOptionPoMap.get(deviceabilityOptionPo.getId());
                if (deviceModelabilityOptionPo != null) {

                    DeviceModelAbilityVo.DeviceModelAbilityOptionVo deviceModelAbilityOptionVo = new DeviceModelAbilityVo.DeviceModelAbilityOptionVo();

                    deviceModelAbilityOptionVo.setAbilityOptionId(deviceabilityOptionPo.getId());
                    deviceModelAbilityOptionVo.setOptionName(deviceabilityOptionPo.getOptionName());
                    deviceModelAbilityOptionVo.setOptionValue(deviceabilityOptionPo.getOptionValue());
                    deviceModelAbilityOptionVo.setDefinedName(deviceModelabilityOptionPo.getDefinedName());
                    deviceModelAbilityOptionVo.setDefaultVal(deviceModelabilityOptionPo.getDefaultValue());
                    deviceModelAbilityOptionVo.setMaxVal(deviceModelabilityOptionPo.getMaxVal());
                    deviceModelAbilityOptionVo.setMinVal(deviceModelabilityOptionPo.getMinVal());

                    deviceModelAbilityOptionVo.setStatus(deviceModelabilityOptionPo.getStatus());

                    abilityOptionList.add(deviceModelAbilityOptionVo);
                }
            }

            modelAbilityVo.setDeviceModelAbilityOptions(abilityOptionList);
            deviceModelAbilityVos.add(modelAbilityVo);
        }

        return deviceModelAbilityVos;

    }

    public List<DeviceSelectRsp> selectByModelId(Integer modelId) {
        Integer customerId = customerService.obtainCustomerId(false);
        return deviceMapper.selectProjectByModelId(customerId, modelId);
    }
    }