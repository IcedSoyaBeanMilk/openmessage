package cn.miketsu.message.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 自动回复配置表
 * @TableName auto_reply
 */
@Data
@Document(collection = "auto_reply")
public class AutoReply {
    /**
     * 
     */
    @Id
    private Long id;

    /**
     * QQ号
     */
    private Long qqNum;

    /**
     * 回复内容
     */
    private String replyContent;

    /**
     * 权重
     */
    private Integer weight;

    private Date createTime;

    private Date updateTime;
}