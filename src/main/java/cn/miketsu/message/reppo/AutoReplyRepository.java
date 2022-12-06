package cn.miketsu.message.reppo;

import cn.miketsu.message.domain.AutoReply;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author sihuangwlp
 * @date 2022/11/7
 */
@Repository
public interface AutoReplyRepository extends MongoRepository<AutoReply,Long> {

    List<AutoReply> findAllByQqNum(Long qqNum);

    void deleteAllByQqNum(Long qqNum);
}
