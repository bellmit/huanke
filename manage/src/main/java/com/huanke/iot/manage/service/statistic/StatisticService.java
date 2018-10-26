package com.huanke.iot.manage.service.statistic;

import com.huanke.iot.base.api.ApiResponse;
import com.huanke.iot.base.constant.CommonConstant;
import com.huanke.iot.base.constant.RetCode;
import com.huanke.iot.base.dao.customer.CustomerUserMapper;
import com.huanke.iot.base.dao.device.DeviceMapper;
import com.huanke.iot.base.dao.device.typeModel.DeviceTypeMapper;
import com.huanke.iot.base.po.device.DevicePo;
import com.huanke.iot.base.util.CommonUtil;
import com.huanke.iot.manage.service.customer.CustomerService;
import com.huanke.iot.manage.vo.request.device.operate.DeviceHomePageStatisticVo;
import com.huanke.iot.manage.vo.response.device.customer.CustomerUserVo;
import com.huanke.iot.manage.vo.response.device.operate.DeviceStatisticsVo;
import com.huanke.iot.manage.vo.response.device.typeModel.DeviceTypeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.DecimalFormat;
import java.util.*;


@Repository
@Slf4j
public class StatisticService {


    @Autowired
    private CustomerUserMapper customerUserMapper;

    @Autowired
    private DeviceTypeMapper deviceTypeMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CommonUtil commonUtil;


    /**
     * 首页面板-统计用户
     *
     * @param
     * @return
     */

    public List<CustomerUserVo> selectUserCountPerMonth() {
        Calendar cal = Calendar.getInstance();
        int nowYear = cal.get(Calendar.YEAR);
        int preYear = nowYear-1;
        List rtnList = new ArrayList();
        Integer customerId = customerService.obtainCustomerId(false);
        //今年的用户数据
        List nowUserConutList = customerUserMapper.selectCustomerUserCount(nowYear,customerId);
        //去年的用户数据
        List preYearUserList = customerUserMapper.selectCustomerUserCount(preYear,customerId);
        //上个月的用户数量
        Long prevMonthCount = new Long(0);
        //去年的上个月的用户数量
        Long prevYearMonthCount = new Long(0);

        String addPercent = "0.00%";
        for(int i=1;i<=12;i++){
            CustomerUserVo.CustomerUserCountVo customerUserCountVo = new CustomerUserVo.CustomerUserCountVo();
            String nowMonth = i +"月";
            if(i<10){
                nowMonth = "0"+i+"月";
            }
            //今年某月的用户量
            Long userCount = new Long(0);
            //今年某月用户增长量
            Long addCount = new Long(0);
            //去年某月用户量
            Long preYearUserCount =  new Long(0);
            //去年某月用户增长量
            Long preYearAddCount =  new Long(0);

            if(nowUserConutList!=null&&nowUserConutList.size()>0){
                for(int m=0;m<nowUserConutList.size();m++){
                    Map tempMap = (Map)nowUserConutList.get(m);
                    String tempMonth = (String)tempMap.get("userMonth");
                    Long tempUserCount = (Long)tempMap.get("userCount");
                    if(tempMonth.equals(nowMonth)){
                        userCount = tempUserCount;
                        addCount = userCount-prevMonthCount;
                        prevMonthCount = userCount;
                        break;
                    }
                }
            }

            if(preYearUserList!=null&&preYearUserList.size()>0){
                for(int m=0;m<preYearUserList.size();m++){
                    Map preMap = (Map)preYearUserList.get(m);
                    String tempMonth = (String)preMap.get("userMonth");
                    Long tempUserCount = (Long)preMap.get("userCount");
                    if(tempMonth.equals(nowMonth)){
                        preYearUserCount = tempUserCount;
                        preYearAddCount = preYearUserCount-prevYearMonthCount;

                        prevYearMonthCount = preYearUserCount;
                        break;
                    }
                }
            }

            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            df.setMinimumFractionDigits(2);
            //如果除数为0
            if(preYearAddCount==0){
                //被除数为0
                if(addCount==0){
                    addPercent = "--";
                }else{
                    addPercent = df.format(addCount * 100.00 / 1) + "%";
                }
            }else {
                addPercent = df.format((addCount-preYearAddCount) * 100.00 / preYearAddCount) + "%";
            }

            customerUserCountVo.setMonth(nowMonth);
            customerUserCountVo.setAddCount(addCount);
            customerUserCountVo.setUserCount(userCount);
            customerUserCountVo.setAddPercent(addPercent);
            rtnList.add(customerUserCountVo);
        }

        return rtnList;
    }

