package cn.miketsu.message.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author sihuangwlp
 * @date 2022/11/26
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(collection = "mirai_user")
public class MiraiUser {

    @Id
    private Long qqNum;

    private String role;

    private Date createTime;

    private Date updateTime;

}
