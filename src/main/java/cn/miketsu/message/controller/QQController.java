package cn.miketsu.message.controller;

import cn.hutool.json.JSONUtil;
import cn.miketsu.api.vo.message.qq.Message;
import cn.miketsu.common.vo.ReturnVo;
import cn.miketsu.api.vo.message.qq.ShareUrl;
import cn.miketsu.message.service.QQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author sihuangwlp
 * @date 2022/8/15
 */
@RestController
@RequestMapping("/qq")
@Slf4j
public class QQController {

    @Autowired
    QQService qqService;

    @PostMapping("/sendTextMessage")
    public ReturnVo<String> sendTextMessage(@RequestBody Message message) {
        log.info("QQ发送纯文本消息接口，传入报文：{}", JSONUtil.toJsonStr(message));
        qqService.sendMessage(message.getReceiver(), message.getGroupId(), message.getMessage());
        ReturnVo<String> returnVo = ReturnVo.ok("发送成功");
        log.info("QQ发送纯文本消息接口，返回报文：{}", JSONUtil.toJsonStr(returnVo));
        return returnVo;
    }

    @PostMapping("/shareUrl")
    public ReturnVo<String> shareUrl(@RequestBody ShareUrl shareUrl) {
        log.info("QQ分享链接接口，传入报文：{}", JSONUtil.toJsonStr(shareUrl));
        qqService.shareUrl(shareUrl.getType(), shareUrl.getFriendNum(), shareUrl.getUrl(), shareUrl.getTitle(), shareUrl.getContent(), shareUrl.getCoverUrl());
        ReturnVo<String> returnVo = ReturnVo.ok("分享成功");
        log.info("QQ分享链接接口，返回报文：{}", JSONUtil.toJsonStr(returnVo));
        return returnVo;
    }
}
