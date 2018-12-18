package com.huanke.iot.api.util.pay;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 描述:
 *
 * @author onlymark
 * @create 2018-12-17 下午4:15
 */
public class WeChatUtil {
    /**
     * 基本常量设置
     */

    /**
     * APPID
     */
    public static String APP_ID = "wxd1fd5b0032139782";// 微信公众号id
    /**
     * 微信支付商户号
     */
    public static String MCH_ID= "1431479102";// 财付通商户号
    /**
     * 请求路径
     */
    public static String UFDODER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    /**
     * 密匙
     */
    public static String API_KEY = "cdfe9490a9b64fada1542deee61a18f9";// 商户号对应的密钥
    /**
     * 发起支付IP
     */
    public static String CREATE_IP = "39.104.52.84";
    /**
     * 回调url
     */
    public static String NOTIFY_URL = "http://907fa4e4.ngrok.io/weixinNotify";

    /**
     * 生成微信签名
     * @param order_id
     *         订单ID
     * @param body
     *         描述
     * @param order_price
     *         价格
     * @return
     */
    public static  String GetWeChatXML(String order_id, String body, double order_price ){
        String currTime = PayCommonUtil.getCurrTime();
        String strTime = currTime.substring(8, currTime.length());
        String strRandom = PayCommonUtil.buildRandom(4) + "";
        //随机字符串
        /*String nonce_str = strTime + strRandom;//UUID.randomUUID().toString();
        nonce_str=nonce_str.substring(0,16);*/
        String nonce_str = UUID.randomUUID().toString().replace("-", "");
        // 获取发起电脑 ip
        String spbill_create_ip = WeChatUtil.CREATE_IP;
        // 回调接口
        String notify_url = WeChatUtil.NOTIFY_URL;
        //交易类型
        String trade_type = "JSAPI";
        //微信价格最小单位分 转换为整数
        DecimalFormat df = new DecimalFormat("#######.##");
        order_price = order_price * 100;
        order_price = Math.ceil(order_price);
        String price = df.format(order_price);
        Map<Object,Object> packageParams = new LinkedHashMap<Object,Object>();
        packageParams.put("appid", APP_ID);
        packageParams.put("body", body);
        packageParams.put("mch_id", MCH_ID);
        packageParams.put("nonce_str", nonce_str);
        packageParams.put("notify_url", notify_url);
        packageParams.put("openid", "oOSkz0XrnfSunFKRD2yzbF_-SpOE");
        packageParams.put("out_trade_no", order_id);
        packageParams.put("spbill_create_ip", spbill_create_ip);
        packageParams.put("total_fee", price);
        packageParams.put("trade_type", trade_type);
        String sign = PayCommonUtil.createSign("UTF-8", packageParams,API_KEY);
        packageParams.put("sign", sign);
        String requestXML = PayCommonUtil.getRequestXml(packageParams);
        System.out.println(requestXML);
        return requestXML;
    }


}