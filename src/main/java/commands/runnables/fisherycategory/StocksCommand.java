package commands.runnables.fisherycategory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
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
import core.utils.FileUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.fishery.Stock;
import modules.fishery.StockMarket;
import modules.graphics.StockMarketGraphics;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.fisheryusers.FisheryMemberStocksData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

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

    private final int MAIN = 0,
            BUY = 1,
            SELL = 2;

    private Stock currentStock;
    private FisheryMemberData fisheryMemberBean;
    private int sharesNum = 0;

    public StocksCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        currentStock = Stock.values()[0];
        fisheryMemberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = BUY)
    public MessageInputResponse onMessageBuy(GuildMessageReceivedEvent event, String input) {
        long maxValue = (long) Math.floor((double) fisheryMemberBean.getCoins() / StockMarket.getValue(currentStock) / (1 + Settings.FISHERY_SHARES_FEES / 100.0));
        return onMessageBuySell(input, Math.min(maxValue, Settings.FISHERY_SHARES_MAX));
    }

    @ControllerMessage(state = SELL)
    public MessageInputResponse onMessageSell(GuildMessageReceivedEvent event, String input) {
        long maxValue = fisheryMemberBean.getStocks(currentStock).getShareSize();
        return onMessageBuySell(input, maxValue);
    }

    private MessageInputResponse onMessageBuySell(String input, long maxValue) {
        long amount = MentionUtil.getAmountExt(input, maxValue);
        if (amount > 0 && amount <= Settings.FISHERY_SHARES_MAX) {
            sharesNum = (int) amount;
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", String.valueOf(1), StringUtil.numToString(Settings.FISHERY_SHARES_MAX)));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerButton(state = MAIN)
    public boolean onButtonMain(ButtonClickEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                if (fisheryMemberBean.getStocks(currentStock).getShareSize() < Settings.FISHERY_SHARES_MAX) {
                    sharesNum = 0;
                    setState(BUY);
                } else {
                    setLog(LogStatus.FAILURE, getString("buy_toomany", StringUtil.numToString(Settings.FISHERY_SHARES_MAX)));
                }
                return true;
            }
            case 1 -> {
                if (fisheryMemberBean.getStocks(currentStock).getShareSize() > 0) {
                    sharesNum = 0;
                    setState(SELL);
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

    @ControllerButton(state = BUY)
    public boolean onButtonBuy(ButtonClickEvent event, int i) {
        if (i == -1) {
            setState(MAIN);
            return true;
        } else if (i == 0 && sharesNum > 0) {
            FisheryMemberStocksData stocksData = fisheryMemberBean.getStocks(currentStock);
            long totalPrice = Math.round(sharesNum * StockMarket.getValue(currentStock) * (1 + Settings.FISHERY_SHARES_FEES / 100.0));
            if (fisheryMemberBean.getCoins() >= totalPrice) {
                if (sharesNum <= Settings.FISHERY_SHARES_MAX - stocksData.getShareSize()) {
                    fisheryMemberBean.addCoinsRaw(-totalPrice);
                    stocksData.add(sharesNum);
                    setLog(LogStatus.SUCCESS, getString("buy_success", sharesNum != 1, StringUtil.numToString(sharesNum), currentStock.getName()));
                    setState(MAIN);
                } else {
                    setLog(LogStatus.FAILURE, getString("buy_toomany", StringUtil.numToString(Settings.FISHERY_SHARES_MAX)));
                }
            } else {
                setLog(LogStatus.FAILURE, getString("buy_notenough"));
            }
            return true;
        }
        return false;
    }

    @ControllerButton(state = SELL)
    public boolean onButtonSell(ButtonClickEvent event, int i) {
        if (i == -1) {
            setState(MAIN);
            return true;
        } else if (i == 0 && sharesNum > 0) {
            FisheryMemberStocksData stocksData = fisheryMemberBean.getStocks(currentStock);
            if (sharesNum <= stocksData.getShareSize()) {
                fisheryMemberBean.addCoinsRaw(sharesNum * StockMarket.getValue(currentStock));
                stocksData.add(-sharesNum);
                setLog(LogStatus.SUCCESS, getString("sell_success", sharesNum != 1, StringUtil.numToString(sharesNum), currentStock.getName()));
                setState(MAIN);
            } else {
                setLog(LogStatus.FAILURE, getString("sell_notenough"));
            }
            return true;
        }
        return false;
    }

    @ControllerSelectionMenu(state = MAIN)
    public boolean onSelectionMenuMain(SelectionMenuEvent event, int i) {
        currentStock = Stock.values()[i];
        return true;
    }

    @Draw(state = MAIN)
    public EmbedBuilder onDrawMain(Member member) throws IOException, ExecutionException, InterruptedException {
        long coins = fisheryMemberBean.getCoins();
        long totalShares = fisheryMemberBean.getStocksTotalShares();
        long totalInvestmentBefore = fisheryMemberBean.getStocksTotalInvestmentBefore();
        long totalInvestmentAfter = fisheryMemberBean.getStocksTotalInvestmentAfter();
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
        SelectionMenu.Builder menuBuilder = SelectionMenu.create("selection")
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
        SelectionMenu menu = menuBuilder.setDefaultValues(List.of(String.valueOf(currentStock.ordinal())))
                .build();
        ActionRow[] actionRows = new ActionRow[] {
                ActionRow.of(buttons),
                ActionRow.of(menu)
        };

        setActionRows(actionRows);
        return EmbedFactory.getEmbedDefault(this)
                .setDescription(desc + "\n" + Emojis.ZERO_WIDTH_SPACE)
                .addField(currentStock.getName(), generateStockDescription(), false)
                .setImage(getStockGraphUrl() + "?" + TimeUtil.currentHour());
    }

    @Draw(state = BUY)
    public EmbedBuilder onDrawBuy(Member member) {
        long coins = fisheryMemberBean.getCoins();
        long price = StockMarket.getValue(currentStock);
        long pricePrevious = StockMarket.getValue(currentStock, -1);
        String desc = getString("buy", StringUtil.numToString(coins));
        String attr = getString(
                "buy_attr",
                sharesNum > 0,
                StringUtil.numToString(price),
                generateChangeArrow(pricePrevious, price),
                StringUtil.numToString(sharesNum),
                StringUtil.numToString(Math.min(Math.round(sharesNum * price * (1 + Settings.FISHERY_SHARES_FEES / 100.0)), Settings.FISHERY_MAX)),
                String.valueOf(Settings.FISHERY_SHARES_FEES)
        );

        if (sharesNum > 0) {
            setComponents(getString("buy_confirm"));
        }
        return EmbedFactory.getEmbedDefault(this, desc, getString("buy_title", currentStock.getName()))
                .addField(Emojis.ZERO_WIDTH_SPACE, attr, false);
    }

    @Draw(state = SELL)
    public EmbedBuilder onDrawSell(Member member) {
        long shares = fisheryMemberBean.getStocks(currentStock).getShareSize();
        long price = StockMarket.getValue(currentStock);
        long pricePrevious = StockMarket.getValue(currentStock, -1);
        String desc = getString(
                "sell",
                shares != 1,
                StringUtil.numToString(shares),
                currentStock.getName()
        );
        String attr = getString(
                "sell_attr",
                sharesNum > 0,
                StringUtil.numToString(price),
                generateChangeArrow(pricePrevious, price),
                StringUtil.numToString(sharesNum),
                StringUtil.numToString(Math.min(sharesNum * price, Settings.FISHERY_MAX))
        );

        if (sharesNum > 0) {
            setComponents(getString("sell_confirm"));
        }
        return EmbedFactory.getEmbedDefault(this, desc, getString("sell_title", currentStock.getName()))
                .addField(Emojis.ZERO_WIDTH_SPACE, attr, false);
    }

    private String generateStockDescription() {
        FisheryMemberStocksData stocksData = fisheryMemberBean.getStocks(currentStock);
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
