package com.huanke.iot.manage.service.device.typeModel;

import com.huanke.iot.base.api.ApiResponse;
import com.huanke.iot.base.constant.CommonConstant;
import com.huanke.iot.base.constant.DeviceConstant;
import com.huanke.iot.base.constant.RetCode;
import com.huanke.iot.base.dao.customer.CustomerMapper;
import com.huanke.iot.base.dao.device.DeviceGroupItemMapper;
import com.huanke.iot.base.dao.device.DeviceIdPoolMapper;
import com.huanke.iot.base.dao.device.DeviceMapper;
import com.huanke.iot.base.dao.device.ability.DeviceAbilityOptionMapper;
import com.huanke.iot.base.dao.device.ability.DeviceTypeAbilitysMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelAbilityMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelAbilityOptionMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceModelMapper;
import com.huanke.iot.base.dao.format.DeviceModelFormatItemMapper;
import com.huanke.iot.base.dao.format.DeviceModelFormatMapper;
import com.huanke.iot.base.dao.format.WxFormatItemMapper;
import com.huanke.iot.base.dao.format.WxFormatPageMapper;
import com.huanke.iot.base.dao.project.ProjectMapper;
import com.huanke.iot.base.dao.user.UserManagerMapper;
import com.huanke.iot.base.exception.BusinessException;
import com.huanke.iot.base.po.customer.CustomerPo;
import com.huanke.iot.base.po.device.DeviceIdPoolPo;
import com.huanke.iot.base.po.device.DevicePo;
import com.huanke.iot.base.po.device.ability.DeviceAbilityOptionPo;
import com.huanke.iot.base.po.device.ability.DeviceTypeAbilitysPo;
import com.huanke.iot.base.po.device.typeModel.DeviceModelAbilityOptionPo;
import com.huanke.iot.base.po.device.typeModel.DeviceModelAbilityPo;
import com.huanke.iot.base.po.device.typeModel.DeviceModelPo;
import com.huanke.iot.base.po.format.DeviceModelFormatItemPo;
import com.huanke.iot.base.po.format.DeviceModelFormatPo;
import com.huanke.iot.base.po.format.WxFormatItemPo;
import com.huanke.iot.base.po.format.WxFormatPagePo;
import com.huanke.iot.base.po.project.ProjectBaseInfo;
import com.huanke.iot.base.po.user.User;
import com.huanke.iot.base.resp.device.ModelProjectRsp;
import com.huanke.iot.base.resp.project.ProjectModelPercentVo;
import com.huanke.iot.base.util.UniNoCreateUtils;
import com.huanke.iot.manage.service.customer.CustomerService;
import com.huanke.iot.manage.service.device.operate.DeviceOperateService;
import com.huanke.iot.manage.service.user.UserService;
import com.huanke.iot.manage.vo.request.device.operate.DevicePoolRequest;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceModelCreateOrUpdateRequest;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceModelFormatCreateRequest;
import com.huanke.iot.manage.vo.request.device.typeModel.DeviceModelQueryRequest;
import com.huanke.iot.manage.vo.response.device.typeModel.DeviceModelAbilityVo;
import com.huanke.iot.manage.vo.response.device.typeModel.DeviceModelVo;
import com.huanke.iot.manage.vo.response.format.ModelFormatVo;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DeviceModelService {

    @Autowired
    private DeviceOperateService deviceOperateService;


    @Autowired
    private DeviceModelAbilityMapper deviceModelAbilityMapper;

    @Autowired
    private DeviceModelAbilityOptionMapper deviceModelAbilityOptionMapper;

    @Autowired
    private DeviceTypeAbilitysMapper deviceTypeAbilitysMapper;

    @Autowired
    private DeviceAbilityOptionMapper deviceAbilityOptionMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private DeviceModelFormatMapper deviceModelFormatMapper;

    @Autowired
    private DeviceModelFormatItemMapper deviceModelFormatItemMapper;

    @Autowired
    private DeviceIdPoolMapper deviceIdPoolMapper;

    @Autowired
    private WxFormatItemMapper wxFormatItemMapper;

    @Autowired
    private WxFormatPageMapper wxFormatPageMapper;

    @Autowired
    private DeviceModelMapper deviceModelMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceGroupItemMapper deviceGroupItemMapper;

    @Autowired
    private ProjectMapper projectMapper;

    private DefaultEventExecutorGroup defaultEventExecutorGroup = new DefaultEventExecutorGroup(16,new DefaultThreadFactory("QueryAblity"));

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
     * ?????? ???????????? ??????
     *
     * @param modelRequest
     * @return
     */
    public ApiResponse<Integer> createOrUpdate(DeviceModelCreateOrUpdateRequest modelRequest) {

        //????????? ??????
        int effectCount = 0;
        Boolean ret = true;
        DeviceModelPo deviceModelPo = new DeviceModelPo();
        User user = userService.getCurrentUser();
        try {
            if (modelRequest != null) {
                BeanUtils.copyProperties(modelRequest, deviceModelPo);
                deviceModelPo.setIconList(String.join(",", modelRequest.getIcons()));
            }
            //?????? productId ????????????
            if (StringUtils.isNotBlank(modelRequest.getProductId())) {
//                DeviceModelPo queryDeviceModelPo = new DeviceModelPo();
//                queryDeviceModelPo = deviceModelMapper.selectByProductId(modelRequest.getProductId());

                if (modelRequest.getId() != null && modelRequest.getId() > 0) {
                    //?????? ?????????productid ????????? ??? ?????? ??? ??????????????? ?????????????????????
//                    if (null != queryDeviceModelPo && !modelRequest.getId().equals(queryDeviceModelPo.getId())) {
//                        return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????id???");
//                    }
                    //??? ?????? ????????????????????????????????????????????? ??????????????????????????????
                    if (modelRequest.getTypeId() != null && modelRequest.getTypeId() > 0) {
                        DevicePo updatePo = new DevicePo();
                        updatePo.setModelId(modelRequest.getId());
                        updatePo.setTypeId(modelRequest.getTypeId());
                        deviceMapper.updateDeviceTypeId(updatePo);
                    } else {
                        return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????????????????");
                    }
                    //????????????
                    if(modelRequest.getHelpFileUrlList() != null && modelRequest.getHelpFileUrlList().size() > 0){
                        deviceModelPo.setHelpFileUrl(String.join(",", modelRequest.getHelpFileUrlList()));
                    }
                    deviceModelPo.setLastUpdateUser(user.getId());
                    deviceModelPo.setLastUpdateTime(System.currentTimeMillis());
                    //????????????????????????????????? ????????????
                    deviceModelPo.setStatus(modelRequest.getStatus() == null ? CommonConstant.STATUS_YES : modelRequest.getStatus());
                    deviceModelMapper.updateById(deviceModelPo);
                } else {
                    //??????????????? ???????????????productid ???????????????
//                    if (null != queryDeviceModelPo) {
//                        return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????id???");
//                    }

                    if (deviceModelPo.getTypeId() == null) {
                        return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????????????????");
                    }
                    deviceModelPo.setModelNo(UniNoCreateUtils.createNo(DeviceConstant.DEVICE_UNI_NO_MODEl));
                    deviceModelPo.setCreateTime(System.currentTimeMillis());
                    deviceModelPo.setCreateUser(user.getId());
                    deviceModelPo.setStatus(modelRequest.getStatus() == null ? CommonConstant.STATUS_YES : modelRequest.getStatus());
                    //????????????
                    if(modelRequest.getHelpFileUrlList() != null && modelRequest.getHelpFileUrlList().size() > 0){
                        deviceModelPo.setHelpFileUrl(String.join(",", modelRequest.getHelpFileUrlList()));
                    }
                    deviceModelMapper.insert(deviceModelPo);

                    //??????wxdeviceid?????? //todo ???????????? 100
                    ApiResponse<Integer> addPoolResult = deviceOperateService.createWxDeviceIdPools(deviceModelPo.getCustomerId(), deviceModelPo.getProductId(), DeviceConstant.WXDEVICEID_DEF_COUNT);
                    if (RetCode.ERROR == addPoolResult.getCode()) {
                        log.error("??????????????????={}", "???????????????" + addPoolResult.getMsg(), "???????????????" + addPoolResult.getCode(), " ???????????????" + addPoolResult.getData());
                    }
                }

                //????????????????????????????????????
                List<DeviceModelCreateOrUpdateRequest.DeviceModelAbilityRequest> deviceModelAbilityRequests = modelRequest.getDeviceModelAbilitys();
                if (deviceModelAbilityRequests != null && deviceModelAbilityRequests.size() > 0) {

                    this.createOrUpdateModelAbilitys(deviceModelAbilityRequests, deviceModelPo.getId());
                }
                //??????h5???????????????????????????
                Integer listShowModelAbilityId = modelRequest.getListShowModelAbilityId();
                //??????????????????
                deviceModelAbilityMapper.updatelistShowAbility(deviceModelPo.getId(), listShowModelAbilityId);
                //????????????????????????
                deviceModelAbilityMapper.updatelistUnShowAbilitys(deviceModelPo.getId(), listShowModelAbilityId);

                //?????? ????????????????????????
                DeviceModelFormatCreateRequest modelFormat = modelRequest.getDeviceModelFormat();

                if (modelFormat != null && modelFormat.getModelFormatPages() != null && modelFormat.getModelFormatPages().size() > 0) {
                    this.createOrUpdateModelFormat(modelFormat, deviceModelPo.getId(), deviceModelPo.getFormatId());
                }

                return new ApiResponse<>(deviceModelPo.getId());
            } else {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "??????id???????????????");
            }


        } catch (Exception e) {
            log.error("?????????????????????", e);
            ret = false;
//            return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????");
            throw new RuntimeException("??????????????????");
        }
    }


    /**
     * ?????? ????????? deviceId ??????
     *
     * @param devicePoolRequest
     * @param devicePoolRequest
     * @param devicePoolRequest
     * @return
     */
    public ApiResponse<Integer> createWxDeviceIdPools(DevicePoolRequest devicePoolRequest) {
        Boolean ret = true;
        try {
            Integer addCount = devicePoolRequest.getAddCount();
            Integer customerId = devicePoolRequest.getCustomerId();
            String productId = devicePoolRequest.getProductId();

            if (null == devicePoolRequest) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????");
            }
            if (null == addCount || addCount <= 0) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "????????????????????????0");
            }
            if (addCount > DeviceConstant.WXDEVICEID_MAX_COUNT) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "????????????????????????");
            }
            //????????????????????????
            CustomerPo queryCustomerPo = customerMapper.selectById(customerId);
            if (null == queryCustomerPo) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????");
            } else if (null == queryCustomerPo.getAppid() || null == queryCustomerPo.getAppsecret()) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "??????????????????????????????");
            }

            DeviceModelPo queryDeviceModel = deviceModelMapper.selectByProductId(productId);
            if (null == queryDeviceModel) {
                return new ApiResponse<>(RetCode.PARAM_ERROR, "?????????????????????");
            }
            return deviceOperateService.createWxDeviceIdPools(customerId, productId, addCount);

        } catch (Exception e) {
            ret = false;
            log.error("createWxDeviceIdPools.error = {}", e);
            throw new RuntimeException("?????? ????????? deviceId ??????");
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param deviceModelAbilityRequests
     * @param modelId
     * @return
     */
    public Boolean createOrUpdateModelAbilitys(List<DeviceModelCreateOrUpdateRequest.DeviceModelAbilityRequest> deviceModelAbilityRequests, Integer modelId) {
        Boolean ret = true;
        try {
            //???????????? ???????????????
            deviceModelAbilityRequests.stream().forEach(deviceModelAbilityRequest -> {

                DeviceModelAbilityPo deviceModelAbilityPo = new DeviceModelAbilityPo();
                deviceModelAbilityPo.setModelId(modelId);
                deviceModelAbilityPo.setDefinedName(deviceModelAbilityRequest.getDefinedName());
                deviceModelAbilityPo.setAbilityId(deviceModelAbilityRequest.getAbilityId());
                deviceModelAbilityPo.setMinVal(deviceModelAbilityRequest.getMinVal());
                deviceModelAbilityPo.setMaxVal(deviceModelAbilityRequest.getMaxVal());

                //??????????????????????????? ???????????????????????????????????????????????? ??????
                //todo ???????????????
                if (deviceModelAbilityRequest.getId() != null && deviceModelAbilityRequest.getId() > 0) {
                    deviceModelAbilityPo.setId(deviceModelAbilityRequest.getId());
                    deviceModelAbilityPo.setLastUpdateTime(System.currentTimeMillis());
                    deviceModelAbilityPo.setStatus(deviceModelAbilityRequest.getStatus() == null ? CommonConstant.STATUS_YES : deviceModelAbilityRequest.getStatus());
                    deviceModelAbilityMapper.updateById(deviceModelAbilityPo);
                } else {
                    deviceModelAbilityPo.setStatus(deviceModelAbilityRequest.getStatus() == null ? CommonConstant.STATUS_YES : deviceModelAbilityRequest.getStatus());
                    deviceModelAbilityPo.setCreateTime(System.currentTimeMillis());
                    deviceModelAbilityMapper.insert(deviceModelAbilityPo);
                }
                //?????? ?????? ????????????????????????????????????
                List<DeviceModelCreateOrUpdateRequest.DeviceModelAbilityOptionRequest> deviceModelAbilityOptionRequests = deviceModelAbilityRequest.getDeviceModelAbilityOptions();
                if (deviceModelAbilityOptionRequests != null && deviceModelAbilityOptionRequests.size() > 0) {
                    deviceModelAbilityOptionRequests.stream().forEach(deviceModelAbilityOptionRequest -> {

                        DeviceModelAbilityOptionPo deviceModelAbilityOptionPo = new DeviceModelAbilityOptionPo();
                        deviceModelAbilityOptionPo.setModelAbilityId(deviceModelAbilityPo.getId());
                        deviceModelAbilityOptionPo.setAbilityOptionId(deviceModelAbilityOptionRequest.getAbilityOptionId());
                        deviceModelAbilityOptionPo.setActualOptionValue(deviceModelAbilityOptionRequest.getActualOptionValue());
                        deviceModelAbilityOptionPo.setDefinedName(deviceModelAbilityOptionRequest.getDefinedName());
                        deviceModelAbilityOptionPo.setDefaultValue(deviceModelAbilityOptionRequest.getDefaultVal());
                        deviceModelAbilityOptionPo.setMinVal(deviceModelAbilityOptionRequest.getMinVal());
                        deviceModelAbilityOptionPo.setMaxVal(deviceModelAbilityOptionRequest.getMaxVal());

                        // ?????? ???id ??? ????????????????????????
                        if (deviceModelAbilityOptionRequest.getId() != null && deviceModelAbilityOptionRequest.getId() > 0) {
                            deviceModelAbilityOptionPo.setStatus(deviceModelAbilityOptionRequest.getStatus() == null ? CommonConstant.STATUS_YES : deviceModelAbilityOptionRequest.getStatus());
                            deviceModelAbilityOptionPo.setId(deviceModelAbilityOptionRequest.getId());
                            deviceModelAbilityOptionPo.setLastUpdateTime(System.currentTimeMillis());
                            deviceModelAbilityOptionMapper.updateById(deviceModelAbilityOptionPo);
                        } else {
                            deviceModelAbilityOptionPo.setStatus(deviceModelAbilityOptionRequest.getStatus() == null ? CommonConstant.STATUS_YES : deviceModelAbilityOptionRequest.getStatus());
                            deviceModelAbilityOptionPo.setCreateTime(System.currentTimeMillis());
                            deviceModelAbilityOptionMapper.insert(deviceModelAbilityOptionPo);
                        }

                    });
                }
            });

        } catch (Exception e) {
            ret = false;
            log.error("????????????-???????????????-??????", e);
            throw new RuntimeException("????????????-???????????????-??????");

        }

        return ret;

    }

    /**
     * ?????? ???????????????
     *
     * @param request
     * @return
     */
    public List<DeviceModelVo> selectList(DeviceModelQueryRequest request) {

        //????????????????????????????????????
        Integer customerId = customerService.obtainCustomerId(false);
        DeviceModelPo queryDeviceModelPo = new DeviceModelPo();
        queryDeviceModelPo.setName(request.getName());
        //??????????????????????????? ???????????????????????????????????????????????????
        queryDeviceModelPo.setCustomerId(request.getCustomerId() == null ? customerId : request.getCustomerId());
        queryDeviceModelPo.setProductId(request.getProductId());
        queryDeviceModelPo.setTypeId(request.getTypeId());
        queryDeviceModelPo.setStatus(request.getStatus());

        Integer offset = (request.getPage() - 1) * request.getLimit();
        Integer limit = request.getLimit();

        //?????? ????????????
        List<DeviceModelPo> deviceModelPos = deviceModelMapper.selectList(queryDeviceModelPo, limit, offset);
        return deviceModelPos.stream().map(deviceModelPo -> {
            DeviceModelVo deviceModelVo = new DeviceModelVo();
            deviceModelVo.setName(deviceModelPo.getName());
            deviceModelVo.setCustomerId(deviceModelPo.getCustomerId());
            deviceModelVo.setCustomerName(deviceModelPo.getCustomerName());
            deviceModelVo.setProductId(deviceModelPo.getProductId());
            deviceModelVo.setProductQrCode(deviceModelPo.getProductQrCode());
            deviceModelVo.setFormatId(deviceModelPo.getFormatId());
            deviceModelVo.setAndroidFormatId(deviceModelPo.getAndroidFormatId());
            deviceModelVo.setTypeId(deviceModelPo.getTypeId());
            deviceModelVo.setTypeName(deviceModelPo.getTypeName());
            deviceModelVo.setRemark(deviceModelPo.getRemark());
            deviceModelVo.setStatus(deviceModelPo.getStatus());
            deviceModelVo.setVersion(deviceModelPo.getVersion());
            deviceModelVo.setChildModelIds(deviceModelPo.getChildModelIds());
            deviceModelVo.setIcons(deviceModelPo.getIconList().split(","));
            deviceModelVo.setModelNo(deviceModelPo.getModelNo());
            deviceModelVo.setModelCode(deviceModelPo.getModelCode());
            deviceModelVo.setId(deviceModelPo.getId());

//            List<DeviceModelAbilityVo> deviceModelAbilityVos = selectModelAbilitysByModelId(deviceModelPo.getId(),deviceModelPo.getTypeId());
//
//            deviceModelVo.setDeviceModelAbilitys(deviceModelAbilityVos);

//            //???????????????
//            ModelFormatVo modelFormatVo = selectModelFormatPages(deviceModelPo.getId(), deviceModelPo.getFormatId());
//            deviceModelVo.setModelFormatVo(modelFormatVo);

            //????????????device_pool?????? ?????????????????? wxDeviceId??????
            DeviceIdPoolPo deviceIdPoolPo = new DeviceIdPoolPo();
            deviceIdPoolPo.setCustomerId(deviceModelPo.getCustomerId());
            deviceIdPoolPo.setProductId(deviceModelPo.getProductId());
            deviceIdPoolPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);

            Integer devicePoolCount = deviceIdPoolMapper.selectCount(deviceIdPoolPo);
            deviceModelVo.setDevicePoolCount(devicePoolCount);
            return deviceModelVo;
        }).collect(Collectors.toList());
    }

    public ApiResponse<Integer> selectCount(Integer status) throws Exception {
        DeviceModelPo deviceModelPo = new DeviceModelPo();
        deviceModelPo.setStatus(status);
        return new ApiResponse<>(RetCode.OK, "??????????????????", this.deviceModelMapper.selectCount(deviceModelPo));
    }

    /**
     * ????????????ID ??????????????????
     *
     * @param typeId
     * @return
     */
    public List<DeviceModelVo> selectByTypeId(Integer typeId) {

        DeviceModelPo queryDeviceModelPo = new DeviceModelPo();

        List<DeviceModelPo> deviceModelPos = deviceModelMapper.selectByTypeId(typeId);
        return deviceModelPos.stream().map(deviceModelPo -> {
            DeviceModelVo deviceModelVo = new DeviceModelVo();
            deviceModelVo.setName(deviceModelPo.getName());
            deviceModelVo.setCustomerId(deviceModelPo.getCustomerId());
            deviceModelVo.setCustomerName(deviceModelPo.getCustomerName());
            deviceModelVo.setProductId(deviceModelPo.getProductId());
            deviceModelVo.setProductQrCode(deviceModelPo.getProductQrCode());
            deviceModelVo.setFormatId(deviceModelPo.getFormatId());
            deviceModelVo.setAndroidFormatId(deviceModelPo.getAndroidFormatId());
            deviceModelVo.setTypeId(deviceModelPo.getTypeId());
            deviceModelVo.setTypeName(deviceModelPo.getTypeName());
            deviceModelVo.setRemark(deviceModelPo.getRemark());
            deviceModelVo.setStatus(deviceModelPo.getStatus());
            deviceModelVo.setVersion(deviceModelPo.getVersion());
            deviceModelVo.setChildModelIds(deviceModelPo.getChildModelIds());
            deviceModelVo.setIcons(deviceModelPo.getIconList().split(","));
            deviceModelVo.setModelNo(deviceModelPo.getModelNo());
            deviceModelVo.setId(deviceModelPo.getId());

            List<DeviceModelAbilityVo> deviceModelAbilityVos = selectModelAbilitysByModelId(deviceModelPo.getId(), deviceModelPo.getTypeId());
            deviceModelVo.setDeviceModelAbilitys(deviceModelAbilityVos);
            return deviceModelVo;
        }).collect(Collectors.toList());

    }

    /**
     * ????????????ID ??????????????????
     *
     * @param typeIds
     * @return
     */
    public List<DeviceModelVo> selectModelsByTypeIds(String typeIds) {

        List<String> typeIdList = Arrays.asList(typeIds.split(","));

        List<DeviceModelPo> deviceModelPos = deviceModelMapper.selectModelsByTypeIds(typeIdList);
        return deviceModelPos.stream().map(deviceModelPo -> {

            DeviceModelVo deviceModelVo = new DeviceModelVo();
            deviceModelVo.setName(deviceModelPo.getName());
            deviceModelVo.setCustomerId(deviceModelPo.getCustomerId());
            deviceModelVo.setCustomerName(deviceModelPo.getCustomerName());
            deviceModelVo.setProductId(deviceModelPo.getProductId());
            deviceModelVo.setProductQrCode(deviceModelPo.getProductQrCode());
            deviceModelVo.setFormatId(deviceModelPo.getFormatId());
            deviceModelVo.setAndroidFormatId(deviceModelPo.getAndroidFormatId());
            deviceModelVo.setTypeId(deviceModelPo.getTypeId());
            deviceModelVo.setTypeName(deviceModelPo.getTypeName());
            deviceModelVo.setRemark(deviceModelPo.getRemark());
            deviceModelVo.setStatus(deviceModelPo.getStatus());
            deviceModelVo.setVersion(deviceModelPo.getVersion());
            deviceModelVo.setChildModelIds(deviceModelPo.getChildModelIds());
            if(deviceModelPo.getIconList()!=null){
                deviceModelVo.setIcons(deviceModelPo.getIconList().split(","));
            }
            deviceModelVo.setModelNo(deviceModelPo.getModelNo());
            deviceModelVo.setId(deviceModelPo.getId());
            Future<  List<DeviceModelAbilityVo>> future = defaultEventExecutorGroup.submit(()->{
              return   selectModelAbilitysByModelId(deviceModelPo.getId(), deviceModelPo.getTypeId());
            });
            try {
                deviceModelVo.setDeviceModelAbilitys(future.get());
            }catch (Exception e){

            }


            return deviceModelVo;
        }).collect(Collectors.toList());

    }

    /**
     * ???????????????????????? ??????
     *
     * @param id
     * @return
     */
    public DeviceModelVo selectById(Integer id) {


        DeviceModelPo deviceModelPo = deviceModelMapper.selectById(id);

        DeviceModelVo deviceModelVo = new DeviceModelVo();
        if (null != deviceModelPo) {

            deviceModelVo.setName(deviceModelPo.getName());
            deviceModelVo.setCustomerId(deviceModelPo.getCustomerId());
            deviceModelVo.setCustomerName(deviceModelPo.getCustomerName());
            deviceModelVo.setProductId(deviceModelPo.getProductId());
            deviceModelVo.setProductQrCode(deviceModelPo.getProductQrCode());
            deviceModelVo.setFormatId(deviceModelPo.getFormatId());
            deviceModelVo.setAndroidFormatId(deviceModelPo.getAndroidFormatId());
            deviceModelVo.setTypeId(deviceModelPo.getTypeId());
            deviceModelVo.setTypeName(deviceModelPo.getTypeName());
            deviceModelVo.setRemark(deviceModelPo.getRemark());
            deviceModelVo.setStatus(deviceModelPo.getStatus());
            deviceModelVo.setVersion(deviceModelPo.getVersion());
            deviceModelVo.setChildModelIds(deviceModelPo.getChildModelIds());
            deviceModelVo.setIcons(deviceModelPo.getIconList().split(","));
            deviceModelVo.setModelNo(deviceModelPo.getModelNo());
            deviceModelVo.setId(deviceModelPo.getId());
            deviceModelVo.setModelCode(deviceModelPo.getModelCode());
            if(deviceModelPo.getHelpFileUrl() != null){
                deviceModelVo.setHelpFileUrlList(Arrays.asList(deviceModelPo.getHelpFileUrl().split(",")));
            }

            deviceModelVo.setCreateUserName(userService.getUserName(deviceModelPo.getCreateUser()));
            deviceModelVo.setLastUpdateUserName(userService.getUserName(deviceModelPo.getLastUpdateUser()));


            List<DeviceModelAbilityVo> deviceModelAbilityVos = selectModelAbilitysByModelId(deviceModelPo.getId(), deviceModelPo.getTypeId());
            DeviceModelAbilityPo deviceModelAbilityPo = deviceModelAbilityMapper.selectListShowAbilityByModelId(deviceModelPo.getId());
            if(deviceModelAbilityPo != null){
                deviceModelVo.setListShowModelAbilityId(deviceModelAbilityPo.getAbilityId());
            }
            //??????????????????
            deviceModelVo.setDeviceModelAbilitys(deviceModelAbilityVos);

            //???????????????
            ModelFormatVo modelFormatVo = selectModelFormatPages(deviceModelPo.getId(), deviceModelPo.getFormatId());
            deviceModelVo.setDeviceModelFormat(modelFormatVo);

            //????????????device_pool?????? ?????????????????? wxDeviceId??????
            DeviceIdPoolPo deviceIdPoolPo = new DeviceIdPoolPo();
            deviceIdPoolPo.setCustomerId(deviceModelPo.getCustomerId());
            deviceIdPoolPo.setProductId(deviceModelPo.getProductId());
            deviceIdPoolPo.setStatus(DeviceConstant.WXDEVICEID_STATUS_NO);

            Integer devicePoolCount = deviceIdPoolMapper.selectCount(deviceIdPoolPo);
            deviceModelVo.setDevicePoolCount(devicePoolCount);

        }
        return deviceModelVo;
    }

    /**
     * ?????????????????? ?????? ??????????????????
     *
     * @param modelId
     * @return
     */
    public List<DeviceModelAbilityVo> selectModelAbilitysByModelId(Integer modelId, Integer typeId) {

        /*?????????????????????????????????*/

        //?????????????????????????????? ?????????
        List<DeviceModelAbilityPo> deviceModelAbilityPos = deviceModelAbilityMapper.selectByModelId(modelId);
        //?????? ??????????????????????????????
        List<DeviceTypeAbilitysPo> deviceTypeAbilitysPos = deviceTypeAbilitysMapper.selectByTypeId(typeId);

        /* ??? ??????????????????????????? ?????? ?????? ????????????????????? ????????????????????????*/
        List<DeviceModelAbilityVo> deviceModelAbilityVos = new ArrayList<DeviceModelAbilityVo>();
        if (deviceModelAbilityPos != null && deviceModelAbilityPos.size() > 0) {
            deviceModelAbilityVos = deviceModelAbilityPos.stream().map(deviceModelAbilityPo -> {

                DeviceModelAbilityVo deviceModelAbilityVo = new DeviceModelAbilityVo();
                deviceModelAbilityVo.setId(deviceModelAbilityPo.getId());
                deviceModelAbilityVo.setModelId(deviceModelAbilityPo.getModelId());
                deviceModelAbilityVo.setAbilityId(deviceModelAbilityPo.getAbilityId());

                deviceModelAbilityVo.setAbilityType(deviceModelAbilityPo.getAbilityType());
                deviceModelAbilityVo.setDefinedName(deviceModelAbilityPo.getDefinedName());
                deviceModelAbilityVo.setDirValue(deviceModelAbilityPo.getDirValue());
                deviceModelAbilityVo.setMinVal(deviceModelAbilityPo.getMinVal());
                deviceModelAbilityVo.setMaxVal(deviceModelAbilityPo.getMaxVal());
                deviceModelAbilityVo.setStatus(deviceModelAbilityPo.getStatus());

                /*??????????????????????????? ??????????????????*/
                if (deviceTypeAbilitysPos != null && deviceTypeAbilitysPos.size() > 0) {
                    for (int m = 0; m < deviceTypeAbilitysPos.size(); m++) {
                        DeviceTypeAbilitysPo deviceTypeAbilitysPo = deviceTypeAbilitysPos.get(m);
                        Integer typeAbilityId = deviceTypeAbilitysPo.getAbilityId();
                        //??? ????????????????????? ???????????????????????????????????? ??????
                        if (typeAbilityId.equals(deviceModelAbilityPo.getAbilityId())) {
                            //?????? ???????????????????????????????????????????????????
                            if (deviceTypeAbilitysPo.getAbilityStatus().equals(CommonConstant.STATUS_YES)) {
                                deviceModelAbilityVo.setUpdateStatus(DeviceConstant.DEVICE_MODEL_ABILITY_UPDATE_NORMAL);
                            } else {
                                //?????????????????????????????????????????????????????????????????????????????????????????????????????????
                                deviceModelAbilityVo.setUpdateStatus(DeviceConstant.DEVICE_MODEL_ABILITY_UPDATE_DISABIE);
                            }

                            break;
                        }
                        //??? ????????????????????? ????????? ???????????????????????? ?????????????????????????????????
                        if (!typeAbilityId.equals(deviceModelAbilityPo.getAbilityId()) && m == deviceTypeAbilitysPos.size() - 1) {
                            deviceModelAbilityVo.setUpdateStatus(DeviceConstant.DEVICE_MODEL_ABILITY_UPDATE_MINUS);
                            break;
                        }
                    }
                }
                //?????? ????????????????????? ??????
                List<DeviceModelAbilityVo.DeviceModelAbilityOptionVo> deviceModelAbilityOptionVos = selectModelAbilityOptionsByModelAbilityId(deviceModelAbilityPo.getId(), deviceModelAbilityPo.getAbilityId());

                deviceModelAbilityVo.setDeviceModelAbilityOptions(deviceModelAbilityOptionVos);
                return deviceModelAbilityVo;
            }).collect(Collectors.toList());
        }

        /* ??? ?????? ????????????????????? ?????? ?????? ????????????????????? ???????????????????????????*/
        if (deviceTypeAbilitysPos != null && deviceTypeAbilitysPos.size() > 0) {
            for (int m = 0; m < deviceTypeAbilitysPos.size(); m++) {
                DeviceTypeAbilitysPo deviceTypeAbilitysPo = deviceTypeAbilitysPos.get(m);
                Integer typeAbilityId = deviceTypeAbilitysPo.getAbilityId();
                if (deviceModelAbilityPos != null && deviceModelAbilityPos.size() > 0) {
                    for (int a = 0; a < deviceModelAbilityPos.size(); a++) {
                        DeviceModelAbilityPo deviceModelAbilitypo = deviceModelAbilityPos.get(a);
                        if (typeAbilityId.equals(deviceModelAbilitypo.getAbilityId())) {
                            break;
                        }
                        if (!typeAbilityId.equals(deviceModelAbilitypo.getAbilityId()) && a == deviceModelAbilityPos.size() - 1) {
                            /*????????????????????????????????????*/
                            DeviceModelAbilityVo deviceModelAbilityVo = operateNewModelAbility(modelId, typeAbilityId, deviceTypeAbilitysPo);
                            deviceModelAbilityVos.add(deviceModelAbilityVo);
                        }
                    }

                } else {
                    /*????????????????????????????????????*/
                    DeviceModelAbilityVo deviceModelAbilityVo = operateNewModelAbility(modelId, typeAbilityId, deviceTypeAbilitysPo);
                    deviceModelAbilityVos.add(deviceModelAbilityVo);
                }
            }
        }


        return deviceModelAbilityVos;
    }

    /**
     * ?????????????????????????????????????????????????????????????????? ????????????????????????
     *
     * @param modelId
     * @return
     */
    private DeviceModelAbilityVo operateNewModelAbility(Integer modelId, Integer typeAbilityId, DeviceTypeAbilitysPo deviceTypeAbilitysPo) {

        DeviceModelAbilityVo deviceModelAbilityVo = new DeviceModelAbilityVo();
        deviceModelAbilityVo.setId(null);
        deviceModelAbilityVo.setModelId(modelId);
        deviceModelAbilityVo.setAbilityId(typeAbilityId);
        deviceModelAbilityVo.setAbilityType(deviceTypeAbilitysPo.getAbilityType());
        deviceModelAbilityVo.setDefinedName(deviceTypeAbilitysPo.getAbilityName());
        deviceModelAbilityVo.setDirValue(deviceTypeAbilitysPo.getDirValue());
        deviceModelAbilityVo.setMinVal(deviceTypeAbilitysPo.getMinVal());
        deviceModelAbilityVo.setMaxVal(deviceTypeAbilitysPo.getMaxVal());
        deviceModelAbilityVo.setStatus(null);
        deviceModelAbilityVo.setUpdateStatus(DeviceConstant.DEVICE_MODEL_ABILITY_UPDATE_ADD);


        List<DeviceAbilityOptionPo> deviceAbilityOptionPos = deviceAbilityOptionMapper.selectOptionsByAbilityId(typeAbilityId);
        if (deviceAbilityOptionPos != null && deviceAbilityOptionPos.size() > 0) {
            List<DeviceModelAbilityVo.DeviceModelAbilityOptionVo> deviceAbilityOptionVos = deviceAbilityOptionPos.stream().map(deviceAbilityOptionPo -> {
                DeviceModelAbilityVo.DeviceModelAbilityOptionVo deviceModelAbilityOptionVo = new DeviceModelAbilityVo.DeviceModelAbilityOptionVo();

                deviceModelAbilityOptionVo.setAbilityOptionId(deviceAbilityOptionPo.getId());
                deviceModelAbilityOptionVo.setDefinedName(deviceAbilityOptionPo.getOptionName());
                deviceModelAbilityOptionVo.setMaxVal(deviceAbilityOptionPo.getMaxVal());
                deviceModelAbilityOptionVo.setMinVal(deviceAbilityOptionPo.getMinVal());
                deviceModelAbilityOptionVo.setStatus(CommonConstant.STATUS_YES);

                return deviceModelAbilityOptionVo;
            }).collect(Collectors.toList());

            deviceModelAbilityVo.setDeviceModelAbilityOptions(deviceAbilityOptionVos);
        } else {
            deviceModelAbilityVo.setDeviceModelAbilityOptions(null);
        }

        return deviceModelAbilityVo;
    }

    /**
     * ?????? ????????????????????? ?????? ??????????????????
     *
     * @param modelAbilityId
     * @return
     */
    public List<DeviceModelAbilityVo.DeviceModelAbilityOptionVo> selectModelAbilityOptionsByModelAbilityId(Integer modelAbilityId, Integer ability) {

        List<DeviceModelAbilityOptionPo> deviceModelAbilityOptionPos = deviceModelAbilityOptionMapper.getOptionsByModelAbilityId(modelAbilityId);

        List<DeviceAbilityOptionPo> deviceAbilityOptionPos = deviceAbilityOptionMapper.selectOptionsByAbilityId(ability);

        List<DeviceModelAbilityVo.DeviceModelAbilityOptionVo> deviceModelAbilityOptionVos = new ArrayList<>();

        //???????????? ??????????????????
        if (deviceAbilityOptionPos != null && deviceAbilityOptionPos.size() > 0) {
            for (int i = 0; i < deviceAbilityOptionPos.size(); i++) {
                DeviceAbilityOptionPo deviceAbilityOptionPo = deviceAbilityOptionPos.get(i);

                DeviceModelAbilityVo.DeviceModelAbilityOptionVo deviceModelAbilityOptionVo = new DeviceModelAbilityVo.DeviceModelAbilityOptionVo();

                deviceModelAbilityOptionVo.setAbilityOptionId(deviceAbilityOptionPo.getId());
                deviceModelAbilityOptionVo.setDefinedName(deviceAbilityOptionPo.getOptionName());
                deviceModelAbilityOptionVo.setDefaultVal(deviceAbilityOptionPo.getDefaultValue());
                deviceModelAbilityOptionVo.setOptionValue(deviceAbilityOptionPo.getOptionValue());
                deviceModelAbilityOptionVo.setMinVal(deviceAbilityOptionPo.getMinVal());
                deviceModelAbilityOptionVo.setMaxVal(deviceAbilityOptionPo.getMaxVal());
                deviceModelAbilityOptionVo.setStatus(deviceAbilityOptionPo.getStatus());
                deviceModelAbilityOptionVo.setId(null);
                deviceModelAbilityOptionVo.setUpdateStatus(DeviceConstant.DEVICE_MODEL_ABILITY_UPDATE_ADD);

                if (deviceModelAbilityOptionPos != null && deviceModelAbilityOptionPos.size() > 0) {
                    for (int m = 0; m < deviceModelAbilityOptionPos.size(); m++) {
                        DeviceModelAbilityOptionPo deviceModelAbilityOptionPo = deviceModelAbilityOptionPos.get(m);
                        if (deviceModelAbilityOptionPo.getAbilityOptionId().equals(deviceAbilityOptionPo.getId())) {
                            deviceModelAbilityOptionVo.setActualOptionValue(deviceModelAbilityOptionPo.getActualOptionValue());
                            deviceModelAbilityOptionVo.setDefinedName(deviceModelAbilityOptionPo.getDefinedName());
                            deviceModelAbilityOptionVo.setDefaultVal(deviceModelAbilityOptionPo.getDefaultValue());
                            deviceModelAbilityOptionVo.setMinVal(deviceModelAbilityOptionPo.getMinVal());
                            deviceModelAbilityOptionVo.setMaxVal(deviceModelAbilityOptionPo.getMaxVal());
                            deviceModelAbilityOptionVo.setStatus(deviceModelAbilityOptionPo.getStatus());
                            deviceModelAbilityOptionVo.setUpdateStatus(DeviceConstant.DEVICE_MODEL_ABILITY_UPDATE_NORMAL);
                            deviceModelAbilityOptionVo.setId(deviceModelAbilityOptionPo.getId());
                            break;
                        }
                    }

                }
                deviceModelAbilityOptionVos.add(deviceModelAbilityOptionVo);
            }

        } else {
            return null;
        }

        return deviceModelAbilityOptionVos;
    }


    /**
     * ???????????? ?????? ??????
     *
     * @param modelId
     * @return
     */
    public ApiResponse<Boolean> deleteModelById(Integer modelId) {

        List<DevicePo> devicePos = deviceMapper.selectByModelId(modelId);
        if (devicePos != null && devicePos.size() > 0) {
            return new ApiResponse<>(RetCode.PARAM_ERROR, "???????????????????????????????????????????????????????????????");
        }

        /*??????????????????*/

        DeviceModelPo delModel = new DeviceModelPo();
        delModel.setId(modelId);
        delModel.setStatus(CommonConstant.STATUS_DEL);
        delModel.setLastUpdateTime(System.currentTimeMillis());
        deviceModelMapper.updateStatusById(delModel);

        /*????????????????????????????????????*/
        List<DeviceModelFormatPo> deviceModelFormatPages = deviceModelFormatMapper.obtainModelFormatByModelId(modelId);
        if(deviceModelFormatPages!=null&&deviceModelFormatPages.size()>0){
            deviceModelFormatPages.stream().forEach(
                    deviceModelFormatPo -> {
                        DeviceModelFormatItemPo delItemPo = new DeviceModelFormatItemPo();

                        delItemPo.setModelFormatId(deviceModelFormatPo.getId());
                        delItemPo.setLastUpdateTime(System.currentTimeMillis());
                        delItemPo.setStatus(CommonConstant.STATUS_DEL);
                        deviceModelFormatItemMapper.updateStatusByModelFormatId(delItemPo);
                    }
            );
        }
        DeviceModelFormatPo delFormatPo = new DeviceModelFormatPo();
        delFormatPo.setModelId(modelId);
        delFormatPo.setLastUpdateTime(System.currentTimeMillis());
        delFormatPo.setStatus(CommonConstant.STATUS_DEL);
        deviceModelFormatMapper.updateStatusByModelId(delFormatPo);

        /*?????? ????????????????????????????????????*/
        List<DeviceModelAbilityPo> deviceModelAbilityPos = deviceModelAbilityMapper.selectByModelId(modelId);
        if(deviceModelAbilityPos!=null&&deviceModelAbilityPos.size()>0){
            deviceModelAbilityPos.stream().forEach(
                    deviceModelAbilityPo -> {
                        DeviceModelAbilityOptionPo delOptionPo = new DeviceModelAbilityOptionPo();
                        delOptionPo.setModelAbilityId(deviceModelAbilityPo.getId());
                        delOptionPo.setLastUpdateTime(System.currentTimeMillis());
                        delOptionPo.setStatus(CommonConstant.STATUS_DEL);
                        deviceModelAbilityOptionMapper.updateStatusByModelAbilityId(delOptionPo);
                    }
            );
        }

        DeviceModelAbilityPo delAbilityPo = new DeviceModelAbilityPo();
        delAbilityPo.setModelId(modelId);
        delAbilityPo.setLastUpdateTime(System.currentTimeMillis());
        delAbilityPo.setStatus(CommonConstant.STATUS_DEL);
        deviceModelAbilityMapper.updateStatusByModelId(delAbilityPo);

        return new ApiResponse<>(RetCode.OK, "????????????");
    }

    /**
     * ???????????? ?????? ?????????????????????
     *
     * @param modelId
     * @return
     */
    public ApiResponse<Boolean> deleteModelByIdForce(Integer modelId) throws Exception {

        List<DevicePo> devicePos = deviceMapper.selectByModelId(modelId);
        if (devicePos != null && devicePos.size() > 0) {
            ApiResponse<Boolean> ret = deviceOperateService.callBackDeviceList(devicePos);

            if (ret.getCode() != RetCode.OK) {
                throw new BusinessException(ret.getMsg());
            }
        }
        /*??????????????????*/
        DeviceModelPo delModel = deviceModelMapper.selectById(modelId);
        delModel.setStatus(CommonConstant.STATUS_DEL);
        delModel.setLastUpdateTime(System.currentTimeMillis());
        deviceModelMapper.updateStatusById(delModel);

        /*????????????????????????????????????*/
        List<DeviceModelFormatPo> deviceModelFormatPages = deviceModelFormatMapper.obtainModelFormatByModelId(modelId);
        if(deviceModelFormatPages!=null&&deviceModelFormatPages.size()>0){
            deviceModelFormatPages.stream().forEach(
                    deviceModelFormatPo -> {
                        DeviceModelFormatItemPo delItemPo = new DeviceModelFormatItemPo();

                        delItemPo.setModelFormatId(deviceModelFormatPo.getId());
                        delItemPo.setLastUpdateTime(System.currentTimeMillis());
                        delItemPo.setStatus(CommonConstant.STATUS_DEL);
                        deviceModelFormatItemMapper.updateStatusByModelFormatId(delItemPo);
                    }
            );
        }
        DeviceModelFormatPo delFormatPo = new DeviceModelFormatPo();
        delFormatPo.setModelId(modelId);
        delFormatPo.setLastUpdateTime(System.currentTimeMillis());
        delFormatPo.setStatus(CommonConstant.STATUS_DEL);
        deviceModelFormatMapper.updateStatusByModelId(delFormatPo);

        /*?????? ????????????????????????????????????*/
        List<DeviceModelAbilityPo> deviceModelAbilityPos = deviceModelAbilityMapper.selectByModelId(modelId);
        if(deviceModelAbilityPos!=null&&deviceModelAbilityPos.size()>0){
            deviceModelAbilityPos.stream().forEach(
                    deviceModelAbilityPo -> {
                        DeviceModelAbilityOptionPo delOptionPo = new DeviceModelAbilityOptionPo();
                        delOptionPo.setModelAbilityId(deviceModelAbilityPo.getId());
                        delOptionPo.setLastUpdateTime(System.currentTimeMillis());
                        delOptionPo.setStatus(CommonConstant.STATUS_DEL);
                        deviceModelAbilityOptionMapper.updateStatusByModelAbilityId(delOptionPo);
                    }
            );
        }

        DeviceModelAbilityPo delAbilityPo = new DeviceModelAbilityPo();
        delAbilityPo.setModelId(modelId);
        delAbilityPo.setLastUpdateTime(System.currentTimeMillis());
        delAbilityPo.setStatus(CommonConstant.STATUS_DEL);
        deviceModelAbilityMapper.updateStatusByModelId(delAbilityPo);


        return new ApiResponse<>(RetCode.OK, "????????????");
    }

    /**
     * ?????? ???????????? ???????????????
     *
     * @param modelFormatRequest
     * @return
     */
    public Boolean createOrUpdateModelFormat(DeviceModelFormatCreateRequest modelFormatRequest, Integer modelId, Integer formatId) {

        Boolean ret = true;
        try {
            //???????????? ???????????????????????????
            if (modelFormatRequest.getModelFormatPages() != null && modelFormatRequest.getModelFormatPages().size() > 0) {
                modelFormatRequest.getModelFormatPages().stream().forEach(modelFormatPageCreateRequest -> {

                    DeviceModelFormatPo deviceModelFormatPo = new DeviceModelFormatPo();

                    if (modelFormatPageCreateRequest != null) {
                        BeanUtils.copyProperties(modelFormatPageCreateRequest, deviceModelFormatPo);
                    }
                    deviceModelFormatPo.setFormatId(formatId);
                    deviceModelFormatPo.setModelId(modelId);

                    if (deviceModelFormatPo.getId() != null && deviceModelFormatPo.getId() > 0) {
                        //?????? ???????????????????????????????????????
                        if (!CommonConstant.STATUS_DEL.equals(modelFormatPageCreateRequest.getStatus())) {
                            deviceModelFormatPo.setStatus(CommonConstant.STATUS_YES);
                        }
                        deviceModelFormatPo.setLastUpdateTime(System.currentTimeMillis());
                        deviceModelFormatMapper.updateById(deviceModelFormatPo);
                    } else {
                        deviceModelFormatPo.setStatus(CommonConstant.STATUS_YES);
                        deviceModelFormatPo.setCreateTime(System.currentTimeMillis());
                        deviceModelFormatMapper.insert(deviceModelFormatPo);
                    }

                    //???????????? ????????? ???????????????
                    if (modelFormatPageCreateRequest.getModelFormatItems() != null && modelFormatPageCreateRequest.getModelFormatItems().size() > 0) {
                        modelFormatPageCreateRequest.getModelFormatItems().stream().forEach(modelFormatItemCreateRequest -> {

                            DeviceModelFormatItemPo deviceModelFormatItemPo = new DeviceModelFormatItemPo();


                            deviceModelFormatItemPo.setItemId(modelFormatItemCreateRequest.getItemId());
                            deviceModelFormatItemPo.setModelFormatId(deviceModelFormatPo.getId());
                            deviceModelFormatItemPo.setShowName(modelFormatItemCreateRequest.getShowName());
                            deviceModelFormatItemPo.setShowStatus(modelFormatItemCreateRequest.getShowStatus());

                            deviceModelFormatItemPo.setAbilityIds(modelFormatItemCreateRequest.getAbilityId());

                            //????????????
                            if (modelFormatItemCreateRequest.getId() != null && modelFormatItemCreateRequest.getId() > 0) {

                                if (!CommonConstant.STATUS_DEL.equals(modelFormatItemCreateRequest.getStatus())) {
                                    deviceModelFormatItemPo.setStatus(CommonConstant.STATUS_YES);
                                } else {
                                    deviceModelFormatItemPo.setStatus(CommonConstant.STATUS_DEL);
                                }
                                deviceModelFormatItemPo.setId(modelFormatItemCreateRequest.getId());
                                deviceModelFormatItemPo.setLastUpdateTime(System.currentTimeMillis());
                                deviceModelFormatItemMapper.updateById(deviceModelFormatItemPo);
                            } else {
                                //????????????

                                deviceModelFormatItemPo.setStatus(CommonConstant.STATUS_YES);
                                deviceModelFormatItemPo.setCreateTime(System.currentTimeMillis());
                                deviceModelFormatItemMapper.insert(deviceModelFormatItemPo);
                            }

                        });
                    }

                });
            }
        } catch (Exception e) {
            log.error("????????????????????????", e);
            throw new RuntimeException("????????????????????????");
        }
        return ret;

    }

    /**
     * ?????????????????? ?????? ??????????????????
     *
     * @param modelId
     * @return //
     */
    public ModelFormatVo selectModelFormatPages(Integer modelId, Integer formatId) {

        ModelFormatVo modelFormatVo = new ModelFormatVo();
        //??????????????????????????????
        List<DeviceModelFormatPo> deviceModelFormatPages = deviceModelFormatMapper.obtainModelFormatPages(modelId, formatId);
        List<ModelFormatVo.DeviceModelFormatPageVo> deviceModelFormatPageVos = new ArrayList<ModelFormatVo.DeviceModelFormatPageVo>();
        if (deviceModelFormatPages != null && deviceModelFormatPages.size() > 0) {
            deviceModelFormatPageVos = deviceModelFormatPages.stream().map(deviceModelFormatPagePo -> {
                ModelFormatVo.DeviceModelFormatPageVo deviceModelFormatPageVo = new ModelFormatVo.DeviceModelFormatPageVo();
                deviceModelFormatPageVo.setId(deviceModelFormatPagePo.getId());
                deviceModelFormatPageVo.setPageId(deviceModelFormatPagePo.getPageId());
                deviceModelFormatPageVo.setShowName(deviceModelFormatPagePo.getShowName());

                deviceModelFormatPageVo.setShowStatus(deviceModelFormatPagePo.getShowStatus());
                deviceModelFormatPageVo.setStatus(deviceModelFormatPagePo.getStatus());
                deviceModelFormatPageVo.setFormatId(formatId);
                deviceModelFormatPageVo.setModelId(modelId);

                WxFormatPagePo wxFormatPagePo = wxFormatPageMapper.selectById(deviceModelFormatPagePo.getPageId());
                if (wxFormatPagePo != null) {
                    deviceModelFormatPageVo.setShowImg(wxFormatPagePo.getShowImg());
                }
                /*
                 *  ??????????????????????????????????????????????????????????????????????????????????????????*/
                //?????? ???????????????
                List<WxFormatItemPo> wxFormatItemPos = wxFormatItemMapper.selectByPageId(formatId, deviceModelFormatPagePo.getPageId());
                //?????? ????????????????????????????????????
                List<DeviceModelFormatItemPo> deviceModelFormatItempos = deviceModelFormatItemMapper.obtainModelFormatItems(deviceModelFormatPagePo.getId());
                List<ModelFormatVo.DeviceModelFormatItemVo> deviceModelFormatItemVos = new ArrayList<ModelFormatVo.DeviceModelFormatItemVo>();
                if (wxFormatItemPos != null && wxFormatItemPos.size() > 0) {
                    deviceModelFormatItemVos = wxFormatItemPos.stream().map(wxFormatItemPo -> {
                        ModelFormatVo.DeviceModelFormatItemVo deviceModelFormatItemVo = new ModelFormatVo.DeviceModelFormatItemVo();

                        deviceModelFormatItemVo.setModelFormatId(deviceModelFormatPagePo.getId());
                        deviceModelFormatItemVo.setItemId(wxFormatItemPo.getId());
                        deviceModelFormatItemVo.setShowName(wxFormatItemPo.getName());
                        deviceModelFormatItemVo.setShowStatus(DeviceConstant.DEVICE_MODEL_FORMAT_ITEM_SHOW_NO);
                        deviceModelFormatItemVo.setStatus(CommonConstant.STATUS_YES);
                        deviceModelFormatItemVo.setAbilityType(wxFormatItemPo.getAbilityType());

                        //?????? ?????? ???????????????????????????
                        if (deviceModelFormatItempos != null && deviceModelFormatItempos.size() > 0) {
                            for (int i = 0; i < deviceModelFormatItempos.size(); i++) {
                                DeviceModelFormatItemPo deviceModelFormatItemPo = deviceModelFormatItempos.get(i);
                                //?????? ?????????id??????
                                if (deviceModelFormatItemPo.getItemId().equals(wxFormatItemPo.getId())) {

                                    deviceModelFormatItemVo.setId(deviceModelFormatItemPo.getId());
                                    deviceModelFormatItemVo.setAbilityId(deviceModelFormatItemPo.getAbilityIds());
                                    deviceModelFormatItemVo.setShowName(deviceModelFormatItemPo.getShowName());
                                    deviceModelFormatItemVo.setShowStatus(deviceModelFormatItemPo.getShowStatus());
                                    deviceModelFormatItemVo.setStatus(deviceModelFormatItemPo.getStatus());
                                    break;
                                }

                            }

                        }

                        return deviceModelFormatItemVo;
                    }).collect(Collectors.toList());

                    deviceModelFormatPageVo.setModelFormatItems(deviceModelFormatItemVos);
                }

                return deviceModelFormatPageVo;

            }).collect(Collectors.toList());

            modelFormatVo.setModelFormatPages(deviceModelFormatPageVos);
        } else {
            modelFormatVo = null;
        }
        return modelFormatVo;
    }


    /**
     * ?????????model?????????
     * @return
     */
    public List<ModelProjectRsp> selectModelDict() {
        Integer customerId = customerService.obtainCustomerId(false);
        return deviceModelMapper.selectProjectRspByCustomerId(customerId);
    }

    public List<ProjectModelPercentVo> queryModelPercent(Integer projectId) {
        ProjectBaseInfo projectBaseInfo = projectMapper.selectById(projectId);
        List<DevicePo> devicePoList = deviceGroupItemMapper.selectByGroupIds(projectBaseInfo.getGroupIds());
        List<Integer> deviceIdList = devicePoList.stream().map(e -> e.getId()).distinct().collect(Collectors.toList());
        return deviceModelMapper.queryModelPercent(deviceIdList);
    }

    public List<DeviceModelVo> queryTypeByCustomerId(Integer customerId) {
      List<DeviceModelPo> deviceModelVos =   deviceModelMapper.queryTypeByCustomerId(customerId);
      return deviceModelVos.stream().map(deviceModelPo -> {
          DeviceModelVo deviceModelVo = new DeviceModelVo();
          deviceModelVo.setId(deviceModelPo.getId());
          deviceModelVo.setName(deviceModelPo.getName());
          deviceModelVo.setTypeId(deviceModelPo.getTypeId());
          deviceModelVo.setProductId(deviceModelPo.getProductId());
          return deviceModelVo;
      }).collect(Collectors.toList());
    }
}