    /**
     * 首页大数据面板-设备类型统计
     *
     * @return
     */
    public List<DeviceTypeVo.DeviceTypePercent> selectTypePercentPerMonth() {

        List<DeviceTypeVo.DeviceTypePercent> deviceTypePercents = new ArrayList<DeviceTypeVo.DeviceTypePercent>();

        Integer customerId = customerService.obtainCustomerId(false);
        /*查询设备总量*/
        DevicePo queryDevicePo = new DevicePo();
        queryDevicePo.setStatus(CommonConstant.STATUS_YES);
        queryDevicePo.setCustomerId(customerId);
        Integer deviceTotal = deviceMapper.selectCount(queryDevicePo);

        if(deviceTotal!=null&&deviceTotal!=0){
            List deviceTypePercentList = deviceTypeMapper.selectTypePercent(customerId);

            if (deviceTypePercentList != null && deviceTypePercentList.size() > 0) {
                for(int i=0;i<deviceTypePercentList.size();i++){
                    DeviceTypeVo.DeviceTypePercent deviceTypePercent = new DeviceTypeVo.DeviceTypePercent();
                    Map deviceTypePercentMap = (Map)deviceTypePercentList.get(i);
                    Integer typeId = (Integer)deviceTypePercentMap.get("typeId");
                    String typeName = (String)deviceTypePercentMap.get("typeName");
                    Long deviceCount = (Long)deviceTypePercentMap.get("deviceCount");

                    DecimalFormat df = new DecimalFormat();
                    df.setMaximumFractionDigits(2);
                    df.setMinimumFractionDigits(2);
                    String typePercent = df.format(deviceCount * 100.00 / deviceTotal) + "%";

                    deviceTypePercent.setTypeId(typeId);
                    deviceTypePercent.setTypeName(typeName);
                    deviceTypePercent.setDeviceCount(deviceCount);
                    deviceTypePercent.setTypePercent(typePercent);

                    deviceTypePercents.add(deviceTypePercent);
                }
            }
        }

        return deviceTypePercents;

    }

    /**
     * 首页面板-统计设备
     *
     * @param
     * @return
     */

