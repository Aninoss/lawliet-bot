package events.scheduleevents.events;

import constants.ExceptionRunnable;
import constants.Settings;
import core.MainLogger;
import core.ShardManager;
import events.scheduleevents.ScheduleEventHourly;
import modules.fishery.Stock;
import modules.fishery.StockMarket;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.AutoStockActivityEntity;
import mysql.hibernate.entity.user.AutoStockOrderEntity;
import mysql.hibernate.entity.user.UserEntity;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryMemberStocksData;
import mysql.redis.fisheryusers.FisheryUserManager;

import java.util.*;
import java.util.stream.Collectors;

@ScheduleEventHourly
public class AutoStocks implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() {
        int actions = 0;

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(AutoStocks.class)) {
            entityManager.getTransaction().begin();

            List<UserEntity> userEntities = AutoStockOrderEntity.findAllUserEntitiesWithAutoStockOrders(entityManager);
            for (UserEntity userEntity : userEntities) {
                userEntity.cleanAutoStockActivities();
                HashMap<Stock, Long> currentBuyNumber = new HashMap<>();
                HashMap<Stock, Long> currentSellNumber = new HashMap<>();

                checkBuyOrders(userEntity, currentBuyNumber);
                checkSellOrders(userEntity, currentSellNumber);
                if (currentBuyNumber.isEmpty() && currentSellNumber.isEmpty()) {
                    continue;
                }
                actions++;

                Set<Long> guildIds = FisheryUserManager.getGuildIdsByUserId(userEntity.getUserId(), false).stream()
                        .filter(ShardManager::guildIsManaged)
                        .collect(Collectors.toSet());
                if (guildIds.isEmpty()) {
                    continue;
                }

                buyShares(guildIds, userEntity, currentBuyNumber);
                sellShares(guildIds, userEntity, currentSellNumber);
            }

            entityManager.getTransaction().commit();
        }

        MainLogger.get().info("Auto Stocks - {} Actions", actions);
    }

    private static void checkBuyOrders(UserEntity userEntity, HashMap<Stock, Long> currentBuyOrders) {
        for (Map.Entry<Stock, AutoStockOrderEntity> entry : new ArrayList<>(userEntity.getAutoStocksBuyOrders().entrySet())) {
            Stock stock = entry.getKey();
            AutoStockOrderEntity order = entry.getValue();
            long stockValue = StockMarket.getValue(stock);

            if (order.getActive() && stockValue <= order.getOrderThreshold()) {
                currentBuyOrders.put(stock, order.getShares());
                userEntity.getAutoStockActivities().add(new AutoStockActivityEntity(AutoStockActivityEntity.Type.BUY, stock, order.getShares(), stockValue));
                if (order.getReactivationThreshold() != null) {
                    order.setActive(false);
                } else {
                    userEntity.getAutoStocksBuyOrders().remove(stock);
                }
            }
            if (!order.getActive() && order.getReactivationThreshold() != null && StockMarket.getValue(stock) >= order.getReactivationThreshold()) {
                order.setActive(true);
                userEntity.getAutoStockActivities().add(new AutoStockActivityEntity(AutoStockActivityEntity.Type.BUY_REACTIVATION, stock, order.getShares(), stockValue));
            }
        }
    }

    private static void checkSellOrders(UserEntity userEntity, HashMap<Stock, Long> currentSellOrders) {
        for (Map.Entry<Stock, AutoStockOrderEntity> entry : new ArrayList<>(userEntity.getAutoStocksSellOrders().entrySet())) {
            Stock stock = entry.getKey();
            AutoStockOrderEntity order = entry.getValue();
            long stockValue = StockMarket.getValue(stock);

            if (order.getActive() && stockValue >= order.getOrderThreshold()) {
                currentSellOrders.put(stock, order.getShares());
                userEntity.getAutoStockActivities().add(new AutoStockActivityEntity(AutoStockActivityEntity.Type.SELL, stock, order.getShares(), stockValue));
                if (order.getReactivationThreshold() != null) {
                    order.setActive(false);
                } else {
                    userEntity.getAutoStocksSellOrders().remove(stock);
                }
            }
            if (!order.getActive() && order.getReactivationThreshold() != null && StockMarket.getValue(stock) <= order.getReactivationThreshold()) {
                order.setActive(true);
                userEntity.getAutoStockActivities().add(new AutoStockActivityEntity(AutoStockActivityEntity.Type.SELL_REACTIVATION, stock, order.getShares(), stockValue));
            }
        }
    }

    private static void buyShares(Set<Long> guildIds, UserEntity userEntity, HashMap<Stock, Long> currentBuyNumber) {
        for (long guildId : guildIds) {
            FisheryMemberData memberData = FisheryUserManager.getGuildData(guildId).getMemberData(userEntity.getUserId());
            long coins = memberData.getCoins();

            for (Stock stock : currentBuyNumber.keySet()) {
                double stockValue = (double) StockMarket.getValue(stock) * (1.0 + Settings.FISHERY_SHARES_FEES / 100.0);
                long n = Math.min(currentBuyNumber.get(stock), (long) (coins / stockValue));
                if (n <= 0) {
                    continue;
                }

                memberData.addCoinsRaw(-Math.round(stockValue * n));
                memberData.getStocks(stock).add((int) n);
            }
        }
    }

    private static void sellShares(Set<Long> guildIds, UserEntity userEntity, HashMap<Stock, Long> currentSellNumber) {
        for (long guildId : guildIds) {
            FisheryMemberData memberData = FisheryUserManager.getGuildData(guildId).getMemberData(userEntity.getUserId());

            for (Stock stock : currentSellNumber.keySet()) {
                long stockValue = StockMarket.getValue(stock);
                FisheryMemberStocksData memberStocksData = memberData.getStocks(stock);

                long n = Math.min(currentSellNumber.get(stock), memberStocksData.getShareSize());
                if (n <= 0) {
                    continue;
                }

                memberData.addCoinsRaw(stockValue * n);
                memberStocksData.add((int) -n);
            }
        }
    }

}
