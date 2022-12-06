package cn.miketsu.message.enums;

import cn.miketsu.common.exception.BusException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author sihuangwlp
 * @date 2022/11/26
 */
@Getter
@AllArgsConstructor
public enum UserRole {
    OWNER("0", "拥有者"), ADMIN("1", "管理员"), MEMBER("2", "成员");

    private final String type;

    private final String name;

    public static UserRole getByType(String type) {
        for (UserRole userRole : values()) {
            if (type.equals(userRole.getType())) {
                return userRole;
            }
        }
        throw new BusException("未获取到对应的角色枚举！");
    }
}