    public List<DeviceStatisticsVo> selectDeviceCountPerMonth() {
        List rtnList = new ArrayList();
        List nowDeviceConutList = new ArrayList();
        List preYearDeviceList = new ArrayList();
        Integer customerId = customerService.obtainCustomerId(false);

        Calendar cal = Calendar.getInstance();
        int nowYear = cal.get(Calendar.YEAR);
        int preYear = nowYear-1;

        if(customerId==null){
            //今年的设备数据
            nowDeviceConutList = deviceMapper.selectDeviceCount(nowYear,CommonConstant.STATUS_YES);
            //去年的设备数据
            preYearDeviceList = deviceMapper.selectDeviceCount(preYear,CommonConstant.STATUS_YES);
        }else{
            //今年的设备数据
            nowDeviceConutList = deviceMapper.selectDeviceCountByCustomer(nowYear,CommonConstant.STATUS_YES,customerId);
            //去年的设备数据
            preYearDeviceList = deviceMapper.selectDeviceCountByCustomer(preYear,CommonConstant.STATUS_YES,customerId);
        }

        //上个月的设备数量
        Long prevMonthCount = new Long(0);
        //去年的上个月的设备数量
        Long prevYearMonthCount = new Long(0);

        String addPercent = "0.00%";
        for(int i=1;i<=12;i++){
            DeviceStatisticsVo deviceStatisticsVo = new DeviceStatisticsVo();
            String nowMonth = i +"月";
            if(i<10){
                nowMonth = "0"+i+"月";
            }
            //今年某月的用户量
            Long deviceCount = new Long(0);
            //今年某月用户增长量
            Long addCount = new Long(0);
            //去年某月用户量
            Long preYearDeviceCount =  new Long(0);
            //去年某月用户增长量
            Long preYearAddCount =  new Long(0);

            if(nowDeviceConutList!=null&&nowDeviceConutList.size()>0){
                for(int m=0;m<nowDeviceConutList.size();m++){
                    Map tempMap = (Map)nowDeviceConutList.get(m);
                    String tempMonth = (String)tempMap.get("deviceMonth");
                    Long tempDeviceCount = (Long)tempMap.get("deviceCount");
                    if(tempMonth.equals(nowMonth)){
                        deviceCount = tempDeviceCount;
                        addCount = deviceCount-prevMonthCount;
                        prevMonthCount = deviceCount;
                        break;
                    }
                }
            }

            if(preYearDeviceList!=null&&preYearDeviceList.size()>0){
                for(int m=0;m<preYearDeviceList.size();m++){
                    Map preMap = (Map)preYearDeviceList.get(m);
                    String tempMonth = (String)preMap.get("deviceMonth");
                    Long tempDeviceCount = (Long)preMap.get("deviceCount");
                    if(tempMonth.equals(nowMonth)){
                        preYearDeviceCount = tempDeviceCount;
                        preYearAddCount = preYearDeviceCount-prevYearMonthCount;

                        prevYearMonthCount = preYearDeviceCount;
                        break;
                    }
                }
            }

            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            df.setMinimumFractionDigits(2);
            //如果除数为0
            if(preYearAddCount==0){
                //被除数为0
                if(addCount==0){
                    addPercent = "--";
                }else{
                    addPercent = df.format(addCount * 100.00 / 1) + "%";
                }
            }else {
                addPercent = df.format((addCount-preYearAddCount) * 100.00 / preYearAddCount) + "%";
            }

            deviceStatisticsVo.setMonth(nowMonth);
            deviceStatisticsVo.setAddCount(addCount);
            deviceStatisticsVo.setDeviceCount(deviceCount);
            deviceStatisticsVo.setAddPercent(addPercent);
            rtnList.add(deviceStatisticsVo);
        }

        return rtnList;
    }

    /**
     * 设备今日增长数
     * @return
     */
    public ApiResponse<Integer> selectDeviceByDay(Integer customerId){
        //获取当前系统时间戳
        Long endTime = System.currentTimeMillis();
        //获取当前日期的0点时间戳
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR,calendar.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH,calendar.get(Calendar.MONTH));
        calendar.set(Calendar.DATE,calendar.get(Calendar.DATE));
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        Long startTime = calendar.getTimeInMillis();

        //查询当日0点时间戳至当前时间戳中的设备数据
        Integer newDeviceOfToday = null;
        if(null!=customerId){
            newDeviceOfToday = this.deviceMapper.selectCustomerDataByTime(startTime,endTime,customerId);
        }else{
            newDeviceOfToday = this.deviceMapper.selectDataByTime(startTime,endTime);
        }
        return new ApiResponse<>(RetCode.OK,"查询今日新增成功",newDeviceOfToday);
    }

    /**
     * 首页统计分析
     * @return
     */
    public ApiResponse<DeviceHomePageStatisticVo> queryHomePageStatistic(){

        DeviceHomePageStatisticVo deviceHomePageStatisticVo = new DeviceHomePageStatisticVo();
        /*当前域名*/
        String userHost = commonUtil.obtainSecondHost();
        /*当前域名的 客户主键*/
        Integer customerId = customerService.obtainCustomerId(false);
        /*统计设备数量*/
        DevicePo queryDevicePo = new DevicePo();
        queryDevicePo.setStatus(CommonConstant.STATUS_YES);
        queryDevicePo.setCustomerId(customerId);
        int totalDeviceCount = deviceMapper.selectCount(queryDevicePo);
        deviceHomePageStatisticVo.setDeviceTotalCount(totalDeviceCount);

        /*统计设备今日增长数量*/
        ApiResponse<Integer> todayDeviceAddCountRtn = selectDeviceByDay(customerId);
        deviceHomePageStatisticVo.setTodayDeviceAddCount(todayDeviceAddCountRtn.getData());


        return new ApiResponse<>(RetCode.OK,"首页统计成功",deviceHomePageStatisticVo);
    }



}
