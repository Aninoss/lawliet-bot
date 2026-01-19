package commands.runnables.fisherysettingscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import constants.Emojis;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "fisherycurrencies",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83D\uDC1F",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = {"currencies"}
)
public class FisheryCurrenciesCommand extends ComponentMenuAbstract {

    private enum Currency { FISH, COINS, RECENT_EFFICIENCY }

    private static final String STATE_CURRENCY = "currency";

    public FisheryCurrenciesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    private StateData currencyStateDate = StateData.of(STATE_CURRENCY, STATE_ROOT, "");
    private Currency currentCurrency = null;
    private String currentCurrencyDefaultName = null;
    private Emoji currentCurrencyDefaultEmoji = null;
    private String currentCurrencyName = null;
    private Emoji currentCurrencyEmoji = null;

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerListeners(event.getMember(),
                currencyStateDate
        );
        return true;
    }

    @Draw(state = STATE_ROOT)
    public List<ContainerChildComponent> drawRoot(Member member) {
        setDescription(getString("root_description"));
        return List.of(
                Section.of(
                        buttonPrimary(Emojis.MENU_SHORT_ARROW_RIGHT, e -> {
                            setState(STATE_CURRENCY);
                            currentCurrency = Currency.FISH;
                            currentCurrencyDefaultName = getString("root_fish");
                            currentCurrencyDefaultEmoji = Emojis.FISH;
                            currentCurrencyName = getString("root_fish");
                            currentCurrencyEmoji = Emojis.FISH;
                            return true;
                        }),
                        TextDisplay.of(Emojis.FISH.getFormatted() + " " + getString("root_fish") + " → " + Emojis.FISH.getFormatted() + " " + getString("root_fish"))
                ),
                Section.of(
                        buttonPrimary(Emojis.MENU_SHORT_ARROW_RIGHT, e -> {
                            setState(STATE_CURRENCY);
                            currentCurrency = Currency.COINS;
                            currentCurrencyDefaultName = getString("root_coins");
                            currentCurrencyDefaultEmoji = Emojis.COINS;
                            currentCurrencyName = getString("root_coins");
                            currentCurrencyEmoji = Emojis.COINS;
                            return true;
                        }),
                        TextDisplay.of(Emojis.COINS.getFormatted() + " " + getString("root_coins") + " → " + Emojis.COINS.getFormatted() + " " + getString("root_coins"))
                ),
                Section.of(
                        buttonPrimary(Emojis.MENU_SHORT_ARROW_RIGHT, e -> {
                            setState(STATE_CURRENCY);
                            currentCurrency = Currency.RECENT_EFFICIENCY;
                            currentCurrencyDefaultName = getString("root_recent_efficiency");
                            currentCurrencyDefaultEmoji = Emojis.GROWTH;
                            currentCurrencyName = getString("root_recent_efficiency");
                            currentCurrencyEmoji = Emojis.GROWTH;
                            return true;
                        }),
                        TextDisplay.of(Emojis.GROWTH.getFormatted() + " " + getString("root_recent_efficiency") + " → " + Emojis.GROWTH.getFormatted() + " " + getString("root_recent_efficiency"))
                )
        );
    }

    @Draw(state = STATE_CURRENCY)
    public List<ContainerChildComponent> drawCurrency(Member member) {
        currencyStateDate.setTitle(currentCurrencyDefaultName);
        return List.of(
                Section.of(
                        buttonPrimary(Emojis.MENU_EDIT, e -> {
                            return true;
                        }),
                        TextDisplay.of(currentCurrencyDefaultEmoji.getFormatted() + " → " + currentCurrencyEmoji.getFormatted())
                ),
                Section.of(
                        buttonPrimary(Emojis.MENU_EDIT, e -> {
                            Modal modal = setStringModal(
                                    getString("currency_name"),
                                    currentCurrencyName,
                                    null,
                                    1,
                                    64,
                                    newName -> {
                                        setUnsavedChanges();
                                        currentCurrencyName = newName;
                                    });
                            e.replyModal(modal).queue();
                            return false;
                        }),
                        TextDisplay.of(currentCurrencyDefaultName + " → " + currentCurrencyName)
                )
        );
    }

}
