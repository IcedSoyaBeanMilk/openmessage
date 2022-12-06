package cn.miketsu.message.reppo;

import cn.miketsu.message.domain.AutoReply;
import cn.miketsu.message.domain.MiraiUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author sihuangwlp
 * @date 2022/11/7
 */
@Repository
public interface MiraiUserRepository extends MongoRepository<MiraiUser,Long> {

}
