package modules.txt2img;

import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.UserEntity;

import java.time.LocalDate;

public class Txt2ImgCallTracker {

    public static int getCalls(EntityManagerWrapper entityManager, long userId) {
        UserEntity user = entityManager.findOrDefault(UserEntity.class, String.valueOf(userId));
        resetCallsOnNewDay(user);
        return user.getTxt2ImgCalls();
    }

    public static void increaseCalls(EntityManagerWrapper entityManager, long userId) {
        UserEntity user = entityManager.findOrDefault(UserEntity.class, String.valueOf(userId));
        resetCallsOnNewDay(user);
        user.beginTransaction();
        user.setTxt2ImgCalls(user.getTxt2ImgCalls() + 1);
        user.setTxt2ImgCallsDate(LocalDate.now());
        user.commitTransaction();
    }

    private static void resetCallsOnNewDay(UserEntity user) {
        if (user.getTxt2ImgCallsDate() != null && LocalDate.now().isAfter(user.getTxt2ImgCallsDate())) {
            user.beginTransaction();
            user.setTxt2ImgCalls(0);
            user.setTxt2ImgCallsDate(LocalDate.now());
            user.commitTransaction();
        }
    }

}
