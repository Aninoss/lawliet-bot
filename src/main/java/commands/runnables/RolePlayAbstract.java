package commands.runnables;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.runnables.interactionscategory.BiteCommand;
import constants.AssetIds;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.RandomPicker;
import core.TextManager;
import core.mention.Mention;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

public abstract class RolePlayAbstract extends Command {

    private final BlockUserPair[] blockUserPairs = new BlockUserPair[0];

    private final boolean interactive;
    private final boolean blockable;
    private final String[] gifs;

    public RolePlayAbstract(Locale locale, String prefix, boolean interactive, String... gifs) {
        this(locale, prefix, interactive, false, gifs);
    }

    public RolePlayAbstract(Locale locale, String prefix, boolean interactive, boolean blockable, String... gifs) {
        super(locale, prefix);
        this.interactive = interactive;
        this.blockable = blockable;
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

        if (blockable) {
            for (BlockUserPair blockUserPair : blockUserPairs) {
                if (blockUserPair.isBlocked(event.getMember(), mention.getElementList())) {
                    EmbedBuilder authorEmbed = EmbedFactory.getEmbedDefault()
                            .setDescription(args);
                    EmbedUtil.setMemberAuthor(authorEmbed, event.getMember());
                    JDAUtil.openPrivateChannel(event.getJDA(), AssetIds.OWNER_USER_ID)
                            .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(authorEmbed.build()))
                            .queue();

                    String text = "**How disgusting! I refuse to run this command!**";
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, text)
                            .setImage("https://cdn.discordapp.com/attachments/736271623098990792/834837745754964008/slap.gif");
                    drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                    return false;
                }
            }
        }

        String quote = "";
        if (mentionPresent) {
            args = mention.getFilteredArgs().get();
        }
        if (args.length() > 0) {
            quote = "\n\n>>> " + args;
        }

        String gifUrl = gifs[RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), gifs.length).get()];
        EmbedBuilder eb;
        if (mentionPresent) {
            eb = EmbedFactory.getEmbedDefault(this, getString("template", mention.isMultiple(), mention.getMentionText(), authorString) + quote)
                    .setImage(gifUrl);
        } else {
            eb = EmbedFactory.getEmbedDefault(this, getString("template_single", authorString) + quote)
                    .setImage(gifUrl);
        }

        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    public boolean onTriggerNonInteractive(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        String gifUrl = gifs[RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), gifs.length).get()];

        String quote = "";
        if (args.length() > 0) {
            quote = "\n\n>>> " + args;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString("template", "**" + StringUtil.escapeMarkdown(event.getMember().getEffectiveName()) + "**") + quote
        ).setImage(gifUrl);

        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }


    private static final class BlockUserPair {

        private final long userId0;
        private final long userId1;

        public BlockUserPair(long userId0, long userId1) {
            this.userId0 = userId0;
            this.userId1 = userId1;
        }

        public boolean isBlocked(Member author, List<ISnowflake> elementList) {
            List<Long> idList = elementList.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
            return author.getIdLong() == userId0 && idList.contains(userId1) ||
                    author.getIdLong() == userId1 && idList.contains(userId0);
        }

    }

}
