package modules.txt2img;

import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.UserEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class Txt2ImgCallTracker {

    public static int getCalls(EntityManagerWrapper entityManager, long userId) {
        UserEntity user = entityManager.findOrDefault(UserEntity.class, String.valueOf(userId));
        resetCallsOnNewWeek(user);
        return user.getTxt2ImgCalls();
    }

    public static void increaseCalls(EntityManagerWrapper entityManager, long userId, int images) {
        UserEntity user = entityManager.findOrDefault(UserEntity.class, String.valueOf(userId));
        resetCallsOnNewWeek(user);
        user.beginTransaction();
        user.setTxt2ImgCalls(user.getTxt2ImgCalls() + images);
        user.setTxt2ImgCallsDate(LocalDate.now());
        user.commitTransaction();
    }

    private static void resetCallsOnNewWeek(UserEntity user) {
        if (user.getTxt2ImgCallsDate() != null && LocalDate.now().with(DayOfWeek.MONDAY).isAfter(user.getTxt2ImgCallsDate())) {
            user.beginTransaction();
            user.setTxt2ImgCalls(0);
            user.setTxt2ImgCallsDate(LocalDate.now());
            user.commitTransaction();
        }
    }

}
