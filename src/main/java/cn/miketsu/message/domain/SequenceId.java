package cn.miketsu.message.domain;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.annotation.Id;

/**
 * @author sihuangwlp
 * @date 2022/11/26
 */
@Document(collection = "sequence")
@Data
public class SequenceId {
    @Id
    private String id;            //主键

    private String collName;    //集合名称

    private long seqId;            //序列值

}