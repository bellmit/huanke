package com.huanke.iot.manage.service.device.group;

import com.alibaba.fastjson.JSON;
import com.huanke.iot.base.api.ApiResponse;
import com.huanke.iot.base.constant.CommonConstant;
import com.huanke.iot.base.constant.DeviceGroupConstants;
import com.huanke.iot.base.constant.RetCode;
import com.huanke.iot.base.dao.customer.CustomerMapper;
import com.huanke.iot.base.dao.device.DeviceGroupItemMapper;
import com.huanke.iot.base.dao.device.DeviceGroupMapper;
import com.huanke.iot.base.dao.device.DeviceGroupSceneMapper;
import com.huanke.iot.base.dao.device.DeviceMapper;
import com.huanke.iot.base.dao.device.ability.DeviceAbilityOptionMapper;
import com.huanke.iot.base.dao.device.data.DeviceOperLogMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelAbilityMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelAbilityOptionMapper;
import com.huanke.iot.base.dao.user.UserManagerMapper;
import com.huanke.iot.base.po.customer.CustomerPo;
import com.huanke.iot.base.po.device.DevicePo;
import com.huanke.iot.base.po.device.ability.DeviceAbilityOptionPo;
import com.huanke.iot.base.po.device.ability.DeviceAbilityPo;
import com.huanke.iot.base.po.device.data.DeviceOperLogPo;
import com.huanke.iot.base.po.device.group.DeviceGroupItemPo;
import com.huanke.iot.base.po.device.group.DeviceGroupPo;
import com.huanke.iot.base.po.device.group.DeviceGroupScenePo;
import com.huanke.iot.base.po.device.typeModel.DeviceModelAbilityOptionPo;
import com.huanke.iot.base.po.user.User;
import com.huanke.iot.base.resp.BaseIdNameRsp;
import com.huanke.iot.manage.service.customer.CustomerService;
import com.huanke.iot.manage.service.gateway.MqttSendService;
import com.huanke.iot.manage.service.user.UserService;
import com.huanke.iot.manage.vo.request.device.group.FuncListMessage;
import com.huanke.iot.manage.vo.request.device.group.GroupControlRequest;
import com.huanke.iot.manage.vo.request.device.group.GroupCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.request.device.group.GroupQueryRequest;
import com.huanke.iot.manage.vo.request.device.operate.DeviceFuncRequest;
import com.huanke.iot.manage.vo.request.device.operate.DeviceQueryRequest;
import com.huanke.iot.manage.vo.response.device.group.DeviceGroupDetailVo;
import com.huanke.iot.manage.vo.response.device.group.DeviceGroupListVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DeviceGroupService {

    public static final String MEMO = "1??????????????????????????????????????????????????????????????? \n" +
            "2?????????????????????????????????????????????????????????????????????????????????????????????git??????????????? \n" +
            "3??????????????????????????????????????????????????????????????????????????????????????????????????? \n" +
            "4??????????????????????????????PM2.5?????????????????????????????? \n" +
            "5??????????????????????????????????????????????????????????????????99.9%????????? \n" +
            "6????????????????????????????????????????????????????????????????????????????????????????????? \n" +
            "7???????????????????????????????????????????????????????????????90%????????? \n" +
            "8?????????????????????????????????????????????????????????????????????????????????????????? \n" +
            "9???????????????????????????????????????????????????????????????????????????????????????????????????";

    public static final String DEFAULT_COVER = "http://idcfota.oss-cn-hangzhou.aliyuncs.com/group.png";

    public static final String DEFAULT_VIDEO_URl = "http://idcfota.oss-cn-hangzhou.aliyuncs.com/default.mp4";

    public static final String DEFAULT_ICON = "http://idcfota.oss-cn-hangzhou.aliyuncs.com/group.png";

    @Autowired
    private DeviceGroupMapper deviceGroupMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceGroupItemMapper deviceGroupItemMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private DeviceOperLogMapper deviceOperLogMapper;

    @Autowired
    private DeviceGroupSceneMapper deviceGroupSceneMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private MqttSendService mqttSendService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private UserManagerMapper userManagerMapper;

    @Autowired
    private DeviceAbilityOptionMapper deviceAbilityOptionMapper;

    @Autowired
    private DeviceModelAbilityMapper deviceModelAbilityMapper;

    @Autowired
    private DeviceModelAbilityOptionMapper deviceModelAbilityOptionMapper;

    /**
     * 2018-08-18
     * ????????????????????????????????????????????????
     * sixiaojun
     *
     * @param groupCreateOrUpdateRequest
     * @return
     */
    public ApiResponse<Integer> createOrUpdateGroup(GroupCreateOrUpdateRequest groupCreateOrUpdateRequest) throws Exception {
        DeviceGroupPo insertOrUpdatePo = new DeviceGroupPo();
        User user = userService.getCurrentUser();
        BeanUtils.copyProperties(groupCreateOrUpdateRequest, insertOrUpdatePo);
        if (null != groupCreateOrUpdateRequest.getId() && 0 < groupCreateOrUpdateRequest.getId()) {
            //???id??????????????????????????????
            insertOrUpdatePo.setLastUpdateTime(System.currentTimeMillis());
            insertOrUpdatePo.setLastUpdateUser(user.getId());
            this.deviceGroupMapper.updateById(insertOrUpdatePo);
        }
        //??????????????????
        else {
            insertOrUpdatePo.setStatus(CommonConstant.STATUS_YES);
            insertOrUpdatePo.setCreateTime(System.currentTimeMillis());
            insertOrUpdatePo.setLastUpdateTime(System.currentTimeMillis());
            insertOrUpdatePo.setLastUpdateUser(user.getId());
            insertOrUpdatePo.setCreateUser(user.getId());
            this.deviceGroupMapper.insert(insertOrUpdatePo);
            //?????????????????????groupId
            groupCreateOrUpdateRequest.setId(insertOrUpdatePo.getId());
        }
        //???????????????????????????
        List<DeviceGroupScenePo> deviceGroupImgScenePoList = this.deviceGroupSceneMapper.selectImgVideoList(groupCreateOrUpdateRequest.getId(),DeviceGroupConstants.IMAGE_VIDEO_MARK_IMAGE);
        //?????????????????????????????????????????????
        if (null != deviceGroupImgScenePoList && 0 < deviceGroupImgScenePoList.size()) {
            this.deviceGroupSceneMapper.deleteBatch(deviceGroupImgScenePoList);
        }
        //???????????????????????????
        List<DeviceGroupScenePo> deviceGroupVideoScenePoList = this.deviceGroupSceneMapper.selectImgVideoList(groupCreateOrUpdateRequest.getId(),DeviceGroupConstants.IMAGE_VIDEO_MARK_VIDEO);
        //?????????????????????????????????????????????
        if (null != deviceGroupVideoScenePoList && 0 < deviceGroupVideoScenePoList.size()) {
            this.deviceGroupSceneMapper.deleteBatch(deviceGroupVideoScenePoList);
        }
        deviceGroupImgScenePoList.clear();
        deviceGroupVideoScenePoList.clear();
        //??????????????????????????????
        if (null != groupCreateOrUpdateRequest.getImagesList() && 0 < groupCreateOrUpdateRequest.getImagesList().size()) {
            groupCreateOrUpdateRequest.getImagesList().stream().forEach(image -> {
                DeviceGroupScenePo deviceGroupScenePo = new DeviceGroupScenePo();
                deviceGroupScenePo.setGroupId(groupCreateOrUpdateRequest.getId());
                deviceGroupScenePo.setImgVideo(image.getImage());
                deviceGroupScenePo.setImgVideoMark(DeviceGroupConstants.IMAGE_VIDEO_MARK_IMAGE);
                deviceGroupScenePo.setCreateTime(System.currentTimeMillis());
                deviceGroupScenePo.setLastUpdateTime(System.currentTimeMillis());
                deviceGroupScenePo.setStatus(CommonConstant.STATUS_YES);
                deviceGroupImgScenePoList.add(deviceGroupScenePo);
            });
            this.deviceGroupSceneMapper.insertBatch(deviceGroupImgScenePoList);
        }
        //??????????????????????????????
        if (null != groupCreateOrUpdateRequest.getVideosList() && 0 < groupCreateOrUpdateRequest.getVideosList().size()) {
            groupCreateOrUpdateRequest.getVideosList().stream().forEach(video -> {
                DeviceGroupScenePo deviceGroupScenePo = new DeviceGroupScenePo();
                deviceGroupScenePo.setGroupId(groupCreateOrUpdateRequest.getId());
                deviceGroupScenePo.setImgVideo(video.getVideo());
                deviceGroupScenePo.setImgVideoMark(DeviceGroupConstants.IMAGE_VIDEO_MARK_VIDEO);
                deviceGroupScenePo.setCreateTime(System.currentTimeMillis());
                deviceGroupScenePo.setLastUpdateTime(System.currentTimeMillis());
                deviceGroupScenePo.setStatus(CommonConstant.STATUS_YES);
                deviceGroupVideoScenePoList.add(deviceGroupScenePo);
            });
            this.deviceGroupSceneMapper.insertBatch(deviceGroupVideoScenePoList);
        }
        //????????????????????????
        //???????????????????????????????????????????????????
        if (null == groupCreateOrUpdateRequest.getDeviceList() || 0 == groupCreateOrUpdateRequest.getDeviceList().size()) {
            return new ApiResponse<>(RetCode.OK, "?????????????????????", insertOrUpdatePo.getId());
        } else {
            //???????????????????????????????????????
            List<DeviceGroupItemPo> deviceGroupItemPoList = this.deviceGroupItemMapper.selectByGroupId(insertOrUpdatePo.getId());
            //????????????????????????????????????
            if (null != deviceGroupItemPoList && 0 < deviceGroupItemPoList.size()) {
                this.deviceGroupItemMapper.deleteBatch(deviceGroupItemPoList);
            }
            deviceGroupItemPoList.clear();
            //??????????????????
            for (DeviceQueryRequest.DeviceQueryList deviceList : groupCreateOrUpdateRequest.getDeviceList()) {
                DevicePo devicePo = this.deviceMapper.selectByMac(deviceList.getMac());
                DeviceGroupItemPo deviceGroupItemPo = new DeviceGroupItemPo();
                deviceGroupItemPo.setGroupId(insertOrUpdatePo.getId());
                deviceGroupItemPo.setDeviceId(devicePo.getId());
                deviceGroupItemPo.setStatus(CommonConstant.STATUS_YES);
                deviceGroupItemPo.setCreateTime(System.currentTimeMillis());
                deviceGroupItemPo.setLastUpdateTime(System.currentTimeMillis());
                deviceGroupItemPoList.add(deviceGroupItemPo);
            }
            //??????????????????
            this.deviceGroupItemMapper.insertBatch(deviceGroupItemPoList);
            return new ApiResponse<>(RetCode.OK, "???????????????????????????", insertOrUpdatePo.getId());
        }
    }

    /**
     * ?????????????????????
     * 2018-09-25
     * sixiaojun
     *
     * @param groupQueryRequest
     * @return
     * @throws Exception
     */
    public ApiResponse<List<DeviceGroupListVo>> queryGroupByPage(GroupQueryRequest groupQueryRequest) throws Exception {
        //????????????????????????????????????
        Integer customerId = customerService.obtainCustomerId(false);

        List<DeviceGroupListVo> deviceGroupListVoList = new ArrayList<>();
        List<DeviceGroupPo> deviceGroupPoList = new ArrayList<>();
        Integer offset = (groupQueryRequest.getPage() - 1) * groupQueryRequest.getLimit();
        Integer limit = groupQueryRequest.getLimit();
        DeviceGroupPo queryPo = new DeviceGroupPo();
        queryPo.setStatus(null);
        BeanUtils.copyProperties(groupQueryRequest, queryPo);

        if(queryPo.getCustomerId()==null){
            queryPo.setCustomerId(customerId);
        }
        //???????????????????????????????????????????????????????????????????????????????????????
        deviceGroupPoList = this.deviceGroupMapper.selectList(queryPo, limit, offset);
        if(null == deviceGroupPoList || 0 == deviceGroupPoList.size()){
            return new ApiResponse<>(RetCode.OK,"????????????");
        }
        deviceGroupPoList.stream().forEach(deviceGroupPo -> {
            DeviceGroupListVo deviceGroupListVo = new DeviceGroupListVo();
            deviceGroupListVo.setId(deviceGroupPo.getId());
            deviceGroupListVo.setName(deviceGroupPo.getName());
            deviceGroupListVo.setIntroduction(deviceGroupPo.getIntroduction());
            deviceGroupListVo.setRemark(deviceGroupPo.getRemark());
            deviceGroupListVo.setStatus(deviceGroupPo.getStatus());
            deviceGroupListVo.setLocation(deviceGroupPo.getLocation());
            deviceGroupListVo.setCreateUser(this.userService.getUserName(deviceGroupPo.getCreateUser()));
            deviceGroupListVo.setLastUpdateUser(this.userService.getUserName(deviceGroupPo.getLastUpdateUser()));
            //?????????????????????????????????
            CustomerPo customerPo = this.customerMapper.selectById(deviceGroupPo.getCustomerId());
            deviceGroupListVo.setCustomerId(customerPo.getId());
            deviceGroupListVo.setCustomerName(customerPo.getName());
            //?????????????????????
            List<DeviceGroupItemPo> deviceGroupItemPoList = this.deviceGroupItemMapper.selectByGroupId(deviceGroupPo.getId());
            if (null != deviceGroupItemPoList && 0 < deviceGroupItemPoList.size()) {
                List<DeviceGroupListVo.DeviceInGroup> deviceInGroupList = new ArrayList<>();
                deviceGroupItemPoList.stream().forEach(deviceGroupItemPo -> {
                    DeviceGroupListVo.DeviceInGroup device = new DeviceGroupListVo.DeviceInGroup();
                    DevicePo devicePo = this.deviceMapper.selectById(deviceGroupItemPo.getDeviceId());
                    BeanUtils.copyProperties(devicePo, device);
                    deviceInGroupList.add(device);
                });
                deviceGroupListVo.setDeviceList(deviceInGroupList);
            }
            //????????????????????????????????????
            deviceGroupListVo.setDeviceCount(deviceGroupItemPoList.size());
            deviceGroupListVo.setCreateTime(deviceGroupPo.getCreateTime());
            deviceGroupListVoList.add(deviceGroupListVo);
        });
        return new ApiResponse<>(RetCode.OK, "??????????????????", deviceGroupListVoList);
    }


    /**
     * ??????????????????
     * sixiaojun
     * 2018-09-25
     *
     * @return
     * @throws Exception
     */
    public ApiResponse<Integer> queryGroupCount(Integer status) throws Exception {
        Integer groupCount;
        //????????????????????????????????????
        Integer customerId = customerService.obtainCustomerId(false);
        DeviceGroupPo deviceGroupPo = new DeviceGroupPo();
        deviceGroupPo.setStatus(status);
        deviceGroupPo.setCustomerId(customerId);
        if(null != deviceGroupPo.getCustomerId()) {
            groupCount = this.deviceGroupMapper.selectCount(deviceGroupPo);
        }else {
            groupCount = this.deviceGroupMapper.selectAllCount(deviceGroupPo);
        }
        return new ApiResponse<>(RetCode.OK, "????????????????????????", groupCount);
    }

    public ApiResponse<List<DeviceGroupPo>> queryAllGroup(){
        Integer offset = 0;
        Integer limit = 100000;
        //????????????????????????????????????
        Integer customerId = customerService.obtainCustomerId(false);
        DeviceGroupPo queryPo = new DeviceGroupPo();
        if(customerId != null){
            queryPo.setCustomerId(customerId);
            CustomerPo customerPo = this.customerMapper.selectById(customerId);
            if(null != customerPo.getParentCustomerId()){
                queryPo.setParentCustomerId(customerPo.getParentCustomerId());
            }
        }
        List<DeviceGroupPo> deviceGroupPoList = this.deviceGroupMapper.selectList(queryPo,limit,offset);
        if(null == deviceGroupPoList || 0 == deviceGroupPoList.size()){
            return new ApiResponse<>(RetCode.OK,"????????????");
        }
        return new ApiResponse<>(RetCode.OK,"????????????????????????",deviceGroupPoList);
    }

    /**
     * ??????id??????????????????
     * sixiaojun
     * 2018-09-26
     *
     * @param groupId
     * @return
     * @throws Exception
     */
    public ApiResponse<DeviceGroupDetailVo> queryGroupById(Integer groupId) throws Exception {
        DeviceGroupDetailVo deviceGroupDetailVo = new DeviceGroupDetailVo();
        DeviceGroupPo deviceGroupPo = this.deviceGroupMapper.selectById(groupId);
        if (null != deviceGroupPo) {
            BeanUtils.copyProperties(deviceGroupPo, deviceGroupDetailVo);
            deviceGroupDetailVo.setCreateUser(this.userService.getUserName(deviceGroupPo.getCreateUser()));
            deviceGroupDetailVo.setLastUpdateUser(this.userService.getUserName(deviceGroupPo.getLastUpdateUser()));
            //?????????????????????
            List<DeviceGroupItemPo> deviceGroupItemPoList = this.deviceGroupItemMapper.selectByGroupId(groupId);
            if (null != deviceGroupItemPoList && 0 < deviceGroupItemPoList.size()) {
                List<DeviceGroupDetailVo.DeviceInGroup> deviceInGroupList = new ArrayList<>();
                deviceGroupItemPoList.stream().forEach(deviceGroupItemPo -> {
                    DeviceGroupDetailVo.DeviceInGroup device = new DeviceGroupDetailVo.DeviceInGroup();
                    DevicePo devicePo = this.deviceMapper.selectById(deviceGroupItemPo.getDeviceId());
                    BeanUtils.copyProperties(devicePo, device);
                    deviceInGroupList.add(device);
                });
                deviceGroupDetailVo.setDeviceList(deviceInGroupList);
            }
            //??????????????????
            List<DeviceGroupScenePo> deviceGroupImgScenePoList = this.deviceGroupSceneMapper.selectImgVideoList(deviceGroupPo.getId(),DeviceGroupConstants.IMAGE_VIDEO_MARK_IMAGE);
            if (null != deviceGroupImgScenePoList && 0 < deviceGroupImgScenePoList.size()) {
                List<DeviceGroupDetailVo.Images> imagesList = new ArrayList<>();
                deviceGroupImgScenePoList.stream().forEach(eachPo -> {
                    DeviceGroupDetailVo.Images image = new DeviceGroupDetailVo.Images();
                    image.setImage(eachPo.getImgVideo());
                    imagesList.add(image);
                });
                deviceGroupDetailVo.setImagesList(imagesList);
            }
            //??????????????????
            List<DeviceGroupScenePo> deviceGroupVideoScenePoList = this.deviceGroupSceneMapper.selectImgVideoList(deviceGroupPo.getId(),DeviceGroupConstants.IMAGE_VIDEO_MARK_VIDEO);
            if (null != deviceGroupVideoScenePoList && 0 < deviceGroupVideoScenePoList.size()) {
                List<DeviceGroupDetailVo.Videos> videosList = new ArrayList<>();
                deviceGroupVideoScenePoList.stream().forEach(eachPo -> {
                    DeviceGroupDetailVo.Videos video = new DeviceGroupDetailVo.Videos();
                    video.setVideo(eachPo.getImgVideo());
                    videosList.add(video);
                });
                deviceGroupDetailVo.setVideosList(videosList);
            }
            return new ApiResponse<>(RetCode.OK, "????????????????????????", deviceGroupDetailVo);
        } else {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????");
        }
    }

    public ApiResponse<Boolean> deleteOneGroup(Integer groupId) throws Exception {
        DeviceGroupPo deviceGroupPo = this.deviceGroupMapper.selectById(groupId);
        if (null != deviceGroupPo) {
            //??????????????????????????????
            List<DeviceGroupItemPo> deviceGroupItemPoList = this.deviceGroupItemMapper.selectByGroupId(groupId);
            if (null != deviceGroupItemPoList && 0 < deviceGroupItemPoList.size()) {
                //????????????????????????
                this.deviceGroupItemMapper.deleteBatch(deviceGroupItemPoList);
            }
            //?????????????????????????????????
            this.deviceGroupSceneMapper.deleteByGroupId(groupId);
            //??????????????????
            this.deviceGroupMapper.deleteById(deviceGroupPo.getId());
            return new ApiResponse<>(RetCode.OK, "??????????????????", true);
        } else {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????", false);
        }
    }

    public ApiResponse<Boolean> sendGroupFunc(GroupControlRequest groupControlRequest,int operType) throws Exception{
        //???????????????????????????
        User user = this.userService.getCurrentUser();
//        User currentUser = this.userManagerMapper.selectByUserName(user.getUserName());
        List<Integer> deviceIdList = groupControlRequest.getDeviceIdList();
        String funcId = groupControlRequest.getFuncId();
        String value = groupControlRequest.getValue();
        for (Integer deviceId : deviceIdList) {
            DeviceFuncRequest deviceFuncRequest = new DeviceFuncRequest();
            deviceFuncRequest.setDeviceId(deviceId);
            deviceFuncRequest.setFuncId(funcId);
            deviceFuncRequest.setValue(value);
            String requestId = sendFunc(deviceFuncRequest, user.getId(), operType);
        }
        return new ApiResponse<>(RetCode.OK,"/????????????",true);
    }


    public String sendFunc(DeviceFuncRequest deviceFuncRequest, Integer userId, Integer operType) {
        DevicePo devicePo = deviceMapper.selectById(deviceFuncRequest.getDeviceId());
        if (devicePo != null) {
            //???????????????????????????????????????
            List<DeviceAbilityPo> deviceAbilityPos = deviceModelAbilityMapper.selectActiveByModelId(devicePo.getModelId());
            List<DeviceAbilityOptionPo> deviceAbilityOptionPos = new ArrayList<>();
            List<DeviceModelAbilityOptionPo> deviceModelAbilityOptionPos = new ArrayList<>();
            deviceAbilityPos.stream().filter(temp ->{return temp.getDirValue().equals(deviceFuncRequest.getFuncId());}).forEach(deviceAbilityPo-> {
                deviceAbilityOptionPos.addAll(deviceAbilityOptionMapper.selectActiveOptionsByAbilityId(deviceAbilityPo.getId()));
                deviceModelAbilityOptionPos.addAll(deviceModelAbilityOptionMapper.queryByModelIdAbilityId(devicePo.getModelId(), deviceAbilityPo.getId()));
            });
            Integer optionId = null;
            String actualValue = deviceFuncRequest.getValue();
            for (DeviceAbilityOptionPo temp : deviceAbilityOptionPos){
                if (deviceFuncRequest.getValue().equals(temp.getOptionValue())){
                    optionId = temp.getId();
                    break;
                }
            }
            for (DeviceModelAbilityOptionPo temp : deviceModelAbilityOptionPos){
                if (temp.getAbilityOptionId().equals(optionId)&& StringUtils.isNotEmpty(temp.getActualOptionValue())){
                    actualValue = temp.getActualOptionValue();
                    break;
                }
            }
            //????????????

            Integer deviceId = devicePo.getId();
            String topic = "/down2/control/" + deviceId;
            String requestId = UUID.randomUUID().toString().replace("-", "");
            DeviceOperLogPo deviceOperLogPo = new DeviceOperLogPo();
            deviceOperLogPo.setFuncId(deviceFuncRequest.getFuncId());
            deviceOperLogPo.setDeviceId(deviceId);
            deviceOperLogPo.setOperType(operType);
            deviceOperLogPo.setOperUserId(userId);
            deviceOperLogPo.setFuncValue(deviceFuncRequest.getValue());
            deviceOperLogPo.setRequestId(requestId);
            deviceOperLogPo.setCreateTime(System.currentTimeMillis());
            deviceOperLogMapper.insert(deviceOperLogPo);
            FuncListMessage funcListMessage = new FuncListMessage();
            funcListMessage.setMsg_type("control");
            funcListMessage.setMsg_id(requestId);
            FuncListMessage.FuncItemMessage funcItemMessage = new FuncListMessage.FuncItemMessage();
            funcItemMessage.setType(deviceFuncRequest.getFuncId());
            funcItemMessage.setValue(actualValue);
            funcListMessage.setDatas(Lists.newArrayList(funcItemMessage));
            mqttSendService.sendMessage(topic, JSON.toJSONString(funcListMessage));
            stringRedisTemplate.opsForHash().put("control2." + deviceId, funcItemMessage.getType(), String.valueOf(funcItemMessage.getValue()));
            log.info(requestId);
            return requestId;
        }
        return "";
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @param deviceLists
     * @return
     */
    public DeviceGroupPo queryGroupName(List<DeviceQueryRequest.DeviceQueryList> deviceLists) {
        DeviceGroupPo deviceGroupPo = null;
        for (DeviceQueryRequest.DeviceQueryList deviceList : deviceLists) {
            DevicePo devicePo = deviceMapper.selectByMac(deviceList.getMac());
            //?????????????????????????????????????????????????????????????????????
            if (null != deviceGroupItemMapper.selectByDeviceId(devicePo.getId())) {
                DeviceGroupItemPo deviceGroupItemPo = deviceGroupItemMapper.selectByDeviceId(devicePo.getId());
                deviceGroupPo = deviceGroupMapper.selectById(deviceGroupItemPo.getGroupId());
            }
        }
        return deviceGroupPo;
    }



    /**
     * ??????????????????????????????????????????????????????
     *
     * @param deviceLists
     * @return
     */
    public Boolean isGroupConflict(List<DeviceQueryRequest.DeviceQueryList> deviceLists) {
        //?????????????????????????????????????????????????????????ID
        int compareGroupId = -1;
        for (DeviceQueryRequest.DeviceQueryList device : deviceLists) {
            DevicePo devicePo = deviceMapper.selectByMac(device.getMac());
            if (null != deviceGroupItemMapper.selectByDeviceId(devicePo.getId())) {
                compareGroupId = deviceGroupItemMapper.selectByDeviceId(devicePo.getId()).getGroupId();
            }
        }
        //???????????????????????????????????????
        if (-1 != compareGroupId) {
            for (DeviceQueryRequest.DeviceQueryList device : deviceLists) {
                DevicePo devicePo = deviceMapper.selectByMac(device.getMac());
                if (null != deviceGroupItemMapper.selectByDeviceId(devicePo.getId())) {
                    Integer currentGroupId = deviceGroupItemMapper.selectByDeviceId(devicePo.getId()).getGroupId();
                    //????????????????????????ID???????????????????????????ID???????????????????????????????????????????????????????????????
                    if (compareGroupId != currentGroupId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 2018-08-20
     * sixiaojun
     * ??????mac??????????????????????????????????????????mac????????????????????????????????????DevicePo???????????????
     *
     * @param deviceList
     * @return devicePo
     */
    public DeviceQueryRequest.DeviceQueryList queryDeviceByMac(GroupCreateOrUpdateRequest deviceList) {
        for (DeviceQueryRequest.DeviceQueryList device : deviceList.getDeviceList()) {
            DevicePo devicePo = deviceMapper.selectByMac(device.getMac());
            if (null == devicePo) {
                return device;
            }
        }
        return null;
    }

    public List<BaseIdNameRsp> queryDevices(Integer groupId) {
        return deviceGroupItemMapper.queryDevices(groupId);
    }
//
//
//    public List<DeviceGroupItemVo> selectList(int page) {
//        //?????????????????????
//        Integer currentPage = page;
//        //?????????????????????
//        Integer limit= 20;
//        //?????????
//        Integer offset = (currentPage - 1) * limit;
//        DeviceGroupPo queryGroup = new DeviceGroupPo();
//        List<DeviceGroupPo> deviceGroupPos = deviceGroupMapper.selectList(queryGroup, limit, offset);
//        return deviceGroupPos.stream().map(deviceGroupPo -> {
//            DeviceGroupItemVo itemVo = new DeviceGroupItemVo();
//            String icon = deviceGroupPo.getIcon();
//            if (StringUtils.isEmpty(icon)) {
//                icon = DEFAULT_ICON;
//            }
//
//            String videoUrl = deviceGroupPo.getVideoUrl();
//            if (StringUtils.isEmpty(videoUrl)) {
//                videoUrl = DEFAULT_VIDEO_URl;
//            }
//
//            String videoCover = deviceGroupPo.getVideoCover();
//            if (StringUtils.isEmpty(videoCover)) {
//                videoCover = DEFAULT_COVER;
//            }
//
//            String memo = deviceGroupPo.getMemo();
//            if (StringUtils.isEmpty(memo)) {
//                memo = MEMO;
//            }
//            itemVo.setGroupName(deviceGroupPo.getGroupName());
//            itemVo.setId(deviceGroupPo.getId());
//            itemVo.setIcon(icon);
//            Integer userId = deviceGroupPo.getUserId();
//            AppUserPo appUserPo = appUserMapper.selectById(userId);
//            if (appUserPo != null) {
//                itemVo.setMaskNickname(appUserPo.getNickname());
//            }
//            itemVo.setMemo(memo);
//            itemVo.setVideoCover(videoCover);
//            itemVo.setVideoUrl(videoUrl);
//            return itemVo;
//        }).collect(Collectors.toList());
//    }
//
//
//    public void updateGroup(DeviceGroupPo deviceGroupPo) {
//        deviceGroupMapper.updateById(deviceGroupPo);
//    }
}