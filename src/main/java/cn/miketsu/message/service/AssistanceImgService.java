package cn.miketsu.message.service;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.miketsu.message.enums.ActivationType;
import cn.miketsu.message.enums.UserRole;
import cn.miketsu.message.listener.CommandListener;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author sihuangwlp
 * @date 2022/12/5
 */
@Service
@Slf4j
public class AssistanceImgService implements CommandService {

    private static final String workplace = System.getProperty("user.dir") + "/skynet-message";

    /**
     * 模板路径
     */
    private static final String templatePath = System.getProperty("user.dir") + "/skynet-message/template.jpg";

    /**
     * 头像缓存目录
     */
    private static final File avatarCache;

    /**
     * 临时文件目录
     */
    private static final File temp;

    /**
     * 应援图输出目录
     */
    private static final File assistanceImg;

    static {
        System.out.println(workplace);

        avatarCache = new File(workplace + "/avatarCache/");
        if (!avatarCache.exists()) {
            avatarCache.mkdirs();
        }

        temp = new File(workplace + "/temp/");
        if (!temp.exists()) {
            temp.mkdirs();
        }

        assistanceImg = new File(workplace + "/assistanceImg/");
        if (!assistanceImg.exists()) {
            assistanceImg.mkdirs();
        }
    }

    @Override
    public ActivationType getActivationType() {
        return ActivationType.GROUP_NO_AT;
    }

    @Override
    public List<UserRole> jurisdiction() {
        return Arrays.asList(UserRole.values());
    }

    @Override
    public String commandFlag() {
        return "应援图生成";
    }

    @Override
    public synchronized MessageChain commandProcess(MessageChain eventMessage, User sender) {
        try {
            //该命令允许带参
            if (eventMessage.size() > 1) {
                //参数可以是at
                if (eventMessage.get(1) instanceof At at) {
                    groups:
                    for (Long effectGroup : CommandListener.effectGroups) {
                        Group group = sender.getBot().getGroup(effectGroup);
                        if (group == null) {
                            continue;
                        }
                        for (NormalMember member : group.getMembers()) {
                            if (member.getId() == at.getTarget()) {
                                sender = member;
                                break groups;
                            }
                        }
                    }
                }
            }

            //下载头像
            String filename = sender.getAvatarUrl().substring(sender.getAvatarUrl().lastIndexOf('/') + 1);
            if (filename.contains("?")) {
                filename = filename.substring(0, filename.lastIndexOf("?"));
            }
            HttpUtil.downloadFile(sender.getAvatarUrl(), FileUtil.file(avatarCache.getAbsolutePath()));

            //重命名下载后的文件
            FileUtil.rename(new File(avatarCache.getAbsolutePath() + File.separator + filename), sender.getId() + ".jpg", true);

            //制作应援图
            String imgMaker = imgMaker(avatarCache.getAbsolutePath() + File.separator + sender.getId() + ".jpg", sender.getId());

            //上传图片以便发送
            net.mamoe.mirai.message.data.Image image = Contact.uploadImage(sender, new File(imgMaker));

            return MessageUtils.newChain(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String imgMaker(String avatar, Long senderId) throws IOException {
        String fileExt = avatar.substring(avatar.lastIndexOf("."));

        //将图像裁切成标准正方形
        BufferedImage read = ImageIO.read(FileUtil.file(avatar));
        if (read.getWidth() != read.getHeight()) {
            String tempPath2 = temp.getAbsolutePath() + File.separator + IdUtil.simpleUUID() + fileExt;
            int sideLength = Math.min(read.getWidth(), read.getHeight());
            ImgUtil.cut(FileUtil.file(avatar), FileUtil.file(tempPath2), new Rectangle(0, 0, sideLength, sideLength));
            avatar = tempPath2;
        }

        //旋转头像
        String tempPath = temp.getAbsolutePath() + File.separator + IdUtil.simpleUUID() + fileExt;
        BufferedImage rotate = rotate(ImageIO.read(FileUtil.file(avatar)), 6);
        ImgUtil.write(rotate, FileUtil.file(tempPath));

        //缩放头像
        BufferedImage bimg = ImageIO.read(FileUtil.file(tempPath));
        String tempPath3 = temp.getAbsolutePath() + File.separator + IdUtil.simpleUUID() + fileExt;
        ImgUtil.scale(
                FileUtil.file(tempPath),
                FileUtil.file(tempPath3),
                135f / (float) bimg.getWidth()//缩放比例
        );

        //获取图像的ARGB矩阵
        int[][] template = getData(templatePath);
        int[][] temp3 = getData(tempPath3);

        //把头像拼上去
        BufferedImage bufferedImage = new BufferedImage(template[0].length, template.length, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < template.length; i++) {
            for (int j = 0; j < template[i].length; j++) {
                if (170 <= j && j < 305 && 15 <= i && i < 150) {
                    bufferedImage.setRGB(j, i, temp3[i - 15][j - 170]);
                } else {
                    bufferedImage.setRGB(j, i, template[i][j]);
                }
            }
        }

        //输出应援图
        File targetFile = new File(assistanceImg.getAbsolutePath() + File.separator + senderId + fileExt);
        ImgUtil.write(bufferedImage, targetFile);
        return targetFile.getAbsolutePath();
    }

    private int[][] getData(String path) throws IOException {
        BufferedImage bimg = ImageIO.read(new File(path));
        int[][] data = new int[bimg.getHeight()][bimg.getWidth()];
        for (int i = 0; i < bimg.getHeight(); i++) {
            for (int j = 0; j < bimg.getWidth(); j++) {
                data[i][j] = bimg.getRGB(j, i);
            }
        }
        return data;
    }

    private BufferedImage rotate(Image src, int angel) {
        int src_width = src.getWidth(null);
        int src_height = src.getHeight(null);
        // calculate the new image size
        Rectangle rect_des = CalcRotatedSize(new Rectangle(new Dimension(
                src_width, src_height)), angel);

        BufferedImage res = null;
        res = new BufferedImage(rect_des.width, rect_des.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = res.createGraphics();
        //用于跟换背景色
        Graphics2D g3 = res.createGraphics();
        // transform
        g2.translate((rect_des.width - src_width) / 2,
                (rect_des.height - src_height) / 2);
        g2.rotate(Math.toRadians(angel), src_width / 2, src_height / 2);
        //设置画笔颜色为白色不设置怎为黑色
        g3.setColor(Color.WHITE);
        //填充背景色
        g3.fill(rect_des);
        g2.drawImage(src, null, null);
        return res;
    }

    private Rectangle CalcRotatedSize(Rectangle src, int angel) {
        // if angel is greater than 90 degree, we need to do some conversion
        if (angel >= 90) {
            if (angel / 90 % 2 == 1) {
                int temp = src.height;
                src.height = src.width;
                src.width = temp;
            }
            angel = angel % 90;
        }

        double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
        double len = 2 * Math.sin(Math.toRadians(angel) / 2) * r;
        double angel_alpha = (Math.PI - Math.toRadians(angel)) / 2;
        double angel_dalta_width = Math.atan((double) src.height / src.width);
        double angel_dalta_height = Math.atan((double) src.width / src.height);

        int len_dalta_width = (int) (len * Math.cos(Math.PI - angel_alpha
                - angel_dalta_width));
        int len_dalta_height = (int) (len * Math.cos(Math.PI - angel_alpha
                - angel_dalta_height));
        int des_width = src.width + len_dalta_width * 2;
        int des_height = src.height + len_dalta_height * 2;
        return new java.awt.Rectangle(new Dimension(des_width, des_height));
    }

}
