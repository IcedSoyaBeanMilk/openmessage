package cn.miketsu.message.service;

import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.RandomUtil;
import cn.miketsu.message.domain.AutoReply;
import cn.miketsu.message.enums.ActivationType;
import cn.miketsu.message.enums.UserRole;
import cn.miketsu.message.reppo.AutoReplyRepository;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.Face;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 被艾特时，若未触发其他指令，则回复问候语句
 *
 * @author sihuangwlp
 * @date 2022/11/26
 */
@Service
@Slf4j
public class SayHelloOnAtIngroupService implements CommandService {

    @Autowired
    AutoReplyRepository autoReplyRepository;

    WeightRandom<MessageChain> commonReply;

    {
        WeightRandom.WeightObj<MessageChain> weightObj1 = new WeightRandom.WeightObj<>(MessageUtils.newChain(new Face(Face.WANG_WANG), new Face(Face.WANG_WANG), new Face(Face.WANG_WANG)), 7);
        WeightRandom.WeightObj<MessageChain> weightObj2 = new WeightRandom.WeightObj<>(MessageUtils.newChain(new PlainText("你不对劲")), 3);
        commonReply = RandomUtil.weightRandom(Arrays.asList(weightObj1, weightObj2));
    }

    @Override
    public ActivationType getActivationType() {
        return ActivationType.GROUP_AT;
    }

    @Override
    public List<UserRole> jurisdiction() {
        return Arrays.asList(UserRole.OWNER, UserRole.ADMIN, UserRole.MEMBER);
    }

    @Override
    public String commandFlag() {
        return "";
    }

    @Override
    public MessageChain commandProcess(MessageChain eventMessage, User sender) {
        List<AutoReply> allReply = autoReplyRepository.findAllByQqNum(sender.getId());
        List<WeightRandom.WeightObj<MessageChain>> options = new ArrayList<>();
        WeightRandom<MessageChain> temp = commonReply;
        if (allReply != null && !allReply.isEmpty()) {
            allReply.forEach(o -> {
                WeightRandom.WeightObj<MessageChain> weightObj = new WeightRandom.WeightObj<>(MessageChain.deserializeFromJsonString(o.getReplyContent()), o.getWeight());
                options.add(weightObj);
            });
            temp = RandomUtil.weightRandom(options);
        }
        return temp.next();
    }
}
