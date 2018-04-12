package com.huanke.iot.api.controller;

import com.alibaba.fastjson.JSON;
import com.huanke.iot.api.gateway.MqttSendService;
import lombok.Data;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/sendmessage")
public class MockSendMessageController {
    @Data
    public static class AlarmMessage{
        private Integer index;
        private Integer type;
        private Integer value;
    }

    @Data
    public static class AlarmListMessage{
        private List<AlarmMessage> alarm;
    }

    @Data
    private static class SwitchDataItem{
        private Integer mode;
        private Integer devicelock;
        private Integer childlock;
        private Integer anion;
        private Integer uvl;
        private Integer heater;
        private List<Item> fan;
        private List<Item>  valve;
    }
    @Data
    private static class Item{
        private Integer index;
        private Integer value;
    }

    @Autowired
    private MqttSendService mqttSendService;


    @RequestMapping("/sendControl")
    @ResponseBody
    public String sendControl(@RequestBody SwitchDataItem switchDataItem){
        String topic = "/down/control/1";
        mqttSendService.sendMessage("/up/alarm/1",JSON.toJSONString(switchDataItem));
        return "ok";
    }


    @ResponseBody
    @RequestMapping("/sendAlarm")
    public String sendAlarmMessage(){
        AlarmMessage alarmMessage = new AlarmMessage();
        alarmMessage.setIndex(1);
        alarmMessage.setType(1);
        alarmMessage.setValue(2);
        AlarmListMessage alarmListMessage = new AlarmListMessage();
        alarmListMessage.setAlarm(Lists.newArrayList(alarmMessage));
        String message = JSON.toJSONString(alarmListMessage);
        mqttSendService.sendMessage("/up/alarm/1",message);
        return "111";
    }
}
