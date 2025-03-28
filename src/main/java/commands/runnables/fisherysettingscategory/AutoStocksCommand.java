package commands.runnables.fisherysettingscategory;

import com.google.common.collect.Lists;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.cache.PatreonCache;
import core.modals.LongModalBuilder;
import core.utils.StringUtil;
import modules.fishery.Stock;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.user.AutoStockActivityEntity;
import mysql.hibernate.entity.user.AutoStockOrderEntity;
import mysql.hibernate.entity.user.UserEntity;
import mysql.redis.RedisManager;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "autostocks",
        emoji = "🤖",
        usesExtEmotes = true,
        executableWithoutArgs = true,
        aliases = { "autostock", "autostonks", "autoshares", "autoshare", "autostockmarket" }
)
public class AutoStocksCommand extends NavigationAbstract {

    private enum Action {BUY, SELL}

    private static final int
            STATE_MANAGE_ORDERS = 1,
            STATE_CONFIG = 2;

    private Action action;
    private Stock currentStock = null;
    private boolean newOrder;
    private AutoStockOrderEntity currentOrder = null;

    public AutoStocksCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefaultState(ButtonInteractionEvent event, int i) {
        return switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                yield false;
            }
            case 0 -> {
                action = Action.BUY;
                setState(STATE_MANAGE_ORDERS);
                yield true;
            }
            case 1 -> {
                action = Action.SELL;
                setState(STATE_MANAGE_ORDERS);
                yield true;
            }
            default -> false;
        };
    }

    @ControllerButton(state = STATE_MANAGE_ORDERS)
    public boolean onButtonManageOrders(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_CONFIG)
    public boolean onButtonConfig(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(STATE_MANAGE_ORDERS);
                return true;
            }
            case 0 -> {
                Modal modal = new LongModalBuilder(this, getString("config_header_threshold", action == Action.SELL))
                        .setGetter(() -> currentOrder.getOrderThreshold())
                        .setSetter(value -> currentOrder.setOrderThreshold(value))
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                Modal modal = new LongModalBuilder(this, getString("config_header_reactivation_threshold"))
                        .setGetter(() -> currentOrder.getReactivationThreshold())
                        .setSetter(value -> currentOrder.setReactivationThreshold(value != 0 ? value : null))
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                Modal modal = new LongModalBuilder(this, getString("config_header_shares"))
                        .setMinMax(1, Long.MAX_VALUE - 1)
                        .setGetter(() -> currentOrder.getShares())
                        .setSetter(value -> currentOrder.setShares(value))
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 3 -> {
                if (!PatreonCache.getInstance().hasPremium(event.getUser().getIdLong(), false)) {
                    setLog(LogStatus.FAILURE, getString("premium"));
                    return true;
                }
                if (currentOrder.getReactivationThreshold() != null &&
                        ((action == Action.BUY && currentOrder.getReactivationThreshold() <= currentOrder.getOrderThreshold()) || (action == Action.SELL && currentOrder.getReactivationThreshold() >= currentOrder.getOrderThreshold()))
                ) {
                    setLog(LogStatus.FAILURE, getString("wrong_reactivation_value", action == Action.SELL));
                    return true;
                }

                EntityManagerWrapper entityManager = getEntityManager();
                entityManager.getTransaction().begin();
                getStockOrderEntities().put(currentStock, currentOrder);
                entityManager.getTransaction().commit();

                RedisManager.update(jedis -> {
                    Pipeline pipeline = jedis.pipelined();
                    FisheryUserManager.setUserActiveOnGuild(pipeline, event.getGuild().getIdLong(), event.getUser().getIdLong());
                    pipeline.sync();
                });

                setLog(LogStatus.SUCCESS, getString("submitted"));
                setState(STATE_MANAGE_ORDERS);
                return true;
            }
            case 4 -> {
                if (newOrder) {
                    return false;
                }

                EntityManagerWrapper entityManager = getEntityManager();
                entityManager.getTransaction().begin();
                getStockOrderEntities().remove(currentStock);
                entityManager.getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("deleted"));
                setState(STATE_MANAGE_ORDERS);
                return true;
            }
        }
        return false;
    }

    @ControllerStringSelectMenu(state = STATE_MANAGE_ORDERS)
    public boolean onStringSelectMenuManageOrders(StringSelectInteractionEvent event, int i) {
        Map<Stock, AutoStockOrderEntity> stockOrderEntities = getStockOrderEntities();
        currentStock = Stock.valueOf(event.getSelectedOptions().get(0).getValue());
        if (!stockOrderEntities.containsKey(currentStock) && !PatreonCache.getInstance().hasPremium(event.getUser().getIdLong(), false)) {
            setLog(LogStatus.FAILURE, getString("premium"));
            return true;
        }

        newOrder = !stockOrderEntities.containsKey(currentStock);
        currentOrder = stockOrderEntities.getOrDefault(currentStock, new AutoStockOrderEntity());
        setState(STATE_CONFIG);
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) {
        UserEntity userEntity = getUserEntityReadOnly();
        if (userEntity.hasOutdatedAutoStockActivities()) {
            userEntity.beginTransaction();
            userEntity.cleanAutoStockActivities();
            userEntity.commitTransaction();
        }

        Set<AutoStockActivityEntity> activities = Lists.reverse(new ArrayList<>(userEntity.getAutoStockActivities())).stream()
                .limit(10)
                .collect(Collectors.toSet());

        String buyOrders = new ListGen<Map.Entry<Stock, AutoStockOrderEntity>>()
                .getList(userEntity.getAutoStocksBuyOrders().entrySet(), getLocale(), set -> getString("default_entry", set.getKey().getName(), StringUtil.numToString(set.getValue().getOrderThreshold())));
        String sellOrders = new ListGen<Map.Entry<Stock, AutoStockOrderEntity>>()
                .getList(userEntity.getAutoStocksSellOrders().entrySet(), getLocale(), set -> getString("default_entry", set.getKey().getName(), StringUtil.numToString(set.getValue().getOrderThreshold())));
        String activityLog = new ListGen<AutoStockActivityEntity>()
                .getList(activities, getLocale(), ListGen.SLOT_TYPE_BULLET, activity -> getString("default_activity_" + activity.getType().name(), activity.getValue() != 1, TimeFormat.DATE_TIME_SHORT.atInstant(activity.getInstant()).toString(), StringUtil.numToString(activity.getValue()), activity.getStock().getName()));

        setComponents(getString("default_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("helptext"))
                .addField(getString("default_buyorders"), buyOrders, true)
                .addField(getString("default_sellorders"), sellOrders, true)
                .addField(getString("default_activitylog"), activityLog, false);
    }

    @Draw(state = STATE_MANAGE_ORDERS)
    public EmbedBuilder onDrawManageOrders(Member member) {
        StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create("stock")
                .setRequiredRange(1, 1)
                .setPlaceholder(getString("manage_placeholder"));

        Map<Stock, AutoStockOrderEntity> stockOrderEntities = getStockOrderEntities();
        for (Stock stock : Stock.values()) {
            String label = stockOrderEntities.containsKey(stock)
                    ? getString("default_entry_noemoji", stock.getName(), StringUtil.numToString(stockOrderEntities.get(stock).getOrderThreshold()))
                    : getString("default_entry_empty", stock.getName());
            Emoji emoji = Emoji.fromUnicode(stockOrderEntities.containsKey(stock) ? ((stockOrderEntities.get(stock).getActive() ? "🟢" : "🔴")) : "⚫");
            selectMenuBuilder.addOption(label, stock.name(), emoji);
        }

        setComponents(selectMenuBuilder.build());
        return EmbedFactory.getEmbedDefault(this, null, getString("manage_title", action == Action.SELL));
    }

    @Draw(state = STATE_CONFIG)
    public EmbedBuilder onDrawConfig(Member member) {
        String[] options = getString("config_options", action == Action.SELL).split("\n");
        if (newOrder) {
            options[4] = "";
        }
        setComponents(options, Set.of(3), Set.of(4));

        return EmbedFactory.getEmbedDefault(this, getString("config_desc", action == Action.SELL), getString("config_title", action == Action.SELL))
                .addField(getString("config_header_stock"), currentStock.getName(), true)
                .addField(getString("config_header_threshold", action == Action.SELL), Emojis.COINS.getFormatted() + " " + StringUtil.numToString(currentOrder.getOrderThreshold()), true)
                .addField(getString("config_header_reactivation_threshold"), currentOrder.getReactivationThreshold() != null ? (Emojis.COINS.getFormatted() + " " + StringUtil.numToString(currentOrder.getReactivationThreshold())) : TextManager.getString(getLocale(), TextManager.GENERAL, "notset"), true)
                .addField(getString("config_header_shares"), StringUtil.numToString(currentOrder.getShares()), true);
    }

    private Map<Stock, AutoStockOrderEntity> getStockOrderEntities() {
        return action == Action.BUY ? getUserEntityReadOnly().getAutoStocksBuyOrders() : getUserEntityReadOnly().getAutoStocksSellOrders();
    }

}
