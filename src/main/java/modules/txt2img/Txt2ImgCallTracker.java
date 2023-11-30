package modules.txt2img;

import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.user.Txt2ImgEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class Txt2ImgCallTracker {

    public static int getCalls(EntityManagerWrapper entityManager, long userId) {
        Txt2ImgEntity txt2img = entityManager.findUserEntity(userId).getTxt2img();
        resetCallsOnNewWeek(txt2img);
        return txt2img.getCalls();
    }

    public static void increaseCalls(EntityManagerWrapper entityManager, long userId, int images) {
        Txt2ImgEntity txt2img = entityManager.findUserEntity(userId).getTxt2img();

        resetCallsOnNewWeek(txt2img);
        txt2img.beginTransaction();
        txt2img.setCalls(txt2img.getCalls() + images);
        txt2img.setCallsDate(LocalDate.now());
        txt2img.commitTransaction();
    }

    private static void resetCallsOnNewWeek(Txt2ImgEntity txt2img) {
        if (txt2img.getCallsDate() != null && LocalDate.now().with(DayOfWeek.MONDAY).isAfter(txt2img.getCallsDate())) {
            txt2img.beginTransaction();
            txt2img.setCalls(0);
            txt2img.setCallsDate(LocalDate.now());
            txt2img.commitTransaction();
        }
    }

}
