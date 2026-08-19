package modules.txt2img;

import commands.Category;
import commands.runnables.nsfwcategory.Txt2HentaiCommand;
import core.TextManager;
import core.utils.StringUtil;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.user.Txt2ImgEntity;
import mysql.hibernate.entity.user.UserEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

public class Txt2ImgCallTracker {

    public static int getRemainingCalls(EntityManagerWrapper entityManager, long userId) {
        if (Instant.now().isAfter(Txt2HentaiCommand.SHUTDOWN_TIME)) {
            return 0;
        }

        return entityManager.findUserEntityReadOnly(userId).getTxt2img().getBoughtImages();
    }

    public static void increaseCalls(EntityManagerWrapper entityManager, long userId, int images) {
        Txt2ImgEntity txt2img = entityManager.findUserEntity(userId).getTxt2img();

        txt2img.beginTransaction();
        txt2img.setCallsDate(LocalDate.now());

        int boughtCalls = Math.min(images, txt2img.getBoughtImages());
        txt2img.setBoughtImages(txt2img.getBoughtImages() - boughtCalls);
        images -= boughtCalls;

        if (images != 0) {
            txt2img.getEntityManager().getTransaction().rollback();
            throw new RuntimeException("Could not increase calls for txt2img commands, \"images\" is " + images);
        }

        txt2img.commitTransaction();
    }

    public static String getRemainingImagesText(Locale locale, UserEntity userEntity) {
        return TextManager.getString(locale, Category.NSFW, "txt2hentai_root_footer",
                StringUtil.numToString(getRemainingCalls(userEntity.entityManager, userEntity.getUserId()))
        );
    }

}
