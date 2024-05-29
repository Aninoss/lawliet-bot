package commands.runnables.fisherycategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import constants.Settings;
import core.EmbedFactory;
import core.LocalFile;
import core.Program;
import core.TextManager;
import core.modals.AmountModalBuilder;
import core.utils.FileUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.fishery.Stock;
import modules.fishery.StockMarket;
import modules.graphics.StockMarketGraphics;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryMemberStocksData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "stocks",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "🚀",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        releaseDate = { 2021, 9, 3 },
        aliases = { "stock", "stonks", "shares", "share", "stockmarket" }
)
public class StocksCommand extends NavigationAbstract implements FisheryInterface {

    private static final int STATE_BUY = 1,
            STATE_SELL = 2;

    private Stock currentStock;
    private FisheryMemberData fisheryMemberData;
    private int sharesNum = 0;

    public StocksCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        currentStock = Stock.values()[0];
        fisheryMemberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = STATE_BUY)
    public MessageInputResponse onMessageBuy(MessageReceivedEvent event, String input) {
        long maxValue = (long) Math.floor((double) fisheryMemberData.getCoins() / StockMarket.getValue(currentStock) / (1 + Settings.FISHERY_SHARES_FEES / 100.0));
        return onMessageBuySell(input, Math.min(maxValue, Settings.FISHERY_SHARES_MAX));
    }

    @ControllerMessage(state = STATE_SELL)
    public MessageInputResponse onMessageSell(MessageReceivedEvent event, String input) {
        long maxValue = fisheryMemberData.getStocks(currentStock).getShareSize();
        return onMessageBuySell(input, maxValue);
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonMain(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                if (fisheryMemberData.getStocks(currentStock).getShareSize() < Settings.FISHERY_SHARES_MAX) {
                    sharesNum = 0;
                    setState(STATE_BUY);
                } else {
                    setLog(LogStatus.FAILURE, getString("buy_toomany", StringUtil.numToString(Settings.FISHERY_SHARES_MAX)));
                }
                return true;
            }
            case 1 -> {
                if (fisheryMemberData.getStocks(currentStock).getShareSize() > 0) {
                    sharesNum = 0;
                    setState(STATE_SELL);
                } else {
                    setLog(LogStatus.FAILURE, getString("sell_none"));
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = STATE_BUY)
    public boolean onButtonBuy(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(DEFAULT_STATE);
                return true;
            }
            case 0 -> {
                long maxValue = (long) Math.floor((double) fisheryMemberData.getCoins() / StockMarket.getValue(currentStock) / (1 + Settings.FISHERY_SHARES_FEES / 100.0));
                Modal modal = new AmountModalBuilder(this, getString("shares"))
                        .setMinMax(1, Math.min(maxValue, Settings.FISHERY_SHARES_MAX))
                        .setGetter(() -> sharesNum != 0 ? (long) sharesNum : null)
                        .setSetterOptionalLogs(value -> {
                            sharesNum = value.intValue();
                            return false;
                        })
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                FisheryMemberStocksData stocksData = fisheryMemberData.getStocks(currentStock);
                long totalPrice = Math.round(sharesNum * StockMarket.getValue(currentStock) * (1 + Settings.FISHERY_SHARES_FEES / 100.0));
                if (fisheryMemberData.getCoins() >= totalPrice) {
                    if (sharesNum <= Settings.FISHERY_SHARES_MAX - stocksData.getShareSize()) {
                        fisheryMemberData.addCoinsRaw(-totalPrice);
                        stocksData.add(sharesNum);
                        setLog(LogStatus.SUCCESS, getString("buy_success", sharesNum != 1, StringUtil.numToString(sharesNum), currentStock.getName()));
                        setState(DEFAULT_STATE);
                    } else {
                        setLog(LogStatus.FAILURE, getString("buy_toomany", StringUtil.numToString(Settings.FISHERY_SHARES_MAX)));
                    }
                } else {
                    setLog(LogStatus.FAILURE, getString("buy_notenough"));
                }
                return true;
            }
        }
        return false;
    }

    @ControllerButton(state = STATE_SELL)
    public boolean onButtonSell(ButtonInteractionEvent event, int i) {
        switch (i) {
            case  -1 -> {
                setState(DEFAULT_STATE);
                return true;
            }
            case 0 -> {
                long maxValue = fisheryMemberData.getStocks(currentStock).getShareSize();
                Modal modal = new AmountModalBuilder(this, getString("shares"))
                        .setMinMax(1, maxValue)
                        .setGetter(() -> sharesNum != 0 ? (long) sharesNum : null)
                        .setSetterOptionalLogs(value -> {
                            sharesNum = value.intValue();
                            return false;
                        })
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                FisheryMemberStocksData stocksData = fisheryMemberData.getStocks(currentStock);
                if (sharesNum <= stocksData.getShareSize()) {
                    fisheryMemberData.addCoinsRaw(sharesNum * StockMarket.getValue(currentStock));
                    stocksData.add(-sharesNum);
                    setLog(LogStatus.SUCCESS, getString("sell_success", sharesNum != 1, StringUtil.numToString(sharesNum), currentStock.getName()));
                    setState(DEFAULT_STATE);
                } else {
                    setLog(LogStatus.FAILURE, getString("sell_notenough"));
                }
                return true;
            }
        }
        return false;
    }

    @ControllerStringSelectMenu(state = DEFAULT_STATE)
    public boolean onSelectMenuMain(StringSelectInteractionEvent event, int i) {
        currentStock = Stock.values()[i];
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) throws IOException, ExecutionException, InterruptedException {
        long coins = fisheryMemberData.getCoins();
        long totalShares = fisheryMemberData.getStocksTotalShares();
        long totalInvestmentBefore = fisheryMemberData.getStocksTotalInvestmentBefore();
        long totalInvestmentAfter = fisheryMemberData.getStocksTotalInvestmentAfter();
        String desc = getString(
                "values",
                StringUtil.numToString(coins),
                StringUtil.numToString(totalShares),
                generateInvestmentGrowth(totalInvestmentBefore, totalInvestmentAfter)
        );

        Button[] buttons = new Button[] {
                Button.of(ButtonStyle.PRIMARY, "0", getString("button_buy")),
                Button.of(ButtonStyle.PRIMARY, "1", getString("button_sell")),
        };
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("selection")
                .setMinValues(1);
        for (int i = 0; i < Stock.values().length; i++) {
            Stock stock = Stock.values()[i];
            long price = StockMarket.getValue(stock);
            long pricePrevious = StockMarket.getValue(stock, -1);
            menuBuilder.addOption(
                    stock.getName(),
                    String.valueOf(i),
                    getString("select_desc", StringUtil.numToString(price), generateChangeArrow(pricePrevious, price))
            );
        }
        SelectMenu menu = menuBuilder.setDefaultValues(List.of(String.valueOf(currentStock.ordinal())))
                .build();
        ActionRow[] actionRows = new ActionRow[] {
                ActionRow.of(buttons),
                ActionRow.of(menu)
        };

        setActionRows(actionRows);
        return EmbedFactory.getEmbedDefault(this)
                .setDescription(desc + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted())
                .addField(currentStock.getName(), generateStockDescription(), false)
                .setImage(getStockGraphUrl() + "?" + TimeUtil.currentHour());
    }

    @Draw(state = STATE_BUY)
    public EmbedBuilder onDrawBuy(Member member) {
        long coins = fisheryMemberData.getCoins();
        long price = StockMarket.getValue(currentStock);
        long pricePrevious = StockMarket.getValue(currentStock, -1);
        String desc = getString("buy", StringUtil.numToString(coins));
        String attr = getString(
                sharesNum > 0 ? "buy_attr" : "buy_attr_empty",
                StringUtil.numToString(price),
                generateChangeArrow(pricePrevious, price),
                StringUtil.numToString(sharesNum),
                StringUtil.numToString(Math.min(Math.round(sharesNum * price * (1 + Settings.FISHERY_SHARES_FEES / 100.0)), Settings.FISHERY_MAX)),
                String.valueOf(Settings.FISHERY_SHARES_FEES)
        );

        String[] options = getString("buy_options").split("\n");
        setComponents(options, Set.of(1), null, sharesNum == 0 ? Set.of(1) : null);
        return EmbedFactory.getEmbedDefault(this, desc, getString("buy_title", currentStock.getName()))
                .addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), attr, false);
    }

    @Draw(state = STATE_SELL)
    public EmbedBuilder onDrawSell(Member member) {
        long shares = fisheryMemberData.getStocks(currentStock).getShareSize();
        long price = StockMarket.getValue(currentStock);
        long pricePrevious = StockMarket.getValue(currentStock, -1);
        String desc = getString(
                "sell",
                shares != 1,
                StringUtil.numToString(shares),
                currentStock.getName()
        );
        String attr = getString(
                sharesNum > 0 ? "sell_attr" : "sell_attr_empty",
                StringUtil.numToString(price),
                generateChangeArrow(pricePrevious, price),
                StringUtil.numToString(sharesNum),
                StringUtil.numToString(Math.min(sharesNum * price, Settings.FISHERY_MAX))
        );

        String[] options = getString("sell_options").split("\n");
        setComponents(options, Set.of(1), null, sharesNum == 0 ? Set.of(1) : null);
        return EmbedFactory.getEmbedDefault(this, desc, getString("sell_title", currentStock.getName()))
                .addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), attr, false);
    }

    private MessageInputResponse onMessageBuySell(String input, long maxValue) {
        long amount = MentionUtil.getAmountExt(input, maxValue);
        if (amount > 0 && amount <= maxValue) {
            sharesNum = (int) amount;
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", String.valueOf(1), StringUtil.numToString(maxValue)));
            return MessageInputResponse.FAILED;
        }
    }

    private String generateStockDescription() {
        FisheryMemberStocksData stocksData = fisheryMemberData.getStocks(currentStock);
        long price = StockMarket.getValue(currentStock);
        long pricePrevious = StockMarket.getValue(currentStock, -1);
        long shares = stocksData.getShareSize();
        long investmentBefore = stocksData.getInvestedBefore();
        long investmentAfter = stocksData.getInvestedAfter();
        return getString(
                "stock",
                StringUtil.numToString(price),
                generateChangeArrow(pricePrevious, price),
                StringUtil.numToString(shares),
                generateInvestmentGrowth(investmentBefore, investmentAfter)
        );
    }

    private String generateInvestmentGrowth(long before, long after) {
        double growthPercent = before != 0 ? (double) after / before - 1 : 0;
        String growthPrefix = growthPercent > 0 ? "+" : "";
        return getString(
                "investment",
                StringUtil.numToString(before),
                StringUtil.numToString(after),
                growthPrefix + StringUtil.doubleToString(growthPercent * 100, 2)
        );
    }

    private String generateChangeArrow(long before, long now) {
        if (now > before) {
            return "🔺";
        } else {
            if (now < before) {
                return "🔻";
            } else {
                return "•";
            }
        }
    }

    private String getStockGraphUrl() throws ExecutionException, InterruptedException, IOException {
        long currentHourSlotMillis = (System.currentTimeMillis() / 3_600_000L) * 3_600_000L;
        LocalFile graphFile = new LocalFile(LocalFile.Directory.CDN, String.format("stockmarket/%d.png", currentStock.getId()));
        if ((graphFile.exists() && currentHourSlotMillis <= graphFile.lastModified()) || !Program.productionMode()) {
            return graphFile.cdnGetUrl();
        } else {
            InputStream is = StockMarketGraphics.createImageGraph(currentStock).get();
            return FileUtil.writeInputStreamToFile(is, graphFile);
        }
    }

}
