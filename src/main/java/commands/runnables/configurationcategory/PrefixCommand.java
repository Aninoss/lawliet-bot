package commands.runnables.configurationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.StringUtil;
import modules.Prefix;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CommandProperties(
        trigger = "prefix",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83D\uDCDB",
        executableWithoutArgs = true
)
public class PrefixCommand extends Command implements OnButtonListener {

    public static int MAX_LENGTH = 5;

    public PrefixCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        if (!args.isEmpty() && !args.isBlank()) {
            if (args.length() <= MAX_LENGTH) {
                Prefix.changePrefix(event.getMember(), getLocale(), args, getGuildEntity());
                drawMessageNew(EmbedFactory.getEmbedDefault(this, getString("changed", StringUtil.escapeMarkdownInField(args))))
                        .exceptionally(ExceptionLogger.get());
                return true;
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(
                        this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", StringUtil.numToString(MAX_LENGTH))
                )).exceptionally(ExceptionLogger.get());
                return false;
            }
        } else {
            registerButtonListener(event.getMember());
            return false;
        }
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        TextInput textInput = TextInput.create("text", getString("new"), TextInputStyle.SHORT)
                .setValue(getGuildEntity().getPrefix())
                .setMinLength(1)
                .setMaxLength(MAX_LENGTH)
                .build();

        Modal modal = ModalMediator.createDrawableCommandModal(this, getString("button"), e -> {
                    deregisterListeners();
                    String newPrefix = e.getValues().get(0).getAsString();
                    if (newPrefix.isBlank()) {
                        newPrefix = "L.";
                    }

                    Prefix.changePrefix(e.getMember(), getLocale(), newPrefix, getGuildEntity());
                    return EmbedFactory.getEmbedDefault(this, getString("changed", StringUtil.escapeMarkdownInField(newPrefix)));
                })
                .addActionRow(textInput)
                .build();
        event.replyModal(modal).queue();
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        String prefix = getGuildEntity().getPrefix();
        setComponents(getString("button"));
        return EmbedFactory.getEmbedDefault(this, getString("current", StringUtil.escapeMarkdownInField(prefix)));
    }

}
