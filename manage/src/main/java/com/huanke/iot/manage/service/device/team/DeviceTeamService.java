package com.huanke.iot.manage.service.device.team;

import com.huanke.iot.base.api.ApiResponse;
import com.huanke.iot.base.constant.CommonConstant;
import com.huanke.iot.base.constant.DeviceConstant;
import com.huanke.iot.base.constant.DeviceTeamConstants;
import com.huanke.iot.base.constant.RetCode;
import com.huanke.iot.base.dao.customer.CustomerMapper;
import com.huanke.iot.base.dao.customer.CustomerUserMapper;
import com.huanke.iot.base.dao.device.*;
import com.huanke.iot.base.exception.BusinessException;
import com.huanke.iot.base.po.customer.CustomerPo;
import com.huanke.iot.base.po.customer.CustomerUserPo;
import com.huanke.iot.base.po.device.DeviceCustomerRelationPo;
import com.huanke.iot.base.po.device.DeviceCustomerUserRelationPo;
import com.huanke.iot.base.po.device.DevicePo;
import com.huanke.iot.base.po.device.team.DeviceTeamItemPo;
import com.huanke.iot.base.po.device.team.DeviceTeamPo;
import com.huanke.iot.base.po.device.team.DeviceTeamScenePo;
import com.huanke.iot.manage.service.customer.CustomerService;
import com.huanke.iot.manage.vo.request.device.operate.QueryInfoByCustomerRequest;
import com.huanke.iot.manage.vo.request.device.team.TeamCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.request.device.team.TeamDeleteRequest;
import com.huanke.iot.manage.vo.request.device.team.TeamListQueryRequest;
import com.huanke.iot.manage.vo.request.device.team.TeamTrusteeRequest;
import com.huanke.iot.manage.vo.response.device.team.DeviceTeamVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DeviceTeamService {
    @Autowired
    private DeviceTeamMapper deviceTeamMapper;

    @Autowired
    private DeviceTeamSceneMapper deviceTeamSceneMapper;

    @Autowired
    private DeviceTeamItemMapper deviceTeamItemMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private CustomerUserMapper customerUserMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private DeviceCustomerRelationMapper deviceCustomerRelationMapper;

    @Autowired
    private DeviceCustomerUserRelationMapper deviceCustomerUserRelationMapper;

    @Autowired
    private CustomerService customerService;


    /**
     * ????????????????????????
     * 2018-08-29
     * sixiaojun
     *
     * @param teamCreateOrUpdateRequest
     * @return
     */
    public ApiResponse<Integer> createNewOrUpdateTeam(TeamCreateOrUpdateRequest teamCreateOrUpdateRequest) throws Exception {
        DeviceTeamPo deviceTeamPo = new DeviceTeamPo();
        //??????????????????openId???????????????userId
        CustomerUserPo customerUserPo = this.customerUserMapper.selectByOpenId(teamCreateOrUpdateRequest.getCreateUserOpenId());
        //???????????????????????????????????????customer???????????????customerId????????????customer
        deviceTeamPo.setCustomerId(customerUserPo.getCustomerId());
        deviceTeamPo.setName(teamCreateOrUpdateRequest.getName());
        deviceTeamPo.setIcon(teamCreateOrUpdateRequest.getIcon());
        //??????
        deviceTeamPo.setVideoCover(teamCreateOrUpdateRequest.getCover());
        //??????????????????????????????????????????materUser????????????
        deviceTeamPo.setCreateUserId(customerUserPo.getId());
        deviceTeamPo.setMasterUserId(customerUserPo.getId());
        //??????????????????????????????
        deviceTeamPo.setTeamStatus(DeviceTeamConstants.DEVICE_TEAM_STATUS_TERMINAL);
        deviceTeamPo.setTeamType(DeviceTeamConstants.DEVICE_TEAM_TYPE_USER);
        deviceTeamPo.setSceneDescription(teamCreateOrUpdateRequest.getSceneDescription());
        //???id?????????????????????????????????
        if (null != teamCreateOrUpdateRequest.getId() && 0 < teamCreateOrUpdateRequest.getId()) {
            deviceTeamPo.setId(teamCreateOrUpdateRequest.getId());
            deviceTeamPo.setLastUpdateTime(System.currentTimeMillis());
            this.deviceTeamMapper.updateById(deviceTeamPo);
        }
        //??????????????????
        else {
            //????????????????????????????????????
            deviceTeamPo.setStatus(CommonConstant.STATUS_YES);
            deviceTeamPo.setCreateTime(System.currentTimeMillis());
            deviceTeamPo.setLastUpdateTime(System.currentTimeMillis());
            //??????????????????
            this.deviceTeamMapper.insert(deviceTeamPo);
        }
        List<DeviceTeamScenePo> deviceTeamImgScenePoList = this.deviceTeamSceneMapper.selectImgVideoList(deviceTeamPo.getId(), DeviceTeamConstants.IMAGE_VIDEO_MARK_IMAGE);
        //??????????????????????????????
        if (null != deviceTeamImgScenePoList && 0 < deviceTeamImgScenePoList.size()) {
            this.deviceTeamSceneMapper.deleteBatch(deviceTeamImgScenePoList);
        }
        List<DeviceTeamScenePo> deviceTeamVideoScenePoList = this.deviceTeamSceneMapper.selectImgVideoList(deviceTeamPo.getId(), DeviceTeamConstants.IMAGE_VIDEO_MARK_VIDEO);
        //??????????????????????????????
        if (null != deviceTeamVideoScenePoList && 0 < deviceTeamVideoScenePoList.size()) {
            this.deviceTeamSceneMapper.deleteBatch(deviceTeamVideoScenePoList);
        }
        deviceTeamImgScenePoList.clear();
        deviceTeamVideoScenePoList.clear();
        //????????????????????????
        if (teamCreateOrUpdateRequest.getImagesList() != null && teamCreateOrUpdateRequest.getImagesList().size() > 0) {
            teamCreateOrUpdateRequest.getImagesList().stream().forEach(image -> {
                DeviceTeamScenePo deviceTeamScenePo = new DeviceTeamScenePo();
                deviceTeamScenePo.setTeamId(deviceTeamPo.getId());
                deviceTeamScenePo.setImgVideo(image.getImage());
                deviceTeamScenePo.setImgVideoMark(DeviceTeamConstants.IMAGE_VIDEO_MARK_IMAGE);
                deviceTeamScenePo.setStatus(CommonConstant.STATUS_YES);
                deviceTeamScenePo.setCreateTime(System.currentTimeMillis());
                deviceTeamScenePo.setLastUpdateTime(System.currentTimeMillis());
                deviceTeamImgScenePoList.add(deviceTeamScenePo);
            });
            this.deviceTeamSceneMapper.insertBatch(deviceTeamImgScenePoList);
        }
        //????????????????????????
        if (teamCreateOrUpdateRequest.getVideosList() != null && teamCreateOrUpdateRequest.getVideosList().size() > 0) {
            teamCreateOrUpdateRequest.getVideosList().stream().forEach(video -> {
                DeviceTeamScenePo deviceTeamScenePo = new DeviceTeamScenePo();
                deviceTeamScenePo.setTeamId(deviceTeamPo.getId());
                deviceTeamScenePo.setImgVideo(video.getVideo());
                deviceTeamScenePo.setImgVideoMark(DeviceTeamConstants.IMAGE_VIDEO_MARK_VIDEO);
                deviceTeamScenePo.setStatus(CommonConstant.STATUS_YES);
                deviceTeamScenePo.setCreateTime(System.currentTimeMillis());
                deviceTeamScenePo.setLastUpdateTime(System.currentTimeMillis());
                deviceTeamVideoScenePoList.add(deviceTeamScenePo);
            });
            this.deviceTeamSceneMapper.insertBatch(deviceTeamVideoScenePoList);
        }
        if (null == teamCreateOrUpdateRequest.getTeamDeviceCreateRequestList() || 0 == teamCreateOrUpdateRequest.getTeamDeviceCreateRequestList().size()) {
            return new ApiResponse<>(RetCode.OK, "????????????????????????", deviceTeamPo.getId());
        }
        //?????????????????????
        else {
            Boolean deviceLinkAge = false;
            List<DevicePo> devicePoList = new ArrayList<>();
            //?????????????????????????????????????????????
            List<DeviceTeamItemPo> deviceTeamItemPoList = this.deviceTeamItemMapper.selectByTeamId(deviceTeamPo.getId());
            //??????????????????????????????
            if (null != deviceTeamItemPoList && 0 < deviceTeamItemPoList.size()) {
                deviceTeamItemPoList.stream().forEach(deviceTeamItemPo -> {
                    DevicePo devicePo = this.deviceMapper.selectById(deviceTeamItemPo.getDeviceId());
                    devicePo.setBindStatus(null);
                    devicePo.setBindTime(null);
                    devicePoList.add(devicePo);
                });
                this.deviceMapper.updateBatch(devicePoList);
                this.deviceTeamItemMapper.deleteBatch(deviceTeamItemPoList);
            }
            List<DeviceCustomerUserRelationPo> deviceCustomerUserRelationPoList = this.deviceCustomerUserRelationMapper.selectByOpenId(teamCreateOrUpdateRequest.getCreateUserOpenId());
            //???????????????????????????????????????
            if (null != deviceCustomerUserRelationPoList && 0 < deviceCustomerUserRelationPoList.size()) {
                this.deviceCustomerUserRelationMapper.deleteBatch(deviceCustomerUserRelationPoList);
            }
            deviceTeamItemPoList.clear();
            deviceCustomerUserRelationPoList.clear();
            devicePoList.clear();
            CustomerPo customerPo = this.customerMapper.selectByTeamId(deviceTeamPo.getId());
            for (TeamCreateOrUpdateRequest.TeamDeviceCreateRequest device : teamCreateOrUpdateRequest.getTeamDeviceCreateRequestList()) {
                DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = new DeviceCustomerUserRelationPo();
                DeviceTeamItemPo deviceTeamItemPo = new DeviceTeamItemPo();
                DevicePo devicePo = this.deviceMapper.selectByMac(device.getMac());
                devicePo.setName(device.getName());
                //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                devicePo.setBindStatus(DeviceConstant.BIND_STATUS_YES);
                //??????????????????
                devicePo.setBindTime(System.currentTimeMillis());
                devicePo.setLastUpdateTime(System.currentTimeMillis());
                deviceTeamItemPo.setDeviceId(devicePo.getId());
                deviceTeamItemPo.setTeamId(deviceTeamPo.getId());
                deviceTeamItemPo.setManageName(device.getManageName());
                deviceTeamItemPo.setUserId(customerUserPo.getId());
                deviceTeamItemPo.setStatus(CommonConstant.STATUS_YES);
                deviceTeamItemPo.setLinkAgeStatus(device.getLinkAgeStatus());
                if (DeviceTeamConstants.DEVICE_TEAM_LINKAGE_YES == device.getLinkAgeStatus()) {
                    deviceLinkAge = true;
                }
                deviceTeamItemPo.setCreateTime(System.currentTimeMillis());
                deviceTeamItemPo.setLastUpdateTime(System.currentTimeMillis());
                deviceCustomerUserRelationPo.setDeviceId(devicePo.getId());
                deviceCustomerUserRelationPo.setCustomerId(customerPo.getId());
                deviceCustomerUserRelationPo.setOpenId(customerUserPo.getOpenId());
                deviceCustomerUserRelationPo.setStatus(CommonConstant.STATUS_YES);
                deviceCustomerUserRelationPo.setCreateTime(System.currentTimeMillis());
                deviceCustomerUserRelationPo.setLastUpdateTime(System.currentTimeMillis());
                devicePoList.add(devicePo);
                deviceTeamItemPoList.add(deviceTeamItemPo);
                deviceCustomerUserRelationPoList.add(deviceCustomerUserRelationPo);
            }
            //???????????????????????????????????????????????????????????????
            if (deviceLinkAge) {
                deviceTeamPo.setTeamType(DeviceTeamConstants.DEVICE_TEAM_TYPE_LINK);
            }
            //????????????????????????
            else {
                deviceTeamPo.setTeamType(DeviceTeamConstants.DEVICE_TEAM_TYPE_USER);
            }
            deviceTeamPo.setLastUpdateTime(System.currentTimeMillis());
            //???????????????????????????
            deviceTeamMapper.updateById(deviceTeamPo);
            //?????????????????????????????????
            this.deviceMapper.updateBatch(devicePoList);
            this.deviceCustomerUserRelationMapper.insertBatch(deviceCustomerUserRelationPoList);
            //???????????????????????????
            this.deviceTeamItemMapper.insertBatch(deviceTeamItemPoList);
            return new ApiResponse<>(RetCode.OK, "?????????????????????????????????", deviceTeamPo.getId());
        }
    }

    /**
     * ?????????????????????
     * sixiaojun
     * 2018-09-01
     *
     * @param teamListQueryRequest
     * @return
     */
    public ApiResponse<List<DeviceTeamVo>> queryTeamList(TeamListQueryRequest teamListQueryRequest) throws Exception {

        //????????????????????????????????????
        Integer customerId = customerService.obtainCustomerId(false);

        //?????????????????????
        Integer currentPage = teamListQueryRequest.getPage();
        //?????????????????????
        Integer limit = teamListQueryRequest.getLimit();
        //?????????
        Integer offset = (currentPage - 1) * limit;
        //?????????????????????????????????????????????deviceTeamPo??????
        DeviceTeamPo queryPo = new DeviceTeamPo();
        queryPo.setName(teamListQueryRequest.getName());
        queryPo.setTeamType(teamListQueryRequest.getTeamType());
        queryPo.setMasterUserId(teamListQueryRequest.getMasterUserId());
        queryPo.setCreateUserId(teamListQueryRequest.getCreateUserId());
        queryPo.setStatus(null == teamListQueryRequest.getStatus() ? CommonConstant.STATUS_YES : teamListQueryRequest.getStatus());

        queryPo.setCustomerId(teamListQueryRequest.getCustomerId() == null ? customerId : customerId);

        List<DeviceTeamPo> deviceTeamPoList = this.deviceTeamMapper.selectList(queryPo, limit, offset);
        List<DeviceTeamVo> deviceTeamVoList = deviceTeamPoList.stream().map(deviceTeamPo -> {
            CustomerUserPo customerUserPo;
            DeviceTeamVo deviceTeamVo = new DeviceTeamVo();
            deviceTeamVo.setId(deviceTeamPo.getId());
            deviceTeamVo.setName(deviceTeamPo.getName());
            deviceTeamVo.setIcon(deviceTeamPo.getIcon());
            //????????????????????????????????????
            //???????????????????????????????????????????????????????????????????????????????????????
            if (null != deviceTeamPo.getCreateUserId()) {
                customerUserPo = this.customerUserMapper.selectByUserId(deviceTeamPo.getCreateUserId());
                //???????????????????????????????????????????????????????????????
                if (null != customerUserPo) {
                    deviceTeamVo.setCreateUserNickName(customerUserPo.getNickname());
                    deviceTeamVo.setCreateUserOpenId(customerUserPo.getOpenId());
                    deviceTeamVo.setCreateUserId(customerUserPo.getId());
                }
                deviceTeamVo.setCreateTime(deviceTeamPo.getCreateTime());
            }
            //????????????????????????????????????
            System.out.println(deviceTeamPo.toString());
            customerUserPo = this.customerUserMapper.selectByUserId(deviceTeamPo.getMasterUserId());
            deviceTeamVo.setMasterOpenId(customerUserPo.getOpenId());
            deviceTeamVo.setMasterNickName(customerUserPo.getNickname());
            //?????????????????????????????????
            deviceTeamVo.setCustomerId(customerUserPo.getCustomerId());
            CustomerPo customerPo = this.customerMapper.selectById(customerUserPo.getCustomerId());
            deviceTeamVo.setCustomerName(customerPo.getName());
            deviceTeamVo.setCover(deviceTeamPo.getVideoCover());
            deviceTeamVo.setSceneDescription(deviceTeamPo.getSceneDescription());

            deviceTeamVo.setMasterUserId(deviceTeamPo.getMasterUserId());
            deviceTeamVo.setCreateUserId(deviceTeamPo.getCreateUserId());
            //?????????????????????1-?????????2-??????
            deviceTeamVo.setStatus(deviceTeamPo.getStatus());
            //?????????????????????????????????????????????
            deviceTeamVo.setTeamStatus(deviceTeamPo.getTeamStatus());
            //?????????????????????????????????????????????
            deviceTeamVo.setTeamType(deviceTeamPo.getTeamType());
            deviceTeamVo.setRemark(deviceTeamPo.getRemark());
            List<DeviceTeamItemPo> deviceTeamItemPos = deviceTeamItemMapper.selectByTeamId(deviceTeamPo.getId());
            if (deviceTeamItemPos != null && deviceTeamItemPos.size() > 0) {
                List<DeviceTeamVo.DeviceTeamItemVo> deviceTeamItemVos = deviceTeamItemPos.stream().map(deviceTeamItemPo -> {
                    DeviceTeamVo.DeviceTeamItemVo deviceTeamItemVo = new DeviceTeamVo.DeviceTeamItemVo();
                    deviceTeamItemVo.setId(deviceTeamItemPo.getId());
                    deviceTeamItemVo.setDeviceId(deviceTeamItemPo.getDeviceId());
                    //??????deviceId??????deviceName???mac
                    DevicePo devicePo = this.deviceMapper.selectById(deviceTeamItemPo.getDeviceId());
                    deviceTeamItemVo.setName(devicePo.getName());
                    deviceTeamItemVo.setMac(devicePo.getMac());
                    deviceTeamItemPo.setManageName(deviceTeamItemPo.getManageName());
                    deviceTeamItemVo.setLinkAgeStatus(deviceTeamItemPo.getLinkAgeStatus());
                    deviceTeamItemVo.setStatus(deviceTeamItemPo.getStatus());
                    deviceTeamItemVo.setTeamId(deviceTeamItemPo.getTeamId());
                    deviceTeamItemVo.setUserId(deviceTeamItemPo.getUserId());
                    return deviceTeamItemVo;
                }).collect(Collectors.toList());
                deviceTeamVo.setTeamDeviceCreateRequestList(deviceTeamItemVos);
                deviceTeamVo.setDeviceCount(deviceTeamItemVos.size());
            } else {
                deviceTeamVo.setTeamDeviceCreateRequestList(null);
                deviceTeamVo.setDeviceCount(0);
            }
            //????????????
            List<DeviceTeamScenePo> deviceTeamImgScenePoList = this.deviceTeamSceneMapper.selectImgVideoList(deviceTeamPo.getId(), DeviceTeamConstants.IMAGE_VIDEO_MARK_IMAGE);
            List<DeviceTeamVo.Images> imagesList = deviceTeamImgScenePoList.stream().map(eachPo -> {
                DeviceTeamVo.Images image = new DeviceTeamVo.Images();
                image.setImage(eachPo.getImgVideo());
                return image;
            }).collect(Collectors.toList());
            deviceTeamVo.setImagesList(imagesList);
            //????????????
            List<DeviceTeamScenePo> deviceTeamVideoScenePoList = this.deviceTeamSceneMapper.selectImgVideoList(deviceTeamPo.getId(), DeviceTeamConstants.IMAGE_VIDEO_MARK_VIDEO);
            List<DeviceTeamVo.Videos> videosList = deviceTeamVideoScenePoList.stream().map(eachPo -> {
                DeviceTeamVo.Videos video = new DeviceTeamVo.Videos();
                video.setVideo(eachPo.getImgVideo());
                return video;
            }).collect(Collectors.toList());
            deviceTeamVo.setVideosList(videosList);
            return deviceTeamVo;
        }).collect(Collectors.toList());
        if (null == deviceTeamVoList || 0 == deviceTeamVoList.size()) {
            return new ApiResponse<>(RetCode.OK, "????????????", null);
        }
        return new ApiResponse<>(RetCode.OK, "?????????????????????", deviceTeamVoList);
    }


    /**
     * ?????????????????????
     * caik
     * 2018-09-01
     *
     * @param teamId
     * @return
     */
    public ApiResponse<DeviceTeamVo> queryTeamById(Integer teamId) throws Exception {

        DeviceTeamPo deviceTeamPo = this.deviceTeamMapper.selectById(teamId);

        if (null != deviceTeamPo) {
            CustomerUserPo customerUserPo;
            DeviceTeamVo deviceTeamVo = new DeviceTeamVo();
            deviceTeamVo.setId(deviceTeamPo.getId());
            deviceTeamVo.setName(deviceTeamPo.getName());
            deviceTeamVo.setIcon(deviceTeamPo.getIcon());

            //????????????????????????????????????
            //???????????????????????????????????????????????????????????????????????????????????????
            if (null != deviceTeamPo.getCreateUserId()) {
                customerUserPo = this.customerUserMapper.selectByUserId(deviceTeamPo.getCreateUserId());
                deviceTeamVo.setCreateUserNickName(customerUserPo.getNickname());
                deviceTeamVo.setCreateUserOpenId(customerUserPo.getOpenId());
                deviceTeamVo.setCreateUserId(customerUserPo.getId());
                deviceTeamVo.setCreateTime(deviceTeamPo.getCreateTime());
            }

            //????????????????????????????????????
            customerUserPo = this.customerUserMapper.selectByUserId(deviceTeamPo.getMasterUserId());
            deviceTeamVo.setMasterOpenId(customerUserPo.getOpenId());
            deviceTeamVo.setMasterNickName(customerUserPo.getNickname());
            //?????????????????????????????????
            deviceTeamVo.setCustomerId(customerUserPo.getCustomerId());
            CustomerPo customerPo = this.customerMapper.selectById(customerUserPo.getCustomerId());
            deviceTeamVo.setCustomerName(customerPo.getName());
            deviceTeamVo.setCover(deviceTeamPo.getVideoCover());
            deviceTeamVo.setSceneDescription(deviceTeamPo.getSceneDescription());

            deviceTeamVo.setMasterUserId(deviceTeamPo.getMasterUserId());
            deviceTeamVo.setCreateUserId(deviceTeamPo.getCreateUserId());
            //?????????????????????1-?????????2-??????
            deviceTeamVo.setStatus(deviceTeamPo.getStatus());
            //?????????????????????????????????????????????
            deviceTeamVo.setTeamStatus(deviceTeamPo.getTeamStatus());
            //?????????????????????????????????????????????
            deviceTeamVo.setTeamType(deviceTeamPo.getTeamType());
            deviceTeamVo.setRemark(deviceTeamPo.getRemark());

            List<DeviceTeamScenePo> deviceImgTeamScenePoList = this.deviceTeamSceneMapper.selectImgVideoList(deviceTeamPo.getId(), DeviceTeamConstants.IMAGE_VIDEO_MARK_IMAGE);
            List<DeviceTeamVo.Images> imagesList = deviceImgTeamScenePoList.stream().map(eachPo -> {
                DeviceTeamVo.Images image = new DeviceTeamVo.Images();
                image.setImage(eachPo.getImgVideo());
                return image;
            }).collect(Collectors.toList());
            deviceTeamVo.setImagesList(imagesList);

            List<DeviceTeamScenePo> deviceVideoTeamScenePoList = this.deviceTeamSceneMapper.selectImgVideoList(deviceTeamPo.getId(), DeviceTeamConstants.IMAGE_VIDEO_MARK_VIDEO);
            List<DeviceTeamVo.Videos> videosList = deviceVideoTeamScenePoList.stream().map(eachPo -> {
                DeviceTeamVo.Videos video = new DeviceTeamVo.Videos();
                video.setVideo(eachPo.getImgVideo());
                return video;
            }).collect(Collectors.toList());
            deviceTeamVo.setVideosList(videosList);

            List<DeviceTeamItemPo> deviceTeamItemPos = deviceTeamItemMapper.selectByTeamId(deviceTeamPo.getId());
            if (deviceTeamItemPos != null && deviceTeamItemPos.size() > 0) {
                List<DeviceTeamVo.DeviceTeamItemVo> deviceTeamItemVos = deviceTeamItemPos.stream().map(deviceTeamItemPo -> {
                    DeviceTeamVo.DeviceTeamItemVo deviceTeamItemVo = new DeviceTeamVo.DeviceTeamItemVo();
                    deviceTeamItemVo.setId(deviceTeamItemPo.getId());
                    deviceTeamItemVo.setDeviceId(deviceTeamItemPo.getDeviceId());
                    //??????deviceId??????deviceName???mac
                    DevicePo devicePo = this.deviceMapper.selectById(deviceTeamItemPo.getDeviceId());
                    deviceTeamItemVo.setName(devicePo.getName());
                    deviceTeamItemVo.setMac(devicePo.getMac());
                    deviceTeamItemPo.setManageName(deviceTeamItemPo.getManageName());
                    deviceTeamItemVo.setLinkAgeStatus(deviceTeamItemPo.getLinkAgeStatus());
                    deviceTeamItemVo.setStatus(deviceTeamItemPo.getStatus());
                    deviceTeamItemVo.setTeamId(deviceTeamItemPo.getTeamId());
                    deviceTeamItemVo.setUserId(deviceTeamItemPo.getUserId());
                    return deviceTeamItemVo;
                }).collect(Collectors.toList());
                deviceTeamVo.setTeamDeviceCreateRequestList(deviceTeamItemVos);
                deviceTeamVo.setDeviceCount(deviceTeamItemVos.size());
            } else {
                deviceTeamVo.setTeamDeviceCreateRequestList(null);
                deviceTeamVo.setDeviceCount(0);
            }

            return new ApiResponse<>(RetCode.OK, "???????????????", deviceTeamVo);

        } else {
            return new ApiResponse<>(RetCode.OK, "???????????????", null);
        }
    }

    /**
     * ?????????????????????????????????????????????openId?????????
     *
     * @param teamTrusteeRequest
     * @return
     */
    @Transactional
    public CustomerUserPo trusteeTeam(TeamTrusteeRequest teamTrusteeRequest) {
        DeviceTeamPo deviceTeamPo = this.deviceTeamMapper.selectById(teamTrusteeRequest.getId());
        Integer oldUserId = deviceTeamPo.getMasterUserId();
        Integer teamId = deviceTeamPo.getId();
        //?????????????????????????????????????????????????????????
        List<DeviceTeamItemPo> deviceTeamItemPos = deviceTeamItemMapper.selectByTeamId(teamId);
        List<DeviceCustomerUserRelationPo> deviceCustomerUserRelationPos = deviceCustomerUserRelationMapper.selectByUserId(oldUserId);
        List<Integer> deviceIdList = deviceCustomerUserRelationPos.stream().map(e -> e.getDeviceId()).collect(Collectors.toList());
        for (DeviceTeamItemPo deviceTeamItemPo : deviceTeamItemPos) {
            if(!deviceIdList.contains(deviceTeamItemPo.getDeviceId())){
                throw new BusinessException("????????????????????????????????????????????????id=" + deviceTeamItemPo.getDeviceId());
            }
        }
        //??????masterUserId?????????????????????????????????
        CustomerUserPo oldUserPo = this.customerUserMapper.selectByUserId(oldUserId);
        CustomerUserPo newUserPo = this.customerUserMapper.selectByOpenId(teamTrusteeRequest.getOpenId());

        //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????,?????????????????????????????????????????????????????????????????????????????????
        List<DeviceCustomerUserRelationPo> oldRelationPos = this.deviceCustomerUserRelationMapper.selectByOpenId(oldUserPo.getOpenId());
        List<DeviceCustomerUserRelationPo> newUserRelationPos = this.deviceCustomerUserRelationMapper.selectByOpenId(newUserPo.getOpenId());
        Map<Integer, DeviceCustomerUserRelationPo> newUserRelationMap = newUserRelationPos.stream().collect(Collectors.toMap(DeviceCustomerUserRelationPo::getDeviceId, a -> a, (k1, k2) -> k1));
        List<DeviceCustomerUserRelationPo> updatePos = new ArrayList<>();
        if (0 != oldRelationPos.size()) {
            oldRelationPos.stream().forEach(oldRelationPo -> {
                if(newUserRelationMap.keySet().contains(oldRelationPo.getDeviceId())){
                    deviceCustomerUserRelationMapper.deleteById(oldRelationPo.getId());
                }
                else {
                    oldRelationPo.setOpenId(teamTrusteeRequest.getOpenId());
                    oldRelationPo.setLastUpdateTime(System.currentTimeMillis());
                    updatePos.add(oldRelationPo);
                }
            });
        }
        this.deviceCustomerUserRelationMapper.updateBatch(updatePos);

        //??????????????????????????????
        deviceTeamPo.setMasterUserId(newUserPo.getId());
        deviceTeamPo.setTeamStatus(DeviceTeamConstants.DEVICE_TEAM_STATUS_TRUSTEE);
        if (teamTrusteeRequest.getDeleteCreator()) {
            deviceTeamPo.setCreateUserId(null);
        }
        Boolean ret = this.deviceTeamMapper.updateById(deviceTeamPo) > 0;

        //??????????????????
        List<Integer> sourceDeviceIdList = deviceTeamItemPos.stream().map(e -> e.getDeviceId()).collect(Collectors.toList());
        List<Integer> updateItemIds = new ArrayList<>();
        List<DeviceTeamItemPo> ownDeviceList = deviceTeamItemMapper.selectByUserId(newUserPo.getId());
        Map<Integer,DeviceTeamItemPo> ownDeviceMap = ownDeviceList.stream().collect(Collectors.toMap(DeviceTeamItemPo::getDeviceId, a->a,(k1, k2)->k1));
        deviceTeamItemPos.stream().forEach(temp ->{
            if(ownDeviceMap.keySet().contains(temp.getDeviceId())){
                DeviceTeamItemPo deviceTeamItemPo = ownDeviceMap.get(temp.getDeviceId());
                deviceTeamItemPo.setTeamId(teamTrusteeRequest.getId());
                deviceTeamItemMapper.updateById(deviceTeamItemPo);
                deviceTeamItemMapper.deleteById(temp.getId());
            }else{
                updateItemIds.add(temp.getId());
            }
        });
        if(updateItemIds.size()>0){
            deviceTeamItemMapper.trusteeTeamItems(updateItemIds, newUserPo.getId());
        }
        if (ret) {
            //??????????????????????????????????????????
            return newUserPo;
        } else {
            return null;
        }
    }

    public ApiResponse<Boolean> deleteOneTeam(TeamDeleteRequest teamDeleteRequest) throws Exception {
        //?????????????????????????????????????????????????????????????????????????????????
        List<DeviceTeamItemPo> deviceTeamItemPoList = this.deviceTeamItemMapper.selectByTeamId(teamDeleteRequest.getTeamId());
        if (null != deviceTeamItemPoList && 0 < deviceTeamItemPoList.size()) {
            //???????????????????????????????????????
            List<DeviceCustomerUserRelationPo> deviceCustomerUserRelationPoList = new ArrayList<>();
            List<DevicePo> devicePoList = new ArrayList<>();
            //???????????????deviceId???????????????????????????????????????
            deviceTeamItemPoList.stream().forEach(deviceTeamItemPo -> {
                DeviceCustomerUserRelationPo deviceCustomerUserRelationPo = this.deviceCustomerUserRelationMapper.selectByDeviceId(deviceTeamItemPo.getDeviceId());
                DevicePo devicePo = this.deviceMapper.selectById(deviceTeamItemPo.getDeviceId());
                devicePo.setBindStatus(DeviceConstant.BIND_STATUS_NO);
                devicePo.setBindTime(null);
                devicePo.setLastUpdateTime(System.currentTimeMillis());
                deviceCustomerUserRelationPoList.add(deviceCustomerUserRelationPo);
                devicePoList.add(devicePo);
            });
            //?????????????????????????????????
            this.deviceTeamItemMapper.deleteByTeamId(teamDeleteRequest.getTeamId());
            //?????????????????????????????????????????????
            this.deviceCustomerUserRelationMapper.deleteBatch(deviceCustomerUserRelationPoList);
            //????????????????????????
            this.deviceMapper.updateBatch(devicePoList);
        }
//        List<DeviceTeamScenePo> deviceTeamScenePoList = this.deviceTeamSceneMapper.selectImgVideoList(teamDeleteRequest.getTeamId());
        //??????????????????
//        if (null != deviceTeamItemPoList && 0 < deviceTeamItemPoList.size()) {
//        }
        this.deviceTeamSceneMapper.deleteByTeamId(teamDeleteRequest.getTeamId());
        //??????????????????
        DeviceTeamPo deviceTeamPo = this.deviceTeamMapper.selectById(teamDeleteRequest.getTeamId());
        //??????????????????
        deviceTeamPo.setStatus(CommonConstant.STATUS_DEL);

        //???????????????????????????
        Boolean ret = this.deviceTeamMapper.updateById(deviceTeamPo) > 0;
        if (ret) {
            return new ApiResponse<>(RetCode.OK, "???????????????", ret);
        } else {
            return new ApiResponse<>(RetCode.ERROR, "???????????????", ret);
        }
    }


    /**
     * ?????????????????????
     *
     * @param teamId
     * @return
     * @throws Exception
     */
    public String createQrCode(@RequestBody Integer teamId) throws Exception {
        DeviceTeamPo deviceTeamPo = deviceTeamMapper.selectById(teamId);
        List<DeviceTeamItemPo> deviceTeamItemPos = deviceTeamItemMapper.selectByTeamId(teamId);
        List<DeviceCustomerUserRelationPo> deviceCustomerUserRelationPos = deviceCustomerUserRelationMapper.selectByUserId(deviceTeamPo.getMasterUserId());
        List<Integer> deviceIdList = deviceCustomerUserRelationPos.stream().map(e -> e.getDeviceId()).collect(Collectors.toList());
        for (DeviceTeamItemPo deviceTeamItemPo : deviceTeamItemPos) {
            if(!deviceIdList.contains(deviceTeamItemPo.getDeviceId())){
                throw new BusinessException("????????????????????????????????????????????????id=" + deviceTeamItemPo.getDeviceId());
            }
        }

        //?????????????????????teamId???????????????customer???appId???????????????
        CustomerPo customerPo = this.customerMapper.selectByTeamId(teamId);
        String appId = customerPo.getAppid();
        String redirect_uri = "http://" + customerPo.getSLD() + ".hcocloud.com/h5/auth?teamId="+ teamId+"&customerId="+customerPo.getId();
        String code = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+appId+"&redirect_uri="
                + URLEncoder.encode(redirect_uri, "UTF-8")+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
        return code;
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param deviceList
     * @param openId
     * @return
     */
    public DevicePo queryDeviceStatus(List<TeamCreateOrUpdateRequest.TeamDeviceCreateRequest> deviceList, String openId) {
        for (TeamCreateOrUpdateRequest.TeamDeviceCreateRequest queryDevice : deviceList) {
            DeviceCustomerRelationPo deviceCustomerRelationPo = this.deviceCustomerRelationMapper.selectByDeviceMac(queryDevice.getMac());
            CustomerUserPo customerUserPo = this.customerUserMapper.selectByOpenId(openId);
            //????????????????????????customer???user????????????customer??????????????????????????????
            if (deviceCustomerRelationPo.getCustomerId() != customerUserPo.getCustomerId()) {
                return this.deviceMapper.selectByMac(queryDevice.getMac());
            }
        }
        return null;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    public Integer selectTeamCount(Integer status) {
        DeviceTeamPo deviceTeamPo = new DeviceTeamPo();
        //????????????????????????????????????
        Integer customerId = customerService.obtainCustomerId(false);
        deviceTeamPo.setStatus(status);
        deviceTeamPo.setCustomerId(customerId);
        return this.deviceTeamMapper.selectCount(deviceTeamPo);
    }

    /**
     * 2018-08-20
     * sixiaojun
     * ??????????????????????????????mac????????????????????????????????????
     *
     * @param deviceList
     * @return
     */
    public TeamCreateOrUpdateRequest.TeamDeviceCreateRequest queryDeviceCustomer(TeamCreateOrUpdateRequest deviceList) {
        Integer customerId = this.customerService.obtainCustomerId(false);
        //??????????????????
        if (null != customerId) {
            log.info("????????????id?????????");
            for (TeamCreateOrUpdateRequest.TeamDeviceCreateRequest device : deviceList.getTeamDeviceCreateRequestList()) {
                DeviceCustomerRelationPo deviceCustomerRelationPo = this.deviceCustomerRelationMapper.selectByDeviceMac(device.getMac());
                //????????????????????????????????????????????????????????????
                if (null == deviceCustomerRelationPo || (null != deviceCustomerRelationPo && customerId != deviceCustomerRelationPo.getCustomerId())) {
                    return device;
                }
            }
        } else {
            //??????
            log.info("????????????id??????,????????????????????????");
            for (TeamCreateOrUpdateRequest.TeamDeviceCreateRequest device : deviceList.getTeamDeviceCreateRequestList()) {
                DeviceCustomerRelationPo deviceCustomerRelationPo = this.deviceCustomerRelationMapper.selectByDeviceMac(device.getMac());
                //????????????????????????????????????????????????????????????
                if (null == deviceCustomerRelationPo) {
                    return device;
                }
            }
        }
        return null;
    }


    /**
     * 2018-08-20
     * sixiaojun
     * ??????mac??????????????????????????????????????????mac????????????????????????????????????DevicePo???????????????
     *
     * @param deviceList
     * @return devicePo
     */
    public TeamCreateOrUpdateRequest.TeamDeviceCreateRequest queryDeviceByMac(TeamCreateOrUpdateRequest deviceList) {
        for (TeamCreateOrUpdateRequest.TeamDeviceCreateRequest device : deviceList.getTeamDeviceCreateRequestList()) {
            DevicePo devicePo = deviceMapper.selectByMac(device.getMac());
            if (null == devicePo) {
                return device;
            }
        }
        return null;
    }

    /**
     * ????????????????????? ??????????????????????????????
     *
     * @param teamDeviceCreateRequest
     * @return
     */
    public TeamCreateOrUpdateRequest.TeamDeviceCreateRequest isDeviceHasTeam(TeamCreateOrUpdateRequest teamDeviceCreateRequest) {
        //?????????????????????????????????openId???????????????????????????
        if (null != teamDeviceCreateRequest.getId() && 0 < teamDeviceCreateRequest.getId()) {
            for (TeamCreateOrUpdateRequest.TeamDeviceCreateRequest device : teamDeviceCreateRequest.getTeamDeviceCreateRequestList()) {
                DevicePo devicePo = this.deviceMapper.selectByMac(device.getMac());
                DeviceTeamItemPo queryDeviceTeamItemPo = this.deviceTeamItemMapper.selectByJoinOpenId(devicePo.getId(), teamDeviceCreateRequest.getCreateUserOpenId());
                if (null != queryDeviceTeamItemPo) {
                    continue;
                } else {
                    queryDeviceTeamItemPo = this.deviceTeamItemMapper.selectByDeviceMac(device.getMac());
                    if (null != queryDeviceTeamItemPo) {
                        return device;
                    }
                }
            }
        } else {
            for (TeamCreateOrUpdateRequest.TeamDeviceCreateRequest device : teamDeviceCreateRequest.getTeamDeviceCreateRequestList()) {
                //???????????????????????????????????????????????????
                DeviceTeamItemPo queryDeviceTeamItemPo = this.deviceTeamItemMapper.selectByDeviceMac(device.getMac());
                if (null != queryDeviceTeamItemPo) {
                    return device;
                }
            }
        }
        return null;
    }

    /**
     * ?????????????????????
     *
     * @param name
     * @return
     */
    public DeviceTeamPo queryTeamByName(String name) {
        DeviceTeamPo deviceTeamPo = new DeviceTeamPo();
        deviceTeamPo.setName(name);
        DeviceTeamPo queryPo = this.deviceTeamMapper.queryByName(deviceTeamPo);
        if (null != queryPo) {
            return queryPo;
        } else {
            return null;
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param queryInfoByCustomerRequest
     * @return
     */
    public List<DevicePo> queryDevicesByCustomer(QueryInfoByCustomerRequest queryInfoByCustomerRequest) {
        List<DeviceCustomerRelationPo> deviceCustomerRelationPoList = this.deviceCustomerRelationMapper.selectByCustomerId(queryInfoByCustomerRequest.getCustomerId());
        List<DevicePo> devicePoList = new ArrayList<>();
        if (null != deviceCustomerRelationPoList) {
            deviceCustomerRelationPoList.stream().forEach(deviceCustomerRelation -> {
                //??????????????????????????????????????????
                if (null == this.deviceCustomerUserRelationMapper.selectByDeviceId(deviceCustomerRelation.getDeviceId())) {
                    DevicePo devicePo = this.deviceMapper.selectById(deviceCustomerRelation.getDeviceId());
                    devicePoList.add(devicePo);
                }
            });
            return devicePoList;
        } else {
            return null;
        }
    }


    /**
     * ??????????????????????????????????????????
     *
     * @param openId
     * @return
     */
    public CustomerUserPo queryCustomerUser(String openId) {
        CustomerUserPo customerUserPo = this.customerUserMapper.selectByOpenId(openId);
        return customerUserPo;
    }
}
