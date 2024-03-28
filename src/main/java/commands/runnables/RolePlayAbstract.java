package commands.runnables;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.runnables.interactionscategory.BiteCommand;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.RandomPicker;
import core.TextManager;
import core.mention.Mention;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.user.RolePlayBlockEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public abstract class RolePlayAbstract extends Command {

    private final boolean interactive;
    private final String[] gifs;

    public RolePlayAbstract(Locale locale, String prefix, boolean interactive, String... gifs) {
        super(locale, prefix);
        this.interactive = interactive;
        this.gifs = gifs;
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        if (interactive) {
            return onTriggerInteractive(event, args);
        } else {
            return onTriggerNonInteractive(event, args);
        }
    }

    public boolean isInteractive() {
        return interactive;
    }

    public boolean onTriggerInteractive(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        Mention mention = MentionUtil.getMentionedString(getLocale(), event.getGuild(), args, event.getMember(), event.getRepliedMember());
        boolean mentionPresent = !mention.getMentionText().isEmpty();
        String authorString = "**" + StringUtil.escapeMarkdown(event.getMember().getEffectiveName()) + "**";

        if (!mentionPresent && mention.containedBlockedUser()) {
            String gif;
            String text;
            if (this instanceof BiteCommand) {
                gif = "https://cdn.discordapp.com/attachments/499629904380297226/958012189875241061/bite.gif";
                text = getString("themselves", authorString);
            } else {
                gif = "https://cdn.discordapp.com/attachments/736277561373491265/736277600053493770/hug.gif";
                text = TextManager.getString(getLocale(), Category.INTERACTIONS, "alone");
            }
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, text)
                    .setImage(gif);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        if (containsBlockedUsers(event.getMember().getIdLong(), mention.getElementList())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_blocked"));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_footer").replace("{PREFIX}", getPrefix()));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        String quote = "";
        if (mentionPresent) {
            args = mention.getFilteredArgs().get();
        }
        if (!args.isEmpty()) {
            quote = "\n\n>>> " + args;
        }

        String gifUrl = gifs[RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), gifs.length).get()];
        EmbedBuilder eb;
        if (mentionPresent) {
            eb = EmbedFactory.getEmbedDefault(this, getString("template", mention.isMultiple(), mention.getMentionText(), authorString) + quote);
        } else {
            eb = EmbedFactory.getEmbedDefault(this, getString("template_single", authorString) + quote);
        }

        eb.setImage(gifUrl);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_footer").replace("{PREFIX}", getPrefix()));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    public boolean onTriggerNonInteractive(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        String gifUrl = gifs[RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), gifs.length).get()];

        String quote = "";
        if (!args.isEmpty()) {
            quote = "\n\n>>> " + args;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString("template", "**" + StringUtil.escapeMarkdown(event.getMember().getEffectiveName()) + "**") + quote
        ).setImage(gifUrl);

        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private boolean containsBlockedUsers(long callerUserId, List<ISnowflake> users) {
        for (ISnowflake user : users) {
            RolePlayBlockEntity rolePlayBlock = getEntityManager().findUserEntityReadOnly(user.getIdLong()).getRolePlayBlock();
            if (rolePlayBlock.getBlockedUserIds().contains(callerUserId) ||
                    (rolePlayBlock.getBlockByDefault() && !rolePlayBlock.getAllowedUserIds().contains(callerUserId))
            ) {
                return true;
            }
        }
        return false;
    }

}
