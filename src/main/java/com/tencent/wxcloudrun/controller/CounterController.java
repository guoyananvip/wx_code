package com.tencent.wxcloudrun.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.tencent.wxcloudrun.vo.Health;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.CounterRequest;
import com.tencent.wxcloudrun.model.Counter;
import com.tencent.wxcloudrun.service.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * counter控制器
 */
@Slf4j
@RestController
@ResponseBody
public class CounterController {

  final CounterService counterService;
  final Logger logger;

  public CounterController(@Autowired CounterService counterService) {
    this.counterService = counterService;
    this.logger = LoggerFactory.getLogger(CounterController.class);
  }


  /**
   * 获取当前计数
   * @return API response json
   */
  @GetMapping(value = "/api/count")
  ApiResponse get() {
    logger.info("小郭测试");
    logger.info("/api/count get request 来喽");
    Optional<Counter> counter = counterService.getCounter(1);
    Integer count = 0;
    if (counter.isPresent()) {
      count = counter.get().getCount();
    }

    return ApiResponse.ok(count);
  }


  /**
   * 更新计数，自增或者清零
   * @param request {@link CounterRequest}
   * @return API response json
   */
  @PostMapping(value = "/api/count")
  ApiResponse create(@RequestBody CounterRequest request) {
    logger.info("触发行为");
    logger.info("/api/count post request, action: {}", request.getAction());

    Optional<Counter> curCounter = counterService.getCounter(1);
    if (request.getAction().equals("inc")) {
      Integer count = 1;
      if (curCounter.isPresent()) {
        count += curCounter.get().getCount();
      }
      Counter counter = new Counter();
      counter.setId(1);
      counter.setCount(count);
      counterService.upsertCount(counter);
      return ApiResponse.ok(count);
    } else if (request.getAction().equals("clear")) {
      if (!curCounter.isPresent()) {
        return ApiResponse.ok(0);
      }
      counterService.clearCount(1);
      return ApiResponse.ok(0);
    } else {
      return ApiResponse.error("参数action错误");
    }
  }
  /**
   * @param urlMap key : 展示给外部看的字符串 |  value: 点击跳转的字符串
   * @return
   */
  public static String getUrlTemp(Map<String, String> urlMap) {
    StringJoiner result = new StringJoiner("\n\n");

    /**
     * msgmenucontent自动回复，需要转码: %E6%88%91%E6%98%AF%E6%9C%8D%E5%8A%A1%E5%99%A8%E7%9A%84%E8%87%AA%E5%8A%A8%E5%9B%9E%E5%A4%8D
     * count: 1
     * 展示给外部看的:点击我，触发自动回复
     */
    Integer count = 1;
    String temp = "<a href=\"weixin://bizmsgmenu?msgmenucontent={}&msgmenuid={}\">{}</a>";

    for (Map.Entry<String, String> stringStringEntry : urlMap.entrySet()) {
      String line = count + "、 " + StrUtil.format(temp, URLUtil.encode(stringStringEntry.getValue()), count.toString(), stringStringEntry.getKey());
      result.add(line);
      count++;
    }
    return result.toString() + "\n";
  }

  // 主动推送demo
  // @PostMapping(value = "/automatic-reply",produces = {"application/json"})
  public String automaticReply2(@RequestBody JSONObject jsonObject, HttpServletRequest request)  {
    String userOpenId = request.getHeader("x-wx-openid");
    log.info("获取用户openId={}", userOpenId);
    JSONObject reqq = new JSONObject();
    reqq.put("touser", userOpenId);
    reqq.put("msgtype", "text");
    JSONObject text = new JSONObject();
    text.put("content", "这是我发送的回复上下文，通过open-api发送");
    reqq.put("text", text);
    log.info("构建http请求发起主动推送消息,reqq={}", JSON.toJSONString(reqq));
    String post = HttpUtil.post("http://api.weixin.qq.com/cgi-bin/message/custom/send", reqq);
    log.info("http请求发起主动推送消息获取响应信息post={}", JSON.toJSONString(post));
    return "success";
  }

  @PostMapping(value = "/automatic-reply", produces = {"application/json"})
  public JSONObject automaticReply(@RequestBody JSONObject jsonObject, HttpServletRequest request)  {
    log.info("接受请求参数,json={}", JSON.toJSONString(jsonObject));

    // 获取入参

    Object toUserName = jsonObject.get("ToUserName");
    Object fromUserName = jsonObject.get("FromUserName");
    String Content = (String) jsonObject.get("Content");
    Long createTime = System.currentTimeMillis() / 1000;

    // 构建响应
    JSONObject resp = new JSONObject();
    if (Content.contains("帮助")) {
      // 注意toUserName和fromUserName需要反过来
      // 需要首字母大写
      resp.put("ToUserName", fromUserName);
      resp.put("FromUserName", toUserName);
      resp.put("CreateTime", createTime);
      resp.put("MsgType", "text");

      HashMap<String, String> map = new HashMap<>();
      map.put("取件相关问题", "我点了取件相关问题");
      map.put("退款相关问题", "我点了退款相关问题");
      map.put("超重补差价相关问题", "我点了超重补差价相关问题");
      map.put("不知道重量怎么下单", "我点了不知道重量怎么下单");
      map.put("按重量还是按体积计费", "我点了按重量还是按体积计费");
      map.put("快递员要求实名寄件", "我点了快递员要求实名寄件");
      map.put("取件码是多少", "我点了取件码是多少");

      String head = "亲，您是不是遇到以下问题，请点击选择\n\n";
      resp.put("Content", head + getUrlTemp(map));
    }

    //测试回复
    /* AtomicInteger user = new AtomicInteger();
     int andIncrement = user.getAndIncrement();
     if ((andIncrement % 4) == 1) {
         resp.put("toUserName", fromUserName);
         resp.put("fromUserName", toUserName);
         resp.put("createTime", createTime);
         resp.put("msgType", msgType);
         resp.put("msgmenu", msgmenu);
     } else if ((andIncrement % 4) == 2) {
         resp.put("toUserName", fromUserName);
         resp.put("fromUserName", toUserName);
         resp.put("CreateTime", createTime);
         resp.put("MsgType", msgType);
         resp.put("msgmenu", msgmenu);
     } else if ((andIncrement % 4) == 3) {
         resp.put("ToUserName", fromUserName);
         resp.put("FromUserName", toUserName);
         resp.put("CreateTime", createTime);
         resp.put("MsgType", "text");
         resp.put("Content", "这是进入第三个菜单的文本消息");
     }else{
         resp.put("toUserName", fromUserName);
         resp.put("fromUserName", toUserName);
         resp.put("createTime", createTime);
         resp.put("msgType", "text");
         resp.put("content", "这是进入第四个菜单的文本消息");
     }*/

    log.info("响应参数,json={}", JSON.toJSONString(resp));
    return resp;
  }

  @PostMapping("/health")
  public String health(@RequestBody Health health) {
    log.info("接受请求参数,json={}", JSON.toJSONString(health));
    return "success";
  }


}