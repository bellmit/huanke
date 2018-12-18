package com.huanke.iot.api.pay;

import com.huanke.iot.api.util.pay.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 描述:
 * 支付Controller
 *
 * @author onlymark
 * @create 2018-12-18 上午9:09
 */
@RequestMapping("/h5/pay")
@RestController
@Slf4j
public class PayController {
    @RequestMapping(value="/pay2")
    public ModelAndView pay2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String order_id= UUID.randomUUID().toString().replace("-", "");
        String body="支付测试";
        Double order =0.01;
        Map<String,String> xmlMap = new HashMap<>();
        Map<String,String> resMap = new HashMap<>();
        //生成签名
        String xml = WeChatUtil.GetWeChatXML(order_id,body,order);
        //xmlMap = XMLUtil.doXMLParse(xml);

        //统一下单
        String SubmitResult = HttpUtil.postData(WeChatUtil.UFDODER_URL,xml);
        SortedMap<Object,Object> SubmitMap = new TreeMap<Object,Object>();
        //解析XML
        resMap = GetWxOrderno.doXMLParse(SubmitResult);
        String result_code  = resMap.get("result_code");
        if("SUCCESS".equals(result_code)) {
            //appId，partnerId，prepayId，nonceStr，timeStamp，package。注意：package的值格式为Sign=WXPay
            SubmitMap.put("appid", WeChatUtil.APP_ID);
            SubmitMap.put("partnerid", WeChatUtil.MCH_ID);
            SubmitMap.put("prepayid", resMap.get("prepay_id"));
            SubmitMap.put("noncestr", resMap.get("nonce_str"));
            Long time = (System.currentTimeMillis() / 1000);
            SubmitMap.put("timestamp", time.toString());
            SubmitMap.put("package", "Sign=WXPay");
            //第二次生成签名
            String sign = PayCommonUtil.createSign("UTF-8", SubmitMap, WeChatUtil.API_KEY);
            SubmitMap.put("sign", sign);
        }
        return null;
    }

    /**
     * 提交支付后的微信异步返回接口
     */
    @RequestMapping(value="/weixinNotify")
    public void weixinNotify(HttpServletRequest request, HttpServletResponse response){
        String out_trade_no=null;
        String return_code =null;
        try {
            InputStream inStream = request.getInputStream();
            ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            outSteam.close();
            inStream.close();
            String resultStr  = new String(outSteam.toByteArray(),"utf-8");
            //logger.info("支付成功的回调："+resultStr);
            System.out.println("支付成功的回调");
            Map<String, Object> resultMap = GetWxOrderno.parseXmlToList(resultStr);
            String result_code = (String) resultMap.get("result_code");
            String is_subscribe = (String) resultMap.get("is_subscribe");
            String transaction_id = (String) resultMap.get("transaction_id");
            String sign = (String) resultMap.get("sign");
            String time_end = (String) resultMap.get("time_end");
            String bank_type = (String) resultMap.get("bank_type");

            out_trade_no = (String) resultMap.get("out_trade_no");
            return_code = (String) resultMap.get("return_code");

            request.setAttribute("out_trade_no", out_trade_no);
            //通知微信.异步确认成功.必写.不然微信会一直通知后台.八次之后就认为交易失败了.
            response.getWriter().write(RequestHandler.setXML("SUCCESS", ""));
        }  catch (Exception e) {
            //logger.error("微信回调接口出现错误：",e);
            System.out.println("微信回调接口出现错误" + e);
            try {
                response.getWriter().write(RequestHandler.setXML("FAIL", "error"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if(return_code.equals("SUCCESS")){
            //支付成功的业务逻辑
        }else{
            //支付失败的业务逻辑
        }
    }
}
