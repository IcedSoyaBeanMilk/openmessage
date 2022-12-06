package cn.miketsu.message.listener;

import cn.hutool.core.util.StrUtil;
import cn.miketsu.message.domain.MiraiUser;
import cn.miketsu.message.enums.ActivationType;
import cn.miketsu.message.enums.UserRole;
import cn.miketsu.message.reppo.MiraiUserRepository;
import cn.miketsu.message.service.CommandService;
import cn.miketsu.message.service.QQService;
import kotlin.coroutines.CoroutineContext;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupAwareMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author sihuangwlp
 * @date 2022/11/27
 */
@Slf4j
@Component
public class CommandListener extends SimpleListenerHost {

    public static Long[] effectGroups = {692286307L, 659036449L, 1011524751L, 280858893L};

    @Autowired
    List<CommandService> commandServices;

    @Autowired
    MiraiUserRepository miraiUserRepository;

    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
        exception.printStackTrace();
    }

    /**
     * 私聊指令监听器
     *
     * @param event
     */
    @EventHandler
    public void opCommand(@NotNull FriendMessageEvent event) {
        List<SingleMessage> temp = new ArrayList<>(event.getMessage());
        temp.remove(0);
        SingleMessage singleMessage = temp.get(0);
        String flag;
        if (singleMessage instanceof PlainText) {
            flag = ((PlainText) singleMessage).getContent();
        } else {
            return;
        }

        for (CommandService commandService : commandServices) {
            if (flag.startsWith(commandService.commandFlag()) && ActivationType.SINGLE.equals(commandService.getActivationType())) {
                commandExcute(temp, event.getSender(), commandService, event.getSender());
            }
        }
    }

    /**
     * 消息接收监听器
     * 用于处理被艾特时的消息
     *
     * @param event
     */
    @EventHandler
    public void onAtInGroup(@NotNull GroupAwareMessageEvent event) {
        //获取发送人QQ号和群号
        long senderId = event.getSender().getId();
        Group group = event.getGroup();
        //只在指定群生效
        if (!Arrays.asList(effectGroups).contains(group.getId())) {
            return;
        }

        //判断消息中是否有艾特我
        List<SingleMessage> singleMessageList = new ArrayList<>(event.getMessage());
        boolean atMe = true;
        for (SingleMessage message : singleMessageList) {
            if (message.contentEquals("@" + QQService.qqNum, true)) {
                atMe = false;
                singleMessageList.remove(message);
                break;
            }
        }
        //如果没有，走ActivationType.GROUP_NO_AT
        singleMessageList.remove(0);
        if (atMe) {
            for (CommandService commandService : commandServices) {
                if (singleMessageList.get(0).contentToString().equals(commandService.commandFlag())
                        && ActivationType.GROUP_NO_AT.equals(commandService.getActivationType())) {
                    if (!commandService.jurisdiction().contains(getSenderRole(senderId))) {
                        log.warn("当前用户{}越权！", senderId);
                        return;
                    }
                    MessageChain eventMessage = MessageUtils.newChain(singleMessageList);
                    String command = MessageChain.serializeToJsonString(eventMessage);
                    log.info("收到指令：{}", command);
                    MessageChain messageReturn = commandService.commandProcess(eventMessage, event.getSender());
                    if (messageReturn != null) {
                        group.sendMessage(messageReturn);
                    }
                    return;
                }
            }
            return;
        }

        //如果有，继续获取指令标识
        SingleMessage singleMessage = singleMessageList.isEmpty() ? new PlainText("") : singleMessageList.get(0);
        String flag;
        if (singleMessage instanceof PlainText) {
            flag = ((PlainText) singleMessage).getContent();
        } else {
            return;
        }

        //新建一个临时变量用于保存指令表示为空字符串的指令服务类。只有遍历完毕后仍未匹配到具有非空指令标识的服务类时才执行。
        CommandService temp = null;
        for (CommandService commandService : commandServices) {
            if (StrUtil.isBlank(commandService.commandFlag())) {
                temp = commandService;
                continue;
            }
            //若匹配到具有非空指令标识的服务类，则执行
            if (flag.startsWith(commandService.commandFlag()) && ActivationType.GROUP_AT.equals(commandService.getActivationType())) {
                commandExcute(singleMessageList, event.getSender(), commandService, group);
                return;
            }
        }
        if (temp != null) {
            commandExcute(singleMessageList, event.getSender(), temp, group);
        }
    }

    /**
     * 执行指令（含鉴权）
     *
     * @param singleMessageList 消息链
     * @param sender            发送者的ID
     * @param commandService    指令处理服务类
     * @param replyer           发送者
     */
    private void commandExcute(List<SingleMessage> singleMessageList, User sender, CommandService commandService, Contact replyer) {
        //获取发送人角色用于鉴权
        UserRole senderRole = getSenderRole(sender.getId());
        if (!commandService.jurisdiction().contains(senderRole)) {
            log.warn("当前用户{}越权！", sender.getId());
            replyer.sendMessage("您不具备使用该命令的权限！");
            return;
        }
        MessageChain eventMessage = MessageUtils.newChain(singleMessageList);
        String command = MessageChain.serializeToJsonString(eventMessage);
        log.info("收到指令：{}", command);
        MessageChain messageReturn = commandService.commandProcess(eventMessage, sender);
        if (messageReturn != null) {
            replyer.sendMessage(messageReturn);
        }
    }

    /**
     * 获取用户的角色
     *
     * @param qqNum
     * @return
     */
    public UserRole getSenderRole(Long qqNum) {
        Optional<MiraiUser> user = miraiUserRepository.findById(qqNum);
        if (user.isEmpty() && qqNum == QQService.ownerNum) {
            Date createTime = new Date();
            miraiUserRepository.save(new MiraiUser(QQService.ownerNum, UserRole.OWNER.getType(), createTime, createTime));
            user = miraiUserRepository.findById(qqNum);
        }
        return user.map(miraiUser -> UserRole.getByType(miraiUser.getRole())).orElse(UserRole.MEMBER);
    }
}
