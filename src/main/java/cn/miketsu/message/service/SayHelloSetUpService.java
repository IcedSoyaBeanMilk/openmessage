package cn.miketsu.message.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.miketsu.common.exception.BusException;
import cn.miketsu.message.domain.AutoReply;
import cn.miketsu.message.domain.SequenceId;
import cn.miketsu.message.enums.ActivationType;
import cn.miketsu.message.enums.UserRole;
import cn.miketsu.message.reppo.AutoReplyRepository;
import cn.miketsu.message.reppo.MiraiUserRepository;
import cn.miketsu.message.reppo.SequenceIdRepository;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 设定自动问候语句
 *
 * @link cn.miketsu.message.service.SayHelloOnAtIngroupService
 * @author sihuangwlp
 * @date 2022/11/26
 */
@Service
@Slf4j
public class SayHelloSetUpService implements CommandService {

    public static final String Separator = " ";
    @Autowired
    MiraiUserRepository miraiUserRepository;

    @Autowired
    SequenceIdRepository sequenceIdRepository;

    @Autowired
    AutoReplyRepository autoReplyRepository;

    @Override
    public ActivationType getActivationType() {
        return ActivationType.SINGLE;
    }

    @Override
    public List<UserRole> jurisdiction() {
        return Arrays.asList(UserRole.OWNER, UserRole.ADMIN);
    }

    @Override
    public String commandFlag() {
        return "自动回复";
    }

    @Override
    public MessageChain commandProcess(MessageChain eventMessage, User sender) {
        String command = MessageChain.serializeToJsonString(eventMessage);
        if (StrUtil.isBlank(command) || eventMessage.isEmpty()) {
            log.error("命令字符串为空！");
            return MessageUtils.newChain(new PlainText("命令字符串为空"));
        }
        if (!(eventMessage.get(0) instanceof PlainText)) {
            log.error("命令识别或执行错误！");
            return MessageUtils.newChain(new PlainText("命令识别或执行错误"));
        }
        command = ((PlainText) eventMessage.get(0)).toString();
        List<String> split = StrUtil.split(command, Separator);

        List<SingleMessage> list = new ArrayList<>(eventMessage);
        list.remove(0);
        list.addAll(0, split.stream().map(PlainText::new).toList());
        log.info("解析后消息链：{}", MessageChain.serializeToJsonString(MessageUtils.newChain(list)));


        try {
            return switch (split.get(0)) {
                case "自动回复设置" -> autoReplyInsert(list);
                case "自动回复删除" -> autoReplyDelete(list);
                case "自动回复展示" -> autoReplyShow(list);
                case "自动回复执行" -> autoReplyExcute(list);
                default -> MessageUtils.newChain(new PlainText("无法识别的命令！"));
            };
        } catch (Exception e) {
            log.error("命令识别或执行错误！", e);
            return MessageUtils.newChain(new PlainText("命令识别或执行错误！报错信息："), new PlainText(e.getMessage()));
        }
    }

    /**
     * 插入新的自动回复设定
     * 示例：自动回复设置 1069567773 1 你好，主人！
     *
     * @param commands
     * @return
     */
    public MessageChain autoReplyInsert(List<SingleMessage> commands) {
        AutoReply autoReply = new AutoReply();
        autoReply.setId(sequenceId("auto_reply_sequence"));
        autoReply.setQqNum(Long.valueOf(commands.get(1).contentToString()));
        autoReply.setWeight(Integer.valueOf(commands.get(2).contentToString()));

        commands.remove(0);
        commands.remove(0);
        commands.remove(0);

        MessageChain messageChain = MessageUtils.newChain(commands);
        autoReply.setReplyContent(MessageChain.serializeToJsonString(messageChain));

        Date createTime = new Date();
        autoReply.setCreateTime(createTime);
        autoReply.setUpdateTime(createTime);

        autoReplyRepository.save(autoReply);

        return MessageUtils.newChain(new PlainText("自动回复设定成功！"));
    }

    /**
     * 自动回复删除
     * 示例：
     * 自动回复删除 用户 1069567773
     * 或
     * 自动回复删除 ID 123
     *
     * @param commands
     * @return
     */
    public MessageChain autoReplyDelete(List<SingleMessage> commands) {
        if (commands.size() < 3) {
            log.error("参数数目不正确！");
            throw new BusException("参数数目不正确！该命令要求您必须指定需要删除的自动回复设定ID或生效人！");
        }
        if ("用户".equals(commands.get(1).contentToString())) {
            autoReplyRepository.deleteAllByQqNum(Long.valueOf(commands.get(2).contentToString()));
        } else if ("ID".equals(commands.get(1).contentToString())) {
            autoReplyRepository.deleteById(Long.valueOf(commands.get(2).contentToString()));
        }

        return MessageUtils.newChain(new PlainText("自动回复删除成功！"));
    }

    /**
     * 自动回复执行
     * 示例：
     * 自动回复删除 123(该参数为自动回复设置的ID)
     *
     * @param commands
     * @return
     */
    public MessageChain autoReplyExcute(List<SingleMessage> commands) {
        if (commands.size() < 2) {
            log.error("参数数目不正确！");
            throw new BusException("参数数目不正确！该命令要求您必须指定需要执行的自动回复设定ID！");
        }
        Optional<AutoReply> autoReply = autoReplyRepository.findById(Long.valueOf(commands.get(1).contentToString()));

        return autoReply.map(reply -> MessageChain.deserializeFromJsonString(reply.getReplyContent())).orElseGet(() -> MessageUtils.newChain(new PlainText("未获取到指定自动回复设定！")));
    }

    /**
     * 展示指定或所有自动回复设置
     * 示例：自动回复展示 (1069567773，该参数可不传，不传时返回全部设定)
     *
     * @param commands
     * @return
     */
    public MessageChain autoReplyShow(List<SingleMessage> commands) {
        List<AutoReply> autoReplies;
        if (commands.size() < 2 || StrUtil.isBlank(commands.get(1).contentToString())) {
            autoReplies = autoReplyRepository.findAll();
        } else {
            autoReplies = autoReplyRepository.findAllByQqNum(Long.parseLong(commands.get(1).contentToString()));
        }
        return MessageUtils.newChain(new PlainText(JSONUtil.toJsonPrettyStr(autoReplies)));
    }

    /**
     * 从序列获取一个ID
     *
     * @param id
     * @return
     */
    public synchronized long sequenceId(String id) {
        Optional<SequenceId> optionalSequenceId = sequenceIdRepository.findById(id);
        if (optionalSequenceId.isEmpty()) {
            SequenceId sequenceId = new SequenceId();
            sequenceId.setId(id);
            sequenceId.setCollName("auto_create");
            sequenceId.setSeqId(1);
            sequenceIdRepository.save(sequenceId);
            return 1;
        }
        SequenceId sequenceId = optionalSequenceId.get();
        long seqId = sequenceId.getSeqId();
        sequenceId.setSeqId(seqId + 1);
        sequenceIdRepository.save(sequenceId);

        return seqId;
    }
}
