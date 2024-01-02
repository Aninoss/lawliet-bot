package modules.txt2img;

import commands.runnables.RunPodAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.user.Txt2ImgEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class Txt2ImgCallTracker {

    public static int getRemainingCalls(EntityManagerWrapper entityManager, long userId, boolean premium) {
        if (premium) {
            return RunPodAbstract.LIMIT_CREATIONS_PER_WEEK - getPremiumCalls(entityManager, userId) +
                    entityManager.findUserEntityReadOnly(userId).getTxt2img().getBoughtImages();
        } else {
            return entityManager.findUserEntityReadOnly(userId).getTxt2img().getBoughtImages();
        }
    }

    public static int getPremiumCalls(EntityManagerWrapper entityManager, long userId) {
        Txt2ImgEntity txt2img = entityManager.findUserEntity(userId).getTxt2img();
        resetPremiumCallsOnNewWeek(txt2img);
        return txt2img.getCalls();
    }

    public static void increaseCalls(EntityManagerWrapper entityManager, long userId, boolean premium, int images) {
        Txt2ImgEntity txt2img = entityManager.findUserEntity(userId).getTxt2img();

        resetPremiumCallsOnNewWeek(txt2img);
        txt2img.beginTransaction();
        txt2img.setCallsDate(LocalDate.now());

        if (premium) {
            int premiumCalls = Math.min(images, RunPodAbstract.LIMIT_CREATIONS_PER_WEEK - txt2img.getCalls());
            txt2img.setCalls(txt2img.getCalls() + premiumCalls);
            images -= premiumCalls;
        }

        int boughtCalls = Math.min(images, txt2img.getBoughtImages());
        txt2img.setBoughtImages(txt2img.getBoughtImages() - boughtCalls);
        images -= boughtCalls;

        if (images != 0) {
            txt2img.getEntityManager().getTransaction().rollback();
            throw new RuntimeException("Could not increase calls for txt2img commands, \"images\" is " + images);
        }

        txt2img.commitTransaction();
    }

    private static void resetPremiumCallsOnNewWeek(Txt2ImgEntity txt2img) {
        if (txt2img.getCallsDate() != null && txt2img.getCalls() > 0 && LocalDate.now().with(DayOfWeek.MONDAY).isAfter(txt2img.getCallsDate())) {
            txt2img.beginTransaction();
            txt2img.setCalls(0);
            txt2img.commitTransaction();
        }
    }

}
