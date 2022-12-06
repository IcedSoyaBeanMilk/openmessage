package cn.miketsu.message.service;

import cn.miketsu.message.enums.ActivationType;
import cn.miketsu.message.enums.UserRole;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;

/**
 * @author wangzefeng
 * @date 2022/12/5
 */
public interface CommandService {

    /**
     * 激活方式
     * @return
     */
    ActivationType getActivationType();

    /**
     * 权限
     * @return
     */
    List<UserRole> jurisdiction();

    /**
     * 识别标识
     * @return
     */
    String commandFlag();

    MessageChain commandProcess(MessageChain eventMessage, User sender);
}
