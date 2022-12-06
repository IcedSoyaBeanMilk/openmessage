package cn.miketsu.message.reppo;

import cn.miketsu.message.domain.MessageStorage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author sihuangwlp
 * @date 2022/11/7
 */
@Repository
public interface MessageStorageRepository extends MongoRepository<MessageStorage,String> {

}
