package commands.runnables.fisherysettingscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import constants.Emojis;
import core.TextManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
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

    private enum Currency {FISH, COINS, RECENT_EFFICIENCY}

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
                Section.of(
                        buttonPrimary(Emojis.MENU_EDIT, e -> {
                            showModal(e, getString("root_fish"), Currency.FISH);
                            return false;
                        }),
                        TextDisplay.of(Emojis.FISH.getFormatted() + " " + getString("root_fish") + " → " + Emojis.FISH.getFormatted() + " " + getString("root_fish"))
                ),
                Section.of(
                        buttonPrimary(Emojis.MENU_EDIT, e -> {
                            showModal(e, getString("root_coins"), Currency.COINS);
                            return false;
                        }),
                        TextDisplay.of(Emojis.COINS.getFormatted() + " " + getString("root_coins") + " → " + Emojis.COINS.getFormatted() + " " + getString("root_coins"))
                ),
                Section.of(
                        buttonPrimary(Emojis.MENU_EDIT, e -> {
                            showModal(e, getString("root_coins"), Currency.RECENT_EFFICIENCY);
                            return false;
                        }),
                        TextDisplay.of(Emojis.GROWTH.getFormatted() + " " + getString("root_recent_efficiency") + " → " + Emojis.GROWTH.getFormatted() + " " + getString("root_recent_efficiency"))
                )
        );
    }

    private void showModal(ButtonInteractionEvent event, String defaultName, Currency currentCurrency) {
        TextInput textInputEmoji = TextInput.create("emoji", TextInputStyle.SHORT)
                .setValue(null)
                .setRequiredRange(1, 4)
                .setRequired(true)
                .build();

        TextInput textInputName = TextInput.create("label", TextInputStyle.SHORT)
                .setValue(null)
                .setRequiredRange(1, 100)
                .setRequired(true)
                .build();

        List<ModalTopLevelComponent> components = List.of(
                Label.of(getString("root_modal_emoji"), textInputEmoji),
                TextDisplay.of("-# " + TextManager.getString(getLocale(), TextManager.GENERAL, "emoji_paste")),
                Label.of(getString("root_modal_name"), textInputName)
        );
        Modal modal = modal(defaultName, components, e -> {
                    String emoji = e.getValue(textInputEmoji.getCustomId()).getAsString();
                    String label = e.getValue(textInputName.getCustomId()).getAsString();
                    //TODO
                }
        );
        event.replyModal(modal).queue();
    }

}
