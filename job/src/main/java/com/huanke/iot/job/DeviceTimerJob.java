package com.huanke.iot.job;

import com.alibaba.fastjson.JSON;
import com.huanke.iot.base.dao.device.DeviceTimerMapper;
import com.huanke.iot.base.dao.device.data.DeviceOperLogMapper;
import com.huanke.iot.base.dao.device.data.DeviceSensorDataMapper;
import com.huanke.iot.base.enums.FuncTypeEnums;
import com.huanke.iot.base.po.device.DeviceTimerPo;
import com.huanke.iot.base.po.device.data.DeviceOperLogPo;
import com.huanke.iot.job.gateway.MqttSendService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class DeviceTimerJob {

    @Autowired
    private MqttSendService mqttSendService;
    @Autowired
    private DeviceTimerMapper deviceTimerMapper;

    @Autowired
    private DeviceOperLogMapper deviceOperLogMapper;

    @Autowired
    private DeviceSensorDataMapper sensorDataMapper;

    @Scheduled(cron = "0 0/30 * * *  ?")
    public void cleanSensorData() {
        DateTime dateTime = new DateTime().plusMinutes(-120);
        sensorDataMapper.clearData(dateTime.getMillis());
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void doWork() {
        try {
            Long t = System.currentTimeMillis();
            log.info("start oncetimeType timer job t = {}", t);
            List<DeviceTimerPo> deviceTimerPos = deviceTimerMapper.queryTimers(t);
            deviceTimerPos.forEach(deviceTimerPo -> {
                Integer deviceId = deviceTimerPo.getDeviceId();
                if (deviceTimerPo.getTimerType() == 1) {
                    sendFunc(deviceId, FuncTypeEnums.MODE.getCode(), 1);
                } else {
                    sendFunc(deviceId, FuncTypeEnums.MODE.getCode(), 0);
                }
                DeviceTimerPo updatePo = new DeviceTimerPo();
                updatePo.setId(deviceTimerPo.getId());
                updatePo.setStatus(4);
                updatePo.setUserId(deviceTimerPo.getUserId());
                updatePo.setExecuteRet(2);
                updatePo.setExecuteTime(System.currentTimeMillis());
                deviceTimerMapper.updateById(updatePo);
            });
            log.info("end oncetimeType timer job");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void doWorkTimerTypeIdea() {
        try {
            Calendar calendar = Calendar.getInstance();
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int hour = calendar.get(Calendar.HOUR);
            int minute = calendar.get(Calendar.MINUTE);
            log.info("start ideaType timer job dayOfWeek = {}", dayOfWeek);
            List<DeviceTimerPo> deviceTimerPos = deviceTimerMapper.queryIdeaTypeTimers(dayOfWeek);
            deviceTimerPos.forEach(deviceTimerPo -> {
                if(deviceTimerPo.getHour() == hour && deviceTimerPo.getMinute() == minute){
                    Integer deviceId = deviceTimerPo.getDeviceId();
                    if (deviceTimerPo.getTimerType() == 1) {
                        sendFunc(deviceId, FuncTypeEnums.MODE.getCode(), 1);
                    } else {
                        sendFunc(deviceId, FuncTypeEnums.MODE.getCode(), 0);
                    }
                }
            });
            log.info("end ideaType timer job");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void doWork1() {
        System.out.println(System.currentTimeMillis());
    }

    /*@Scheduled(cron = "0 0 0 * * ?")
    public void doWorkTypeIdea() {
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        List<DeviceTimerPo> deviceTimerPos = deviceTimerMapper.queryIdeaTimers(dayOfWeek);
        for (DeviceTimerPo deviceTimerPo : deviceTimerPos) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR, deviceTimerPo.getHour());
            calendar.set(Calendar.MINUTE, deviceTimerPo.getMinute());
            calendar.set(Calendar.SECOND, deviceTimerPo.getSecond());
            long targetMillis = calendar.getTimeInMillis();
            long currentMillis = System.currentTimeMillis();
            long delay = targetMillis - currentMillis > 0 ? targetMillis - currentMillis : 0;

            ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    Integer deviceId = deviceTimerPo.getDeviceId();
                    if (deviceTimerPo.getTimerType() == 1) {
                        sendFunc(deviceId, FuncTypeEnums.MODE.getCode(), 1);
                    } else {
                        sendFunc(deviceId, FuncTypeEnums.MODE.getCode(), 0);
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }*/

    public String sendFunc(Integer deviceId, String funcId, Integer funcValue) {
        String topic = "/down/control/" + deviceId;
        String requestId = UUID.randomUUID().toString().replace("-", "");
        DeviceOperLogPo deviceOperLogPo = new DeviceOperLogPo();
        deviceOperLogPo.setFuncId(funcId);
        deviceOperLogPo.setDeviceId(deviceId);
        deviceOperLogPo.setRequestId(requestId);
        deviceOperLogPo.setOperType(4);
        deviceOperLogPo.setOperUserId(0);
        deviceOperLogPo.setCreateTime(System.currentTimeMillis());
        deviceOperLogMapper.insert(deviceOperLogPo);
        FuncListMessage funcListMessage = new FuncListMessage();
        funcListMessage.setMsg_type("control");
        funcListMessage.setMsg_id(requestId);
        FuncItemMessage funcItemMessage = new FuncItemMessage();
        funcItemMessage.setType(funcId);
        funcItemMessage.setValue(String.valueOf(funcValue));
        funcListMessage.setDatas(Lists.newArrayList(funcItemMessage));
        mqttSendService.sendMessage(topic, JSON.toJSONString(funcListMessage));
        return requestId;

    }

    @Data
    public static class FuncItemMessage {
        private String type;
        private String value;
    }

    @Data
    public static class FuncListMessage {
        private String msg_id;
        private String msg_type;
        private List<FuncItemMessage> datas;
    }

}
