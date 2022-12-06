package cn.miketsu.message.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 指令激活方式
 * @author wangzefeng
 * @date 2022/12/5
 */
@Getter
@AllArgsConstructor
public enum ActivationType {
    SINGLE("0", "私聊"),
    GROUP_AT("1", "群聊+艾特"),
    /**
     * 此类型较为特殊，为避免不必要的指令激活：
     * 1、指令标识和收到的消息必须完全匹配；
     * 2、处理方法在不需要回复消息时应返回null；
     * 3、若出现越权，不会回复任何消息。
     */
    GROUP_NO_AT("2", "群聊+不用艾特");

    private String type;

    private String name;
}
