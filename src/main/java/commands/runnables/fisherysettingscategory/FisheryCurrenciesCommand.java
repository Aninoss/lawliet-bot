package commands.runnables.fisherysettingscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.TextManager;
import core.utils.MentionUtil;
import modules.fishery.FisheryCurrency;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.FisheryCurrencyEntity;
import mysql.hibernate.entity.guild.FisheryEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
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

    public static final int MAX_EMOJI_LENGTH = 100;
    public static final int MAX_NAME_LENGTH = 100;

    public FisheryCurrenciesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerListeners(event.getMember());
        return true;
    }

    @Draw(state = STATE_ROOT)
    public List<ContainerChildComponent> drawRoot(Member member) {
        setDescription(getString("root_description"));
        return List.of(
                createSection(
                        Emojis.FISH,
                        getString("root_fish"),
                        FisheryCurrency.FISH,
                        BotLogEntity.Event.FISHERY_CURRENCIES_FISH
                ),
                createSection(
                        Emojis.COINS,
                        getString("root_coins"),
                        FisheryCurrency.COINS,
                        BotLogEntity.Event.FISHERY_CURRENCIES_COINS
                ),
                createSection(
                        Emojis.RECENT_EFFICIENCY,
                        getString("root_recent_efficiency"),
                        FisheryCurrency.RECENT_EFFICIENCY,
                        BotLogEntity.Event.FISHERY_CURRENCIES_RECENT_EFFICIENCY
                )
        );
    }

    private Section createSection(Emoji defaultEmoji, String defaultName, FisheryCurrency currency, BotLogEntity.Event botLogEvent) {
        FisheryCurrencyEntity currencyEntity = getGuildEntity().getFishery().getCurrencyEffectivelyReadOnly(currency);
        return Section.of(
                buttonPrimary(Emojis.MENU_EDIT, e -> {
                    showModal(e, defaultName, currency, currencyEntity, botLogEvent);
                    return false;
                }),
                TextDisplay.of(defaultEmoji.getFormatted() + " " +
                        defaultName + " → " +
                        currencyEntity.getEmoji().getFormatted() + " " +
                        currencyEntity.getName()
                )
        );
    }

    private void showModal(ButtonInteractionEvent event, String defaultName, FisheryCurrency currency, FisheryCurrencyEntity currencyEntity, BotLogEntity.Event botLogEvent) {
        TextInput textInputEmoji = TextInput.create("emoji", TextInputStyle.SHORT)
                .setValue(currencyEntity.getEmoji().getFormatted())
                .setRequiredRange(0, MAX_EMOJI_LENGTH)
                .setRequired(false)
                .build();

        TextInput textInputName = TextInput.create("label", TextInputStyle.SHORT)
                .setValue(currencyEntity.getName())
                .setRequiredRange(0, MAX_NAME_LENGTH)
                .setRequired(false)
                .build();

        List<ModalTopLevelComponent> components = List.of(
                Label.of(getString("root_modal_emoji"), textInputEmoji),
                TextDisplay.of("-# " + TextManager.getString(getLocale(), TextManager.GENERAL, "modal_emoji") + " " + TextManager.getString(getLocale(), TextManager.GENERAL, "modal_reset")),
                Label.of(getString("root_modal_name"), textInputName),
                TextDisplay.of("-# " + TextManager.getString(getLocale(), TextManager.GENERAL, "modal_reset"))
        );
        Modal modal = modal(defaultName, components, e -> {
                    String emojiFormatted = e.getValue(textInputEmoji.getCustomId()).getAsString();
                    if (emojiFormatted.isEmpty()) {
                        emojiFormatted = null;
                    } else {
                        List<Emoji> emojiList = MentionUtil.getEmojis(e.getGuild(), emojiFormatted).getList();
                        if (emojiList.isEmpty()) {
                            setLog(LogStatus.FAILURE, getString("invalid_emoji"));
                            return;
                        }
                        emojiFormatted = emojiList.get(0).getFormatted();
                    }

                    String name = e.getValue(textInputName.getCustomId()).getAsString();
                    if (name.isEmpty()) {
                        name = null;
                    }

                    FisheryEntity fisheryEntity = getGuildEntity().getFishery();
                    String previousCurrencyString = createCombinedCurrencyString(fisheryEntity, currency);

                    fisheryEntity.beginTransaction();
                    FisheryCurrencyEntity newCurrencyEntity = new FisheryCurrencyEntity(emojiFormatted, name);
                    fisheryEntity.setCurrency(currency, newCurrencyEntity);

                    String newCurrencyString = createCombinedCurrencyString(fisheryEntity, currency);
                    BotLogEntity.log(getEntityManager(), botLogEvent, e.getMember(), previousCurrencyString, newCurrencyString);
                    fisheryEntity.commitTransaction();
                }
        );
        event.replyModal(modal).queue();
    }

    public static String createCombinedCurrencyString(FisheryEntity fisheryEntity, FisheryCurrency currency) {
        FisheryCurrencyEntity currencyEntity = fisheryEntity.getCurrencyEffectivelyReadOnly(currency);
        return createCombinedCurrencyString(currencyEntity);
    }

    public static String createCombinedCurrencyString(FisheryCurrencyEntity currencyEntity) {
        return currencyEntity.getEmoji().getFormatted() + " " + currencyEntity.getName();
    }

}
