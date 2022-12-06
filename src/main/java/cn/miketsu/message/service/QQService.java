package cn.miketsu.message.service;

import cn.miketsu.common.exception.BusException;
import cn.miketsu.message.listener.CommandListener;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.contact.AudioSupported;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.BotConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * @author sihuangwlp
 * @date 2022/8/15
 */
@Service("qqService")
@Slf4j
public class QQService {

    private final Bot bot;

    /**
     * 机器人的QQ号码
     */
    public static Long qqNum = 0L;

    /**
     * 机器人的登录密码
     */
    public static String password = "";

    /**
     * 该项目所有者的QQ号码
     */
    public static Long ownerNum = 0L;

    @Autowired
    private CommandListener commandListener;

    {
        bot = BotFactory.INSTANCE.newBot(qqNum, password, new BotConfiguration() {{
            setHeartbeatStrategy(HeartbeatStrategy.REGISTER);
            setProtocol(MiraiProtocol.MACOS);
            File workingDir = new File(System.getProperty("user.dir") + "/miria");
            if (!workingDir.exists()) {
                workingDir.mkdirs();
            }
            setWorkingDir(workingDir);
            setCacheDir(new File("cache"));
            fileBasedDeviceInfo();
//            redirectBotLogToFile();
            redirectNetworkLogToFile();
        }});
    }

    /**
     * 无参构造，注册事件监听器
     */
    public QQService() {
        bot.login();
    }

    @PostConstruct
    public void addListeners(){
        bot.getEventChannel().registerListenerHost(commandListener);
    }

    /**
     * 发纯文本消息
     * 是好友就用好友身份发，是（指定的）群友就用群友身份发，都不是就用陌生人身份发
     *
     * @param qqNum    收信人qq号码
     * @param groupNum 群号
     * @param message  消息内容
     */
    public void sendMessage(Long qqNum, Long groupNum, String message) {
        User friend = bot.getFriend(qqNum);
        if (friend == null && groupNum != null) {
            Group group = bot.getGroup(groupNum);
            if (group == null) {
                log.error("指定的群组{}不存在！", groupNum);
                throw new RuntimeException("指定的群组不存在！");
            }
            friend = group.get(qqNum);
        }

        if (friend == null) {
            friend = bot.getStranger(qqNum);
        }

        if (friend == null) {
            log.error("未获取到用户对象！");
            throw new BusException("未获取到用户对象！");
        } else {
            friend.sendMessage(message);
            log.info("成功向{}发送消息：{}", qqNum, message);
        }
    }

    /**
     * 分享url
     *
     * @param type      0-好友，1-群组
     * @param friendNum 好友的qq号或群号
     * @param url       需要分享的url
     * @param title     标题
     * @param content   内容
     * @param coverUrl  封面图
     */
    public void shareUrl(String type, Long friendNum, String url, String title, String content, String coverUrl) {
        AudioSupported audioSupported = "0".equals(type) ? bot.getFriend(friendNum) : bot.getGroup(friendNum);
        if (audioSupported == null) {
            throw new BusException("未获取到指定好友或群组！");
        }
        ServiceMessage share = RichMessage.share(url, title, content, coverUrl);
        audioSupported.sendMessage(share);
    }
}
