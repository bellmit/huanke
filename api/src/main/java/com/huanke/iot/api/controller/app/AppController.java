package com.huanke.iot.api.controller.app;

import com.google.common.collect.Lists;
import com.huanke.iot.api.controller.app.response.AppDeviceDataVo;
import com.huanke.iot.api.controller.app.response.AppDeviceListVo;
import com.huanke.iot.api.controller.app.response.AppInfoVo;
import com.huanke.iot.api.controller.app.response.AppSceneVo;
import com.huanke.iot.api.controller.h5.BaseController;
import com.huanke.iot.api.controller.h5.req.*;
import com.huanke.iot.api.controller.h5.response.DeviceModelVo;
import com.huanke.iot.api.controller.h5.response.LocationVo;
import com.huanke.iot.api.controller.h5.response.SensorDataVo;
import com.huanke.iot.api.controller.h5.response.WeatherVo;
import com.huanke.iot.api.service.device.basic.AppBasicService;
import com.huanke.iot.api.service.device.basic.AppDeviceDataService;
import com.huanke.iot.api.service.device.basic.DeviceDataService;
import com.huanke.iot.api.service.device.basic.DeviceService;
import com.huanke.iot.base.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RequestMapping("/app/api/base")
@Slf4j
@RestController
public class AppController extends BaseController {

    @Autowired
    private DeviceDataService deviceDataService;

    @Autowired
    private AppBasicService appBasicService;

    @Autowired
    private AppDeviceDataService appDeviceDataService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Autowired
    private DeviceService deviceService;

    @Value("${apkKey}")
    private String apkKey;

    @GetMapping("/removeIMeiInfo")
    public ApiResponse<Object> removeIMeiInfo(HttpServletRequest request) {
        return appBasicService.removeIMeiInfo(request);
    }

