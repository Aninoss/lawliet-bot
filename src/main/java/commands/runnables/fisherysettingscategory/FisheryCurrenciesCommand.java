package commands.runnables.fisherysettingscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.TextManager;
import core.utils.MentionUtil;
import modules.fishery.FisheryCurrency;
import mysql.hibernate.entity.FisheryCurrencyEntity;
import mysql.hibernate.entity.guild.GuildEntity;
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
import java.util.function.Consumer;

@CommandProperties(
        trigger = "fisherycurrencies",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83D\uDC1F",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = {"currencies"}
)
public class FisheryCurrenciesCommand extends ComponentMenuAbstract {

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
                        currencyEntity -> {
                            GuildEntity guildEntity = getGuildEntity();
                            guildEntity.beginTransaction();
                            guildEntity.getFishery().setFishCurrency(currencyEntity);
                            guildEntity.commitTransaction();
                        }
                ),
                createSection(
                        Emojis.COINS,
                        getString("root_coins"),
                        FisheryCurrency.COINS,
                        currencyEntity -> {
                            GuildEntity guildEntity = getGuildEntity();
                            guildEntity.beginTransaction();
                            guildEntity.getFishery().setCoinsCurrency(currencyEntity);
                            guildEntity.commitTransaction();
                        }
                ),
                createSection(
                        Emojis.GROWTH,
                        getString("root_recent_efficiency"),
                        FisheryCurrency.RECENT_EFFICIENCY,
                        currencyEntity -> {
                            GuildEntity guildEntity = getGuildEntity();
                            guildEntity.beginTransaction();
                            guildEntity.getFishery().setRecentEfficiencyCurrency(currencyEntity);
                            guildEntity.commitTransaction();
                        }
                )
        );
    }

    private Section createSection(Emoji defaultEmoji, String defaultName, FisheryCurrency currency, Consumer<FisheryCurrencyEntity> setter) {
        FisheryCurrencyEntity currencyEntity = getGuildEntity().getFishery().getCurrencyEffectivelyReadOnly(currency);
        return Section.of(
                buttonPrimary(Emojis.MENU_EDIT, e -> {
                    showModal(e, defaultName, currency, currencyEntity, setter);
                    return false;
                }),
                TextDisplay.of(defaultEmoji.getFormatted() + " " +
                        defaultName + " → " +
                        currencyEntity.getEmoji().getFormatted() + " " +
                        currencyEntity.getName()
                )
        );
    }

    private void showModal(ButtonInteractionEvent event, String defaultName, FisheryCurrency currency, FisheryCurrencyEntity currencyEntity, Consumer<FisheryCurrencyEntity> setter) {
        TextInput textInputEmoji = TextInput.create("emoji", TextInputStyle.SHORT)
                .setValue(currencyEntity.getEmoji().getFormatted())
                .setRequiredRange(0, 100)
                .setRequired(false)
                .build();

        TextInput textInputName = TextInput.create("label", TextInputStyle.SHORT)
                .setValue(currencyEntity.getName())
                .setRequiredRange(0, 100)
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

                    FisheryCurrencyEntity newCurrencyEntity = new FisheryCurrencyEntity(emojiFormatted, name);
                    setter.accept(newCurrencyEntity);
                }
        );
        event.replyModal(modal).queue();
    }

}
