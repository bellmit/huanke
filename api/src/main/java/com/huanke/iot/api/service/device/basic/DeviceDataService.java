package com.huanke.iot.api.service.device.basic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.huanke.iot.api.constants.DeviceAbilityTypeContants;
import com.huanke.iot.api.controller.h5.req.DeviceFuncVo;
import com.huanke.iot.api.controller.h5.req.DeviceGroupFuncVo;
import com.huanke.iot.api.controller.h5.req.ShareRequest;
import com.huanke.iot.api.controller.h5.response.DeviceAbilitysVo;
import com.huanke.iot.api.controller.h5.response.DeviceDetailVo;
import com.huanke.iot.api.controller.h5.response.DeviceShareVo;
import com.huanke.iot.api.controller.h5.response.SensorDataVo;
import com.huanke.iot.api.gateway.MqttSendService;
import com.huanke.iot.api.util.FloatDataUtil;
import com.huanke.iot.api.wechat.WechartUtil;
import com.huanke.iot.base.constant.CommonConstant;
import com.huanke.iot.base.constant.DeviceConstant;
import com.huanke.iot.base.constant.DeviceTeamConstants;
import com.huanke.iot.base.dao.customer.CustomerMapper;
import com.huanke.iot.base.dao.customer.CustomerUserMapper;
import com.huanke.iot.base.dao.customer.WxBgImgMapper;
import com.huanke.iot.base.dao.customer.WxConfigMapper;
import com.huanke.iot.base.dao.device.DeviceCustomerUserRelationMapper;
import com.huanke.iot.base.dao.device.DeviceMapper;
import com.huanke.iot.base.dao.device.DeviceTeamItemMapper;
import com.huanke.iot.base.dao.device.DeviceTeamMapper;
import com.huanke.iot.base.dao.device.ability.DeviceAbilityMapper;
import com.huanke.iot.base.dao.device.ability.DeviceAbilityOptionMapper;
import com.huanke.iot.base.dao.device.data.DeviceOperLogMapper;
import com.huanke.iot.base.dao.device.stat.DeviceSensorStatMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelAbilityMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelAbilityOptionMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceTypeMapper;
import com.huanke.iot.base.enums.FuncTypeEnums;
import com.huanke.iot.base.enums.SensorTypeEnums;
import com.huanke.iot.base.exception.BusinessException;
import com.huanke.iot.base.po.customer.CustomerUserPo;
import com.huanke.iot.base.po.customer.WxConfigPo;
import com.huanke.iot.base.po.device.DeviceCustomerUserRelationPo;
import com.huanke.iot.base.po.device.DevicePo;
import com.huanke.iot.base.po.device.ability.DeviceAbilityOptionPo;
import com.huanke.iot.base.po.device.ability.DeviceAbilityPo;
import com.huanke.iot.base.po.device.data.DeviceOperLogPo;
import com.huanke.iot.base.po.device.stat.DeviceSensorStatPo;
import com.huanke.iot.base.po.device.team.DeviceTeamItemPo;
import com.huanke.iot.base.po.device.team.DeviceTeamPo;
import com.huanke.iot.base.po.device.typeModel.DeviceModelAbilityOptionPo;
import com.huanke.iot.base.po.device.typeModel.DeviceModelAbilityPo;
import com.huanke.iot.base.po.device.typeModel.DeviceTypePo;
import com.huanke.iot.base.util.LocationUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DeviceDataService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceOperLogMapper deviceOperLogMapper;

    @Autowired
    private MqttSendService mqttSendService;

    @Autowired
    private DeviceTypeMapper deviceTypeMapper;

    @Autowired
    private DeviceModelMapper deviceModelMapper;

    @Autowired
    private DeviceAbilityMapper deviceAbilityMapper;

    @Autowired
    private DeviceAbilityOptionMapper deviceAbilityOptionMapper;

    @Autowired
    private CustomerUserMapper customerUserMapper;

    @Autowired
    private DeviceCustomerUserRelationMapper deviceCustomerUserRelationMapper;

    @Autowired
    private DeviceTeamItemMapper deviceTeamItemMapper;

    @Autowired
    private DeviceTeamMapper deviceTeamMapper;

    @Autowired
    private DeviceSensorStatMapper deviceSensorStatMapper;

    @Autowired
    private WxConfigMapper wxConfigMapper;

    @Autowired
    private LocationUtils locationUtils;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private DeviceModelAbilityOptionMapper deviceModelAbilityOptionMapper;

    @Autowired
    private DeviceModelAbilityMapper deviceModelAbilityMapper;

    @Autowired
    private WxBgImgMapper wxBgImgMapper;

    @Autowired
    private WechartUtil wechartUtil;

    @Value("${unit}")
    private Integer unit;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static String[] modes = {"?????????????????????", "??????????????????", "??????????????????", "??????????????????"};

    private static final int MASTER = 1;
    private static final int SLAVE = 2;
    @Value("${speed}")
    private int speed;

    private static final String TOKEN_PREFIX = "token.";

    @Transactional
    public Object shareDevice(Integer toId, ShareRequest request) {
        Integer deviceId = request.getDeviceId();
        String master = request.getMasterOpenId();
        String token = request.getToken();
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        String deviceIdStr = devicePo.getWxDeviceId();
        if (devicePo == null) {
            log.error("??????????????????deviceId={}", deviceId);
            throw new BusinessException("???????????????");
        }
        //???????????????customerId
        Integer customerId = deviceMapper.getCustomerId(devicePo);
        CustomerUserPo customerUserPo = customerUserMapper.selectByOpenId(master);
        if (customerUserPo == null) {
            log.error("??????????????????openId={}", master);
            throw new BusinessException("???????????????");
        }
        String storeToken = stringRedisTemplate.opsForValue().get(TOKEN_PREFIX + deviceIdStr);
        if (StringUtils.isEmpty(storeToken) || !StringUtils.equals(storeToken, token)) {
            log.error("?????????Token???deviceIdStr={}", deviceIdStr);
            throw new BusinessException("??????????????????");
        }
        if (customerUserPo.getId().equals(toId)) {
            log.error("???????????????????????????");
            throw new BusinessException("???????????????????????????");
        }

        //TODO??????deviceId????????????????????????????????????
        DeviceTeamItemPo queryTeamItemPo = new DeviceTeamItemPo();
        queryTeamItemPo.setDeviceId(deviceId);
        queryTeamItemPo.setUserId(customerUserPo.getId());
        queryTeamItemPo.setStatus(1);
        Integer itemCount = deviceTeamMapper.queryItemCount(queryTeamItemPo);
        if (itemCount == 0) {
            log.error("?????????????????????");
            throw new BusinessException("?????????????????????");
        }
        //TODO??????deviceId????????????????????????????????????
        DeviceTeamItemPo toQueryTeamItemPo = new DeviceTeamItemPo();
        toQueryTeamItemPo.setDeviceId(deviceId);
        toQueryTeamItemPo.setUserId(toId);
        toQueryTeamItemPo.setStatus(null);
        List<DeviceTeamItemPo> deviceTeamItemPoList = deviceTeamMapper.queryTeamItems(toQueryTeamItemPo);
        if (deviceTeamItemPoList.size() > 0 && deviceTeamItemPoList.get(0).getStatus().equals(CommonConstant.STATUS_YES)) {
            log.error("???????????????????????????");
            throw new BusinessException("???????????????????????????");
        }

        DeviceTeamPo deviceTeamPo = new DeviceTeamPo();
        String defaultTeamName = wxConfigMapper.selectConfigByCustomerId(customerId).getDefaultTeamName();
        deviceTeamPo.setName(defaultTeamName);
        deviceTeamPo.setMasterUserId(toId);
        deviceTeamPo.setCreateUserId(toId);
        deviceTeamPo.setCustomerId(customerUserPo.getCustomerId());
        deviceTeamPo.setStatus(1);
        //?????????????????????????????????
        DeviceTeamPo toDeviceTeamPo = deviceTeamMapper.queryByName(deviceTeamPo);
        Integer teamId;
        if (toDeviceTeamPo != null && toDeviceTeamPo.getStatus().equals(CommonConstant.STATUS_YES)) {
            teamId = toDeviceTeamPo.getId();
        } else {
            deviceTeamPo.setCreateTime(System.currentTimeMillis());
            deviceTeamPo.setTeamStatus(1);
            deviceTeamPo.setTeamType(DeviceTeamConstants.DEVICE_TEAM_TYPE_USER);
            deviceTeamPo.setCreateUserId(toId);
            deviceTeamPo.setCustomerId(customerId);
            deviceTeamMapper.insert(deviceTeamPo);
            teamId = deviceTeamPo.getId();
        }
        if (deviceTeamItemPoList.size() > 0 && deviceTeamItemPoList.get(0).getStatus().equals(CommonConstant.STATUS_DEL)) {
            DeviceTeamItemPo deviceTeamItemPo = deviceTeamItemPoList.get(0);
            deviceTeamItemPo.setTeamId(teamId);
            deviceTeamItemPo.setLastUpdateTime(System.currentTimeMillis());
            deviceTeamItemPo.setStatus(CommonConstant.STATUS_YES);
            deviceTeamItemMapper.updateById(deviceTeamItemPo);
            return true;
        }
        DeviceTeamItemPo queryItemPo = new DeviceTeamItemPo();
        queryItemPo.setDeviceId(deviceId);
        queryItemPo.setUserId(toId);
        queryItemPo.setTeamId(teamId);
        queryItemPo.setStatus(1);
        queryItemPo.setCreateTime(System.currentTimeMillis());
        deviceTeamItemMapper.insert(queryItemPo);
        return true;
    }

    public Boolean updateRelation(String joinOpenId, Integer userId, Integer deviceId, Integer status) {
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        if (devicePo == null) {
            log.error("??????????????????deviceId={}", deviceId);
            return false;
        }
        CustomerUserPo customerUserPo = customerUserMapper.selectById(userId);
        DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = new DeviceCustomerUserRelationPo();
        deviceCustomerUserRelationPo.setOpenId(customerUserPo.getOpenId());
        deviceCustomerUserRelationPo.setDeviceId(deviceId);
        DeviceCustomerUserRelationPo byDeviceCustomerUserRelationPo = deviceCustomerUserRelationMapper.findAllByDeviceCustomerUserRelationPo(deviceCustomerUserRelationPo);
        if (byDeviceCustomerUserRelationPo == null) {
            log.error("?????????????????????deviceId={}", deviceId);
            return false;
        }

        CustomerUserPo beClearCustomerUserPo = customerUserMapper.selectByOpenId(joinOpenId);
        if (beClearCustomerUserPo == null) {
            log.error("???????????????????????????deviceId={}", deviceId);
            return false;
        }
        if (beClearCustomerUserPo.getId().equals(userId)) {
            log.error("?????????????????????????????????deviceId={}", deviceId);
            return false;
        }
        DeviceTeamItemPo queryItemPo = new DeviceTeamItemPo();
        queryItemPo.setUserId(beClearCustomerUserPo.getId());
        queryItemPo.setDeviceId(deviceId);
        queryItemPo.setStatus(null);
        List<DeviceTeamItemPo> deviceTeamItemPos = deviceTeamMapper.queryTeamItems(queryItemPo);
        if (deviceTeamItemPos.size() == 0) {
            log.error("??????????????????????????????deviceId={}", deviceId);
            return false;
        }
        DeviceTeamItemPo deviceTeamItemPo = deviceTeamItemPos.get(0);
        return deviceTeamItemMapper.updateStatus(deviceTeamItemPo.getDeviceId(), deviceTeamItemPo.getUserId(), status) > 0;
    }


    public List<SensorDataVo> getHistoryData(Integer deviceId, Integer type) {

        Long startTimestamp = new DateTime().plusDays(-1).getMillis();
        Long endTimeStamp = System.currentTimeMillis();

        List<SensorDataVo> sensorDataVos = Lists.newArrayList();
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        if (devicePo == null) {
            return null;
        }
        Integer modelId = devicePo.getModelId();
        List<DeviceAbilityPo> deviceAbilityPos = deviceModelAbilityMapper.selectActiveByModelId(modelId);
        List<String> dirValues = deviceAbilityPos.stream().map(deviceAbilityPo -> deviceAbilityPo.getDirValue()).collect(Collectors.toList());

        List<DeviceSensorStatPo> deviceSensorPos = deviceSensorStatMapper.selectData(devicePo.getId(), startTimestamp, endTimeStamp);
        for (String sensorType : dirValues) {
            SensorDataVo sensorDataVo = new SensorDataVo();
            SensorTypeEnums sensorTypeEnums = SensorTypeEnums.getByCode(sensorType);
            if (sensorTypeEnums == null) {
                continue;
            }
            sensorDataVo.setName(sensorTypeEnums.getMark());
            sensorDataVo.setUnit(sensorTypeEnums.getUnit());
            sensorDataVo.setType(sensorType);
            List<String> xdata = Lists.newArrayList();
            List<String> ydata = Lists.newArrayList();
            for (DeviceSensorStatPo deviceSensorPo : deviceSensorPos) {
                if (deviceSensorPo.getPm() == null && devicePo.getHostDeviceId() == null) {
                    continue;
                }
                xdata.add(new DateTime(deviceSensorPo.getStartTime()).toString("yyyy-MM-dd HH:mm:ss"));
                switch (sensorTypeEnums){
                    case CO2_IN:
                        ydata.add(deviceSensorPo.getCo2()!=null? deviceSensorPo.getCo2().toString(): "0");
                        break;
                    case HUMIDITY_IN:
                        ydata.add(deviceSensorPo.getHum()!=null? deviceSensorPo.getHum().toString(): "0");
                        break;
                    case TEMPERATURE_IN:
                        ydata.add(deviceSensorPo.getTem()!=null? deviceSensorPo.getTem().toString(): "0");
                        break;
                    case HCHO_IN:
                        ydata.add(FloatDataUtil.getFloat(deviceSensorPo.getHcho()));
                        break;
                    case PM25_IN:
                        ydata.add(deviceSensorPo.getPm()!=null? deviceSensorPo.getPm().toString(): "0");
                        break;
                    case TVOC_IN:
                        ydata.add(FloatDataUtil.getFloat(deviceSensorPo.getTvoc()));
                        break;
                    case NH3_IN:
                        ydata.add(deviceSensorPo.getNh3()!=null? deviceSensorPo.getNh3().toString(): "0");
                        break;
                    case ANION_IN:
                        ydata.add(deviceSensorPo.getAnion()!=null? deviceSensorPo.getAnion().toString(): "0");
                        break;
                    case OUT_WATER_TEM:
                        ydata.add(deviceSensorPo.getOutWaterTem()!=null? deviceSensorPo.getOutWaterTem().toString(): "0");
                        break;
                    case IN_WATER_TEM:
                        ydata.add(deviceSensorPo.getInWaterTem()!=null? deviceSensorPo.getInWaterTem().toString(): "0");
                        break;
                    default:
                        break;
                }
                sensorDataVo.setXdata(xdata);
                sensorDataVo.setYdata(ydata);
            }
            if (!ydata.isEmpty()) {
                sensorDataVos.add(sensorDataVo);
            }
            sensorDataVo.setXdata(xdata);
            sensorDataVo.setYdata(ydata);
        }

        return sensorDataVos;
    }

    public List<DeviceShareVo> shareList(Integer userId, Integer deviceId) {
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        if (devicePo != null) {
            CustomerUserPo customerUserPo = customerUserMapper.selectById(userId);
            DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = new DeviceCustomerUserRelationPo();
            deviceCustomerUserRelationPo.setOpenId(customerUserPo.getOpenId());
            deviceCustomerUserRelationPo.setDeviceId(deviceId);
            DeviceCustomerUserRelationPo byDeviceCustomerUserRelationPo = deviceCustomerUserRelationMapper.findAllByDeviceCustomerUserRelationPo(deviceCustomerUserRelationPo);

            if (byDeviceCustomerUserRelationPo != null) {
                DeviceTeamItemPo deviceTeamItemPo = new DeviceTeamItemPo();
                deviceTeamItemPo.setDeviceId(deviceId);
                List<DeviceTeamItemPo> deviceTeamItemPos = deviceTeamItemMapper.selectItemsByDeviceId(deviceId);
                List<DeviceCustomerUserRelationPo> masterDeviceCustomerUserRelationPos = deviceCustomerUserRelationMapper.queryByDeviceId(deviceId);
                List<DeviceTeamItemPo> finalDeviceTeamItemPos = deviceTeamItemPos.stream().filter(deviceTeamItemPo1 -> {
                    List<String> openIdsList = masterDeviceCustomerUserRelationPos.stream().map(deviceCustomerUserRelationPo1 -> deviceCustomerUserRelationPo1.getOpenId()).collect(Collectors.toList());
                    Integer userId1 = deviceTeamItemPo1.getUserId();
                    CustomerUserPo customerUserPo1 = customerUserMapper.selectById(userId1);
                    if (!openIdsList.contains(customerUserPo1.getOpenId())) {
                        return true;
                    }
                    return false;
                }).sorted(Comparator.comparing(DeviceTeamItemPo::getCreateTime)).collect(Collectors.toList());
                List<DeviceShareVo> shareVos = finalDeviceTeamItemPos.stream()
                        .map(finalDeviceTeamItemPo -> {
                            Integer deviceUserId = finalDeviceTeamItemPo.getUserId();
                            CustomerUserPo deviceCustomerUserPo = customerUserMapper.selectById(deviceUserId);
                            DeviceShareVo deviceShareVo = new DeviceShareVo();
                            deviceShareVo.setUserId(deviceCustomerUserPo.getId());
                            deviceShareVo.setNickname(deviceCustomerUserPo.getNickname());
                            deviceShareVo.setJoinTime(finalDeviceTeamItemPo.getCreateTime());
                            deviceShareVo.setOpenId(deviceCustomerUserPo.getOpenId());
                            deviceShareVo.setHeadImg(deviceCustomerUserPo.getHeadimgurl());
                            deviceShareVo.setStatus(finalDeviceTeamItemPo.getStatus() == 1 ? true : false);
                            return deviceShareVo;
                        }).collect(Collectors.toList());

                return shareVos;
            }
        }
        return Lists.newArrayList();
    }

    @Transactional
    public Boolean deleteDevice(Integer userId, Integer deviceId) {
        if (deviceId == null) {
            log.error("deviceId????????????");
            return false;
        }
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        if (devicePo == null) {
            log.error("??????????????????");
            return false;
        }
        Boolean ret = false;

        Integer iDeviceId = devicePo.getId();

        CustomerUserPo customerUserPo = customerUserMapper.selectById(userId);
        DeviceCustomerUserRelationPo querydeviceCustomerUserRelationPo = new DeviceCustomerUserRelationPo();
        String openId = customerUserPo.getOpenId();
        querydeviceCustomerUserRelationPo.setOpenId(openId);
        querydeviceCustomerUserRelationPo.setDeviceId(iDeviceId);
        DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = deviceCustomerUserRelationMapper.findAllByDeviceCustomerUserRelationPo(querydeviceCustomerUserRelationPo);
        if (deviceCustomerUserRelationPo == null) {
            log.error("?????????????????????, userId={}, deviceId={}, openId={}", userId, deviceId, openId);
            return false;
        }

        if (deviceCustomerUserRelationPo.getParentOpenId() == null) {
            //????????????
            log.info("???????????????????????????userId={}, deviceId={}", userId, deviceId);
            ret = deviceCustomerUserRelationMapper.deleteRelationByJoinId(openId, iDeviceId) > 0;
            ret = ret && deviceTeamItemMapper.deleteByJoinId(iDeviceId, userId) > 0;
            //deviceGroupItemMapper.deleteByJoinId(iDeviceId, userId);
        } else {
            ret = deviceCustomerUserRelationMapper.deleteRelationByJoinId(customerUserPo.getOpenId(), iDeviceId) > 0;
            ret = ret && deviceTeamItemMapper.deleteByJoinId(iDeviceId, userId) > 0;
            //deviceGroupItemMapper.deleteByJoinId(iDeviceId, userId);
        }
        return ret;
    }

    /**
     * ????????????????????????
     *
     * @param abilityIds
     * @return
     */
    public List<DeviceAbilitysVo> queryDetailAbilitysValue(Integer deviceId, List<Integer> abilityIds) {
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        Integer modelId = devicePo.getModelId();
        boolean isOld = devicePo.getOld() == 1;
        List<DeviceAbilitysVo> deviceAbilitysVoList = new ArrayList<>();
        Map<Object, Object> datas = stringRedisTemplate.opsForHash().entries("sensor2." + deviceId);
        Map<Object, Object> controlDatas = stringRedisTemplate.opsForHash().entries("control2." + deviceId);
        //??????????????????
        List<DeviceAbilityPo> deviceAbilityPoCaches = deviceAbilityMapper.selectList(new DeviceAbilityPo(), 10000, 0);
        Map<Integer,DeviceAbilityPo> deviceAbilityPoMap = deviceAbilityPoCaches.stream().collect(Collectors.toMap(DeviceAbilityPo::getId, a -> a,(k1, k2)->k1));
        for (Integer abilityId : abilityIds) {
            List<DeviceModelAbilityOptionPo> deviceModelAbilityOptionPoCaches = new ArrayList<>();
            Map<Integer, DeviceModelAbilityOptionPo> deviceModelAbilityOptionPoMap = new HashMap<>();
            DeviceAbilityPo deviceabilityPo = deviceAbilityPoMap.get(abilityId);
            String dirValue = deviceabilityPo.getDirValue();
            Integer abilityType = deviceabilityPo.getAbilityType();
            DeviceAbilitysVo deviceAbilitysVo = new DeviceAbilitysVo();
            deviceAbilitysVo.setAbilityName(deviceabilityPo.getAbilityName());
            deviceAbilitysVo.setId(abilityId);
            deviceAbilitysVo.setAbilityType(abilityType);
            deviceAbilitysVo.setDirValue(dirValue);
            switch (abilityType) {
                case DeviceAbilityTypeContants.ability_type_text:
                    if(!isOld){
                        String data = getData(controlDatas, dirValue);
                        deviceAbilitysVo.setCurrValue(data);
                        deviceAbilitysVo.setUnit(deviceabilityPo.getRemark());
                    }else {
                        if (deviceabilityPo.getWriteStatus() == 1) {
                            String data = getData(controlDatas, dirValue);
                            if (data.equals("0")) {
                                data = getData(datas, dirValue);
                            }
                            deviceAbilitysVo.setCurrValue(data);
                            deviceAbilitysVo.setUnit(deviceabilityPo.getRemark());
                        } else {
                            deviceAbilitysVo.setCurrValue(getData(datas, dirValue));
                            deviceAbilitysVo.setUnit(deviceabilityPo.getRemark());
                        }
                    }
                    break;
                case DeviceAbilityTypeContants.ability_type_single:
                    List<DeviceAbilityOptionPo> deviceabilityOptionPos = deviceAbilityOptionMapper.selectOptionsByAbilityId(abilityId);
                    //??????
                    deviceModelAbilityOptionPoCaches = deviceModelAbilityOptionMapper.queryByModelIdAbilityId(modelId ,abilityId );
                    deviceModelAbilityOptionPoMap = deviceModelAbilityOptionPoCaches.stream().collect(Collectors.toMap(DeviceModelAbilityOptionPo::getAbilityOptionId, a -> a, (k1, k2) -> k1));
                    String optionValue = getData(controlDatas, dirValue);
                    List<DeviceAbilitysVo.abilityOption> abilityOptionList = new ArrayList<>();
                    for (DeviceAbilityOptionPo deviceabilityOptionPo : deviceabilityOptionPos) {
                        DeviceModelAbilityOptionPo deviceModelAbilityOptionPo = deviceModelAbilityOptionPoMap.get(deviceabilityOptionPo.getId());
                        if(deviceModelAbilityOptionPo == null){
                            continue;
                        }
                        DeviceAbilitysVo.abilityOption abilityOption = new DeviceAbilitysVo.abilityOption();
                        abilityOption.setDirValue(deviceabilityOptionPo.getOptionValue());
                        if ((StringUtils.isNotEmpty(deviceModelAbilityOptionPo.getActualOptionValue())&&optionValue.equals(deviceModelAbilityOptionPo.getActualOptionValue()))
                                ||(StringUtils.isEmpty(deviceModelAbilityOptionPo.getActualOptionValue())&&optionValue.equals(deviceabilityOptionPo.getOptionValue()))) {
                            deviceAbilitysVo.setCurrValue(deviceabilityOptionPo.getOptionValue());
                            abilityOption.setIsSelect(1);
                        } else {
                            abilityOption.setIsSelect(0);
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
                    List<DeviceAbilitysVo.abilityOption> abilityOptionList1 = new ArrayList<>();
                    for (DeviceAbilityOptionPo deviceabilityOptionPo : deviceabilityOptionPos1) {
                        DeviceModelAbilityOptionPo deviceModelAbilityOptionPo = deviceModelAbilityOptionPoMap.get(deviceabilityOptionPo.getId());
                        if(deviceModelAbilityOptionPo == null){
                            continue;
                        }
                        String targetOptionValue = deviceabilityOptionPo.getOptionValue();
                        String finalOptionValue = getData(controlDatas, targetOptionValue);
                        DeviceAbilitysVo.abilityOption abilityOption = new DeviceAbilitysVo.abilityOption();
                        abilityOption.setDirValue(deviceabilityOptionPo.getOptionValue());
                        if (Integer.valueOf(finalOptionValue) == 1) {
                            abilityOption.setIsSelect(1);
                        } else {
                            abilityOption.setIsSelect(0);
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
                        List<DeviceAbilitysVo.abilityOption> abilityOptionList5 = new ArrayList<>();
                        for (DeviceAbilityOptionPo deviceabilityOptionPo : deviceabilityOptionPos5) {
                            String optionValue5 = getData(controlDatas, deviceabilityOptionPo.getOptionValue());
                            DeviceAbilitysVo.abilityOption abilityOption = new DeviceAbilitysVo.abilityOption();
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
                        List<DeviceAbilitysVo.abilityOption> abilityOptionList5 = new ArrayList<>();
                        for (DeviceAbilityOptionPo deviceabilityOptionPo : deviceabilityOptionPos5) {
                            DeviceModelAbilityOptionPo deviceModelAbilityOptionPo = deviceModelAbilityOptionPoMap.get(deviceabilityOptionPo.getId());
                            if(deviceModelAbilityOptionPo == null){
                                continue;
                            }
                            DeviceAbilitysVo.abilityOption abilityOption = new DeviceAbilitysVo.abilityOption();
                            abilityOption.setDirValue(deviceabilityOptionPo.getOptionValue());
                            if (optionValue5.equals(deviceabilityOptionPo.getOptionValue())) {
                                abilityOption.setIsSelect(1);
                            } else {
                                abilityOption.setIsSelect(0);
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
//            DeviceAbilitysVo deviceAbilitysVo = new DeviceAbilitysVo();
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

    public Boolean updateAllRelation(Integer deviceId, Integer status) {
        List<DeviceTeamItemPo> deviceTeamItemPos = deviceTeamItemMapper.selectItemsByDeviceId(deviceId);
        List<DeviceCustomerUserRelationPo> deviceCustomerUserRelationPos = deviceCustomerUserRelationMapper.queryByDeviceId(deviceId);
        List<DeviceTeamItemPo> finalDeviceTeamItemPos = deviceTeamItemPos.stream().filter(deviceTeamItemPo -> {
            List<String> openIdsList = deviceCustomerUserRelationPos.stream().map(deviceCustomerUserRelationPo -> deviceCustomerUserRelationPo.getOpenId()).collect(Collectors.toList());
            Integer userId = deviceTeamItemPo.getUserId();
            CustomerUserPo customerUserPo = customerUserMapper.selectById(userId);
            if (!openIdsList.contains(customerUserPo.getOpenId())) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        for (DeviceTeamItemPo deviceTeamItemPo : finalDeviceTeamItemPos) {
            deviceTeamItemPo.setStatus(status);
            deviceTeamItemMapper.updateById(deviceTeamItemPo);
        }
        return true;
    }

    public Boolean clearRelation(String openId, Integer userId, Integer deviceId) {
        DeviceTeamItemPo queryItemPo = new DeviceTeamItemPo();
        CustomerUserPo beClearCustomerUserPo = customerUserMapper.selectByOpenId(openId);
        queryItemPo.setUserId(beClearCustomerUserPo.getId());
        queryItemPo.setDeviceId(deviceId);
        queryItemPo.setStatus(null);
        List<DeviceTeamItemPo> deviceTeamItemPos = deviceTeamMapper.queryTeamItems(queryItemPo);
        if (deviceTeamItemPos.size() == 0) {
            log.error("??????????????????????????????deviceId={}", deviceId);
            return false;
        }
        DeviceTeamItemPo deviceTeamItemPo = deviceTeamItemPos.get(0);
        return deviceTeamItemMapper.deleteByJoinId(deviceTeamItemPo.getDeviceId(), deviceTeamItemPo.getUserId()) > 0;
    }

    public Map<String, String> queryStrainerData(Integer deviceId, List<String> dirValueList) {
        Map<String, String> strainerMap = new HashMap<>();
        Map<Object, Object> controlDatas = stringRedisTemplate.opsForHash().entries("control2." + deviceId);
        for (String dirValue : dirValueList) {
            String optionValue = getData(controlDatas, dirValue);
            strainerMap.put(dirValue, optionValue);
        }
        return strainerMap;
    }

    public String queryServerUser(Integer userId) {
        CustomerUserPo customerUserPo = customerUserMapper.selectById(userId);
        WxConfigPo wxConfigPo = wxConfigMapper.selectConfigByCustomerId(customerUserPo.getCustomerId());
        return wxConfigPo.getServiceUser();
    }

    @Transactional
    public Boolean deleteDeviceItem(Integer userId, String openId, Integer deviceId) throws Exception {
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = new DeviceCustomerUserRelationPo();
        deviceCustomerUserRelationPo.setDeviceId(deviceId);
        deviceCustomerUserRelationPo.setOpenId(openId);
        DeviceCustomerUserRelationPo byDeviceCustomerUserRelationPo = deviceCustomerUserRelationMapper.findAllByDeviceCustomerUserRelationPo(deviceCustomerUserRelationPo);
        if(byDeviceCustomerUserRelationPo != null){
            //?????????????????????????????????
            Boolean aBoolean = deleteDevice(userId, deviceId);
            Map<String, String> requestMap = new HashMap<>();
            requestMap.put("OpenID", openId);
            requestMap.put("DeviceID", devicePo.getWxDeviceId());
            requestMap.put("status", "huanke");
            //????????????????????????
            devicePo.setBindStatus(DeviceConstant.BIND_STATUS_NO);
            devicePo.setBindTime(null);
            deviceMapper.updateById(devicePo);
            aBoolean = aBoolean && wechartUtil.unbindDevice(openId, devicePo.getWxDeviceId());
            return aBoolean;
        }else{
            //??????????????????????????????????????????
            return deviceTeamItemMapper.deleteByJoinId(deviceId, userId) > 0;
        }
    }

    public List<String> queryBgImgs(Integer customerId) {
        List<String> bgImgs = wxBgImgMapper.queryBgImgs(customerId);
        return bgImgs;
    }


    @Data
    public static class FuncItemMessage {
        private String type;
        private String value;
        private String childid;

    }

    @Data
    public static class FuncListMessage {
        private String msg_id;
        private String msg_type;
        private List<DeviceDataService.FuncItemMessage> datas;
    }

    public DeviceDetailVo queryDetailByDeviceId(String deviceId) {
        DeviceDetailVo deviceDetailVo = new DeviceDetailVo();
        DevicePo devicePo = deviceMapper.selectByWxDeviceId(deviceId);

        if (devicePo != null) {
            deviceDetailVo.setDeviceName(devicePo.getName());
            deviceDetailVo.setDeviceId(deviceId);
            DeviceTypePo deviceTypePo = deviceTypeMapper.selectById(devicePo.getTypeId());
            if (deviceTypePo != null) {
                deviceDetailVo.setDeviceTypeName(deviceTypePo.getName());
            }

            deviceDetailVo.setIp(devicePo.getIp());
            deviceDetailVo.setMac(devicePo.getMac());
            deviceDetailVo.setDate(new DateTime().toString("yyyy???MM???dd???"));
            getIndexData(deviceDetailVo, devicePo.getId(), devicePo.getTypeId());
            if (deviceDetailVo.getPm() == null || StringUtils.isEmpty(deviceDetailVo.getPm().getData()) || StringUtils.equals("0", deviceDetailVo.getPm().getData())) {
                deviceDetailVo.setAqi("0");
            } else {
                Integer pm = Integer.valueOf(deviceDetailVo.getPm().getData());
                deviceDetailVo.setAqi(String.valueOf(getAqi(pm)));
            }
            fillDeviceInfo(deviceDetailVo, devicePo);
        }

        JSONObject weatherJson = locationUtils.getWeather(devicePo.getIp(), false);
        if (weatherJson != null) {
            if (weatherJson.containsKey("result")) {
                JSONObject result = weatherJson.getJSONObject("result");
                if (result != null) {
                    deviceDetailVo.setOuterHum(result.getString("humidity"));
                    deviceDetailVo.setOuterPm(result.getString("aqi"));
                    deviceDetailVo.setOuterTem(result.getString("temperature_curr"));
                    deviceDetailVo.setWeather(result.getString("weather_curr"));
                }
            }
        }
        if (StringUtils.isEmpty(devicePo.getLocation())) {
            JSONObject locationJson = locationUtils.getLocation(devicePo.getIp(), false);
            if (locationJson != null) {
                if (locationJson.containsKey("content")) {
                    JSONObject content = locationJson.getJSONObject("content");
                    if (content != null) {
                        if (content.containsKey("address_detail")) {
                            JSONObject addressDetail = content.getJSONObject("address_detail");
                            if (addressDetail != null) {
                                deviceDetailVo.setProvince(addressDetail.getString("province"));
                                deviceDetailVo.setCity(addressDetail.getString("city"));
                                deviceDetailVo.setArea(deviceDetailVo.getCity());
                                deviceDetailVo.setLocation(deviceDetailVo.getProvince() + "," + deviceDetailVo.getCity());
                            }
                        }
                    }
                }
            }
        } else {
            String[] locationArray = devicePo.getLocation().split(",");
            deviceDetailVo.setArea(Joiner.on(" ").join(locationArray));
            deviceDetailVo.setLocation(devicePo.getLocation());
        }
        return deviceDetailVo;
    }

    private void fillDeviceInfo(DeviceDetailVo deviceDetailVo, DevicePo devicePo) {
        DeviceDetailVo.DeviceInfoItem info = new DeviceDetailVo.DeviceInfoItem();
        info.setDeviceSupport("????????????");
        info.setSoftSupport("????????????");
        info.setMac(devicePo.getMac());
        info.setId(devicePo.getId());
        String version = devicePo.getVersion();
        JSONObject jsonObject = JSON.parseObject(version);
        if (jsonObject != null) {
            info.setHardVersion(jsonObject.getString("hardware"));
            info.setSoftVersion(jsonObject.getString("software"));
        }
        deviceDetailVo.setDeviceInfoItem(info);
    }

    public void sendGroupFunc(DeviceGroupFuncVo deviceGroupFuncVo, Integer userId, int operType) {
        List<Integer> deviceIdList = deviceGroupFuncVo.getDeviceIdList();
        String funcId = deviceGroupFuncVo.getFuncId();
        String value = deviceGroupFuncVo.getValue();
        for (Integer deviceId : deviceIdList) {
            DeviceFuncVo deviceFuncVo = new DeviceFuncVo();
            deviceFuncVo.setDeviceId(deviceId);
            deviceFuncVo.setFuncId(funcId);
            deviceFuncVo.setValue(value);
            String requestId = sendFunc(deviceFuncVo, userId, operType);
        }
    }

    public Boolean sendFuncs(DeviceFuncVo deviceFuncVo, Integer userId, Integer operType){
        //???????????????????????????
        DevicePo devicePo = deviceMapper.selectById(deviceFuncVo.getDeviceId());
        if(null != devicePo){
            //?????????????????????????????????
            if(devicePo.getEnableStatus() == DeviceConstant.ENABLE_STATUS_NO){
                throw new BusinessException("?????????????????????");
            }
            //???????????????????????????
            sendFunc(deviceFuncVo,userId,operType);
            //???????????????????????????????????????
            DeviceTeamItemPo deviceTeamItemPo = this.deviceTeamItemMapper.selectByDeviceId(devicePo.getId());
            if(null != deviceTeamItemPo && deviceTeamItemPo.getLinkAgeStatus().equals(1)){
                //?????????????????????????????????
                List<DeviceTeamItemPo> deviceTeamItemPoList = this.deviceTeamItemMapper.selectLinkDevice(deviceTeamItemPo);
                for (DeviceTeamItemPo eachPo : deviceTeamItemPoList) {
                    DeviceFuncVo linkDeviceFuncVo = new DeviceFuncVo();
                    linkDeviceFuncVo.setDeviceId(eachPo.getDeviceId());
                    linkDeviceFuncVo.setFuncId(deviceFuncVo.getFuncId());
                    linkDeviceFuncVo.setValue(deviceFuncVo.getValue());
                    String requestId = sendFunc(linkDeviceFuncVo,userId, operType);
                }
            }
            return true;
        }
        return false;
    }

    public String sendFunc(DeviceFuncVo deviceFuncVo, Integer userId, Integer operType) {
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
            DeviceOperLogPo deviceOperLogPo = new DeviceOperLogPo();
            deviceOperLogPo.setFuncId(deviceFuncVo.getFuncId());
            deviceOperLogPo.setDeviceId(devicePo.getId());
            deviceOperLogPo.setOperType(operType);
            deviceOperLogPo.setOperUserId(userId);
            deviceOperLogPo.setFuncValue(deviceFuncVo.getValue());
            deviceOperLogPo.setRequestId(requestId);
            deviceOperLogPo.setCreateTime(System.currentTimeMillis());
            deviceOperLogMapper.insert(deviceOperLogPo);
            FuncListMessage funcListMessage = new FuncListMessage();
            funcListMessage.setMsg_type("control");
            funcListMessage.setMsg_id(requestId);
            FuncItemMessage funcItemMessage = new FuncItemMessage();
            funcItemMessage.setType(deviceFuncVo.getFuncId());
            funcItemMessage.setValue(actualValue);
            funcItemMessage.setChildid(devicePo.getChildId());
            funcListMessage.setDatas(Lists.newArrayList(funcItemMessage));
            mqttSendService.sendMessage(topic, JSON.toJSONString(funcListMessage),devicePo.isOldDevice());
            stringRedisTemplate.opsForHash().put("control2." + devicePo.getId(), funcItemMessage.getType(), String.valueOf(funcItemMessage.getValue()));
            return requestId;
        }
        return "";
    }

    public void flushCache() {
        customerMapper.flushCache();
        //customerUserMapper.flushCache();//????????????api??????????????????????????????????????????????????????????????????????????????
        deviceAbilityMapper.flushCache();
        deviceAbilityOptionMapper.flushCache();
        deviceModelMapper.flushCache();
        deviceModelAbilityMapper.flushCache();
        deviceModelAbilityOptionMapper.flushCache();
    }
    private void getIndexData(DeviceDetailVo deviceDetailVo, Integer deviceId, Integer deviceTypeId) {

        Map<Object, Object> datas = stringRedisTemplate.opsForHash().entries("sensor2." + deviceId);
        Map<Object, Object> controlDatas = stringRedisTemplate.opsForHash().entries("control2." + deviceId);

        DeviceDetailVo.PmDataItem pm = new DeviceDetailVo.PmDataItem();
        pm.setData(getData(datas, SensorTypeEnums.PM25_IN.getCode()));
        pm.setUnit(SensorTypeEnums.PM25_IN.getUnit());
        String data = pm.getData();
        if (StringUtils.isNotEmpty(data)) {
            Integer diData = Integer.valueOf(data);
            if (diData >= 0 && diData <= 35) {
                pm.setMass("???");
            } else if (diData > 35 && diData <= 75) {
                pm.setMass("???");
            } else if (diData > 75 && diData <= 150) {
                pm.setMass("???");
            } else {
                pm.setMass("???");
            }
        } else {
            pm.setMass("");
        }
        deviceDetailVo.setPm(pm);

        DeviceDetailVo.SysDataItem co2 = new DeviceDetailVo.SysDataItem();
        co2.setData(getData(datas, SensorTypeEnums.CO2_IN.getCode()));
        co2.setUnit(SensorTypeEnums.CO2_IN.getUnit());
        deviceDetailVo.setCo2(co2);


        DeviceDetailVo.SysDataItem tvoc = new DeviceDetailVo.SysDataItem();
        tvoc.setUnit(SensorTypeEnums.TVOC_IN.getUnit());
        String tvocData = getData(datas, SensorTypeEnums.TVOC_IN.getCode());
        if (StringUtils.isNotEmpty(tvocData)) {
            Integer digData = Integer.valueOf(tvocData);
            tvoc.setData(FloatDataUtil.getFloat(digData));
        } else {
            tvoc.setData("0");
        }
        deviceDetailVo.setTvoc(tvoc);

        DeviceDetailVo.SysDataItem hcho = new DeviceDetailVo.SysDataItem();
        hcho.setUnit(SensorTypeEnums.HCHO_IN.getUnit());
        String hchoData = getData(datas, SensorTypeEnums.HCHO_IN.getCode());
        if (StringUtils.isNotEmpty(hchoData)) {
            Integer digData = Integer.valueOf(hchoData);
            hcho.setData(FloatDataUtil.getFloat(digData));
        } else {
            hcho.setData("0");
        }
        deviceDetailVo.setHcho(hcho);

        DeviceDetailVo.SysDataItem tem = new DeviceDetailVo.SysDataItem();
        tem.setData(getData(datas, SensorTypeEnums.TEMPERATURE_IN.getCode()));
        tem.setUnit(SensorTypeEnums.TEMPERATURE_IN.getUnit());
        deviceDetailVo.setTem(tem);


        DeviceDetailVo.SysDataItem hum = new DeviceDetailVo.SysDataItem();
        hum.setData(getData(datas, SensorTypeEnums.HUMIDITY_IN.getCode()));
        hum.setUnit(SensorTypeEnums.HUMIDITY_IN.getUnit());
        deviceDetailVo.setHum(hum);

        DeviceDetailVo.SysDataItem remain = new DeviceDetailVo.SysDataItem();
        remain.setData(getData(controlDatas, FuncTypeEnums.TIMER_REMAIN.getCode()));
        remain.setUnit("???");
        deviceDetailVo.setRemain(remain);

        DeviceDetailVo.SysDataItem screen = new DeviceDetailVo.SysDataItem();
        String time = getData(controlDatas, FuncTypeEnums.TIMER_SCREEN.getCode());
        if (StringUtils.isNotEmpty(time)) {
            screen.setData(String.valueOf(unit * Integer.valueOf(time)));
        } else {
            screen.setData("0");
        }
        screen.setUnit("???");
        deviceDetailVo.setScreen(screen);


        DeviceDetailVo.DataItem modeItem = new DeviceDetailVo.DataItem();
        modeItem.setChoice(FuncTypeEnums.MODE.getRange());
        modeItem.setType(FuncTypeEnums.MODE.getCode());
        String modeValue = getData(controlDatas, FuncTypeEnums.MODE.getCode());
        modeItem.setValue(modeValue);

        deviceDetailVo.setModeItem(modeItem);

        DeviceTypePo deviceTypePo = deviceTypeMapper.selectById(deviceTypeId);
        if (deviceTypePo != null) {
            //typeId ???abilityId
            List<DeviceAbilityPo> deviceabilityPos = deviceAbilityMapper.selectDeviceAbilitysByTypeId(deviceTypeId);
            List<String> winds = getType(FuncTypeEnums.WIND1.getCode().substring(0, 2), deviceabilityPos);
            List<DeviceDetailVo.OtherItem> dataItems = winds.stream().map(wind -> {
                DeviceDetailVo.OtherItem dataItem = new DeviceDetailVo.OtherItem();
                dataItem.setType(wind);
                StringBuilder choiceSb = new StringBuilder();
                String[] dataArray = {"???", "???", "???", "???", "???", "???", "???"};
                for (int i = 0; i < speed; i++) {
                    choiceSb.append((i + 1)).append(":").append(dataArray[i]).append("?????????");
                    if (i != (speed - 1)) {
                        choiceSb.append(",");
                    }
                }
                dataItem.setChoice(choiceSb.toString());
                dataItem.setValue(getData(controlDatas, wind));
                if (winds.size() == 1) {
                    dataItem.setName("??????");
                } else {
                    dataItem.setName(FuncTypeEnums.getByCode(wind).getMark());
                }
                return dataItem;
            }).collect(Collectors.toList());
            deviceDetailVo.setWindItems(dataItems);
            List<JSONArray> jsonArrays = Lists.newArrayList();
            List<String> uvList = getType(FuncTypeEnums.UV.getCode().substring(0, 2), deviceabilityPos);
            if (uvList.size() > 0) {
                List<DeviceDetailVo.OtherItem> uvItems = uvList.stream().map(uv -> {
                    DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                    otherItem.setName(FuncTypeEnums.getByCode(uv).getMark());
                    otherItem.setType(uv);
                    otherItem.setValue(getData(controlDatas, uv));
                    return otherItem;
                }).collect(Collectors.toList());
                JSONArray uv = new JSONArray();
                uv.addAll(uvItems);
                jsonArrays.add(uv);
            }

            List<String> screens = getType(FuncTypeEnums.TIMER_SCREEN.getCode().substring(0, 2), deviceabilityPos);
            if (screens.size() > 0) {
                List<DeviceDetailVo.DataItem> screentItems = screens.stream().map(screenStr -> {
                    DeviceDetailVo.DataItem screen1 = new DeviceDetailVo.DataItem();
                    screen1.setValue(getData(controlDatas, screenStr));
                    screen1.setType(screenStr);
                    screen1.setChoice("0");
                    return screen1;
                }).collect(Collectors.toList());
                deviceDetailVo.setScreens(screentItems);
            }

            List<String> anoins = getType(FuncTypeEnums.ANION.getCode().substring(0, 2), deviceabilityPos);
            if (anoins.size() > 0) {
                List<DeviceDetailVo.OtherItem> anoinsItems = anoins.stream().map(anoin -> {
                    DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                    otherItem.setName(FuncTypeEnums.getByCode(anoin).getMark());
                    otherItem.setType(anoin);
                    otherItem.setValue(getData(controlDatas, anoin));
                    return otherItem;
                }).collect(Collectors.toList());
                JSONArray array = new JSONArray();
                array.addAll(anoinsItems);
                jsonArrays.add(array);
            }

            List<String> warms = getType(FuncTypeEnums.WARM.getCode().substring(0, 2), deviceabilityPos);
            if (warms.size() > 0) {
                List<DeviceDetailVo.OtherItem> warmItems = warms.stream().map(warm -> {
                    DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                    otherItem.setName(FuncTypeEnums.getByCode(warm).getMark());
                    otherItem.setType(warm);
                    otherItem.setValue(getData(controlDatas, warm));
                    return otherItem;
                }).collect(Collectors.toList());
                JSONArray array = new JSONArray();
                array.addAll(warmItems);
                jsonArrays.add(array);
            }

            List<String> humList = getType(FuncTypeEnums.HUMIDIFER.getCode().substring(0, 2), deviceabilityPos);
            if (humList.size() > 0) {
                List<DeviceDetailVo.OtherItem> humItems = humList.stream().map(humStr -> {
                    DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                    otherItem.setName(FuncTypeEnums.getByCode(humStr).getMark());
                    otherItem.setType(humStr);
                    otherItem.setValue(getData(controlDatas, humStr));
                    return otherItem;
                }).collect(Collectors.toList());
                JSONArray array = new JSONArray();
                array.addAll(humItems);
                jsonArrays.add(array);
            }

            List<String> deHumList = getType(FuncTypeEnums.DEHUMIDIFER.getCode().substring(0, 2), deviceabilityPos);
            if (deHumList.size() > 0) {
                List<DeviceDetailVo.OtherItem> dehumItems = deHumList.stream().map(dehum -> {
                    DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                    otherItem.setName(FuncTypeEnums.getByCode(dehum).getMark());
                    otherItem.setType(dehum);
                    otherItem.setValue(getData(controlDatas, dehum));
                    return otherItem;
                }).collect(Collectors.toList());
                JSONArray array = new JSONArray();
                array.addAll(dehumItems);
                jsonArrays.add(array);
            }

            List<String> valves = getType(FuncTypeEnums.VALVE1.getCode().substring(0, 2), deviceabilityPos);
            if (valves.size() > 0) {
                List<DeviceDetailVo.OtherItem> valvesItems = valves.stream().map(valve -> {
                    DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                    if (valves.size() == 1) {
                        otherItem.setName("?????????");
                    } else {
                        otherItem.setName(FuncTypeEnums.getByCode(valve).getMark());
                    }
                    otherItem.setType(valve);
                    otherItem.setValue(getData(controlDatas, valve));
                    return otherItem;
                }).collect(Collectors.toList());
                JSONArray array = new JSONArray();
                array.addAll(valvesItems);
                jsonArrays.add(array);
            }

            List<String> frankList = getType(FuncTypeEnums.FRANKLINISM.getCode().substring(0, 2), deviceabilityPos);
            if (frankList.size() > 0) {
                List<DeviceDetailVo.OtherItem> frankItems = frankList.stream().map(frank -> {
                    DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                    otherItem.setName(FuncTypeEnums.getByCode(frank).getMark());
                    otherItem.setType(frank);
                    otherItem.setValue(getData(controlDatas, frank));
                    return otherItem;
                }).collect(Collectors.toList());
                JSONArray array = new JSONArray();
                array.addAll(frankItems);
                jsonArrays.add(array);
            }

            List<String> heatList = getType(FuncTypeEnums.HEAT.getCode().substring(0, 2), deviceabilityPos);
            if (heatList.size() > 0) {
                List<DeviceDetailVo.OtherItem> heatItems = heatList.stream().map(heat -> {
                    DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                    otherItem.setName(FuncTypeEnums.getByCode(heat).getMark());
                    otherItem.setType(heat);
                    otherItem.setValue(getData(controlDatas, heat));
                    return otherItem;
                }).collect(Collectors.toList());
                JSONArray array = new JSONArray();
                array.addAll(heatItems);
                jsonArrays.add(array);
            }
            deviceDetailVo.setFuncs(jsonArrays);

            List<DeviceDetailVo.OtherItem> timers = Lists.newArrayList();
            deviceDetailVo.setTimers(timers);
            deviceDetailVo.setFuncs(jsonArrays);

            List<String> openList = getType(FuncTypeEnums.TIMER_OEPN.getCode().substring(0, 2), deviceabilityPos);
            if (openList.size() > 0) {
                String open = openList.get(0);
                DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                otherItem.setName(FuncTypeEnums.getByCode(open).getMark());
                otherItem.setType(open);
                data = getData(controlDatas, open);
                if (StringUtils.isNotEmpty(data)) {
                    otherItem.setValue(getData(controlDatas, open));
                } else {
                    otherItem.setValue("0");
                }
                timers.add(otherItem);
            }

            List<String> closeList = getType(FuncTypeEnums.TIMER_CLOSE.getCode().substring(0, 2), deviceabilityPos);
            if (closeList.size() > 0) {
                String close = closeList.get(0);
                DeviceDetailVo.OtherItem otherItem = new DeviceDetailVo.OtherItem();
                otherItem.setName(FuncTypeEnums.getByCode(close).getMark());
                otherItem.setType(close);
                data = getData(controlDatas, close);
                if (StringUtils.isNotEmpty(data)) {
                    otherItem.setValue(getData(controlDatas, close));
                } else {
                    otherItem.setValue("0");
                }
                timers.add(otherItem);
            }
        }
        DeviceDetailVo.DataItem childItem = new DeviceDetailVo.DataItem();
        childItem.setType(FuncTypeEnums.CHILD_LOCK.getCode());
        childItem.setChoice("0:??????,1:??????");
        childItem.setValue(getData(controlDatas, FuncTypeEnums.CHILD_LOCK.getCode()));
        deviceDetailVo.setChildItem(childItem);
    }

    private List<String> getType(String smallType, List<DeviceAbilityPo> deviceabilityPos) {
        List<String> retList = Lists.newArrayList();
        for (DeviceAbilityPo deviceabilityPo : deviceabilityPos) {
            if (deviceabilityPo.getDirValue().startsWith(smallType)) {
                retList.add(deviceabilityPo.getDirValue());
            }
        }
        return retList;
    }

    private String getData(Map<Object, Object> map, String key) {
        if (map.containsKey(key)) {
            return (String) map.get(key);
        }
        return "0";
    }

    private static int getAqi(Integer pm2_5) {
        float[] tbl_aqi = {0f, 50f, 100f, 150f, 200f, 300f, 400f, 500f};
        float[] tbl_pm2_5 = {0f, 35f, 75f, 115f, 150f, 250f, 350f, 500f};
        int i;
        if (pm2_5 > tbl_pm2_5[7]) {
            return (int) tbl_aqi[7];
        }
        for (i = 0; i < 8 - 1; i++) {
            if ((pm2_5 >= tbl_pm2_5[i]) && (pm2_5 < tbl_pm2_5[i + 1])) {
                break;
            }
        }
        return (int) (((tbl_aqi[i + 1] - tbl_aqi[i]) / (tbl_pm2_5[i + 1] - tbl_pm2_5[i]) * (pm2_5 - tbl_pm2_5[i]) + tbl_aqi[i]));
    }

    /*public boolean verifyUser(Integer userId, Integer deviceId) {
        DevicePo devicePo = deviceMapper.selectById(deviceId);
        Integer hostDeviceId = devicePo.getHostDeviceId();
        CustomerUserPo customerUserPo = customerUserMapper.selectById(userId);
        String wxOpenId = customerUserPo.getOpenId();
        DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = new DeviceCustomerUserRelationPo();
        deviceCustomerUserRelationPo.setOpenId(wxOpenId);
        if(hostDeviceId != null){
            deviceCustomerUserRelationPo.setDeviceId(hostDeviceId);
        }else{
            deviceCustomerUserRelationPo.setDeviceId(deviceId);
        }
        Integer count = deviceCustomerUserRelationMapper.queryRelationCount(deviceCustomerUserRelationPo);
        if (count > 0) {
            return true;
        }
        return false;
    }*/
}