    @GetMapping("/setApkInfo")
    public void setApkInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String objectApiResponse = appBasicService.addUserAppInfo(request);
        response.setContentType(ContentType.TEXT_HTML.getMimeType());
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print("<span style='font-size:55px'>"+objectApiResponse+"</span>");
    }

    @PostMapping("/getQRCode")
    public ApiResponse<Object> getQRCode(HttpServletRequest request) {
        String appId = request.getParameter("appId");
        return new ApiResponse<>(appBasicService.getQRCode(appId));
    }

    @GetMapping("/queryDeviceList")
    public ApiResponse<AppDeviceListVo> queryDeviceList() {
        Integer userId = getCurrentUserIdForApp();
        log.debug("???????????????????????????userId={}", userId);
        try {
            AppDeviceListVo deviceListVo = appDeviceDataService.obtainMyDevice(userId);
            return new ApiResponse<>(deviceListVo);
        }catch (Exception e){
            log.error("queryData error {}",userId ,e);
        }
        return   new ApiResponse(Lists.newArrayList());
    }

    @PostMapping("/queryChildDevice/{hostDeviceId}")
    public Object obtainChildDevice(@PathVariable("hostDeviceId") Integer hostDeviceId) {
        Integer userId = getCurrentUserIdForApp();
        Integer customerId = getCurrentCustomerId();
        log.info("????????????????????????userId={}??? hostDeviceId={}", userId, hostDeviceId);
        List<AppDeviceListVo.DeviceItemPo> childDeviceList = appDeviceDataService.queryChildDevice(hostDeviceId,customerId);
        return new ApiResponse<>(childDeviceList);
    }

    @PostMapping("/getModelVo")
    public ApiResponse<DeviceModelVo> getModelVo(@RequestBody DeviceFormatRequest request) {
        Integer deviceId = request.getDeviceId();
        log.debug("??????????????????deviceId={}", deviceId);
        DeviceModelVo deviceModelVo = appBasicService.getModelVo(deviceId);
        return new ApiResponse<>(deviceModelVo);
    }

    @PostMapping("/queryDetailByDeviceId")
    public ApiResponse<List<AppDeviceDataVo>> queryDetailByDeviceId(@RequestBody DeviceAbilitysRequest request) {
        Integer deviceId = request.getDeviceId();
        log.debug("???????????????????????????ID={}",deviceId);
        List<Integer> abilityIds = request.getAbilityIds();
        List<AppDeviceDataVo> deviceAbilityVos = appDeviceDataService.queryDetailAbilitysValue(deviceId,abilityIds);
        return new ApiResponse<>(deviceAbilityVos);
    }

    @GetMapping("/getWeatherAndLocation/{deviceId}")
    public ApiResponse<List<Object>> queryDeviceWeather(@PathVariable("deviceId") Integer deviceId) {
        log.debug("???????????????????????????ID={}",deviceId);
        WeatherVo weatherVo = deviceService.queryDeviceWeather(deviceId);
        LocationVo locationVo = deviceService.queryDeviceLocation(deviceId);
        if (StringUtils.isNotEmpty(locationVo.getArea())){
            String[] s = locationVo.getArea().split(" ");
            if(s.length>3) {
                locationVo.setArea(StringUtils.isEmpty(s[1])?s[2]:s[1]);
            }
        }
        List resp = new ArrayList();
        resp.add(weatherVo);
        resp.add(locationVo);
        return new ApiResponse<>(resp);
    }

    @PostMapping("/getHistoryData")
    public ApiResponse<List<SensorDataVo>> getHistoryData(@RequestBody HistoryDataRequest request) {
        Integer deviceId = request.getDeviceId();
        Integer type = request.getType();
        Integer userId = getCurrentUserIdForApp();
        log.debug("???????????????????????????userId={}, deviceId={}, type={}", userId, deviceId, type);
        return new ApiResponse<>(appBasicService.getHistoryData(deviceId, type));
    }

    @PostMapping("/editDevice")
    public ApiResponse<Boolean> editDevice(@RequestBody DeviceRequest request) {
        Integer deviceId = request.getDeviceId();
        String deviceName = request.getDeviceName();
        Integer userId = getCurrentUserIdForApp();
        boolean ret = deviceService.editDevice(userId, deviceId, deviceName);
        return new ApiResponse<>(ret);
    }

    @GetMapping("/obtainApk")
    public ApiResponse<AppInfoVo> obtainApk(HttpServletRequest request) {
//        String apkInfo = stringRedisTemplate.opsForValue().get(apkKey);
//        if (StringUtils.isNotEmpty(apkInfo)) {
//            AppInfoVo appInfoVo = JSON.parseObject(apkInfo, AppInfoVo.class);
//            return new ApiResponse<>(appInfoVo);
//        }
//        return new ApiResponse<>();
        String appId = request.getParameter("appId");
        String appNo = request.getParameter("appNo");
        return new ApiResponse<>(appBasicService.getApkInfo(appId,appNo));
    }

    @PostMapping("/sendFunc")
    public ApiResponse<Boolean> sendFuc(@RequestBody DeviceFuncVo deviceFuncVo){
        log.debug("???????????????"+deviceFuncVo.toString());
        String funcId = deviceFuncVo.getFuncId();
        Boolean request = deviceDataService.sendFuncs(deviceFuncVo,getCurrentUserIdForApp(),2);
        return new ApiResponse<>(request);
    }

    @PostMapping("/getAppPassword")
    public ApiResponse<String> getAppPassword(HttpServletRequest request){
        String appId = request.getParameter("appId");
        log.debug("???????????????????????????appId={}",appId);
        String response= appBasicService.getPassword(appId);
        return new ApiResponse<>(response);
    }

    @PostMapping("/getCustomerScene")
    public ApiResponse<AppSceneVo> getCustomerScene(){
        AppSceneVo request= appBasicService.getCustomerSceneInfo();
        return new ApiResponse<>(request);
    }
}
