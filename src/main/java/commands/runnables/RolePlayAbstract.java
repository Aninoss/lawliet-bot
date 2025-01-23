package commands.runnables;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.OnEntitySelectMenuListener;
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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class RolePlayAbstract extends Command implements OnEntitySelectMenuListener {

    private final boolean interactive;
    private final String[] gifs;
    private String gifUrl;
    private String quote = "";
    private List<Member> selectMenuMemberMentions;

    public RolePlayAbstract(Locale locale, String prefix, boolean interactive, String... gifs) {
        super(locale, prefix);
        this.interactive = interactive;
        this.gifs = gifs;
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        gifUrl = gifs[RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), gifs.length).get()];
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
        boolean mentionPresent = !mention.getElementList().isEmpty();
        boolean selfReference = !mentionPresent && mention.containedBlockedUser();

        if (mentionPresent) {
            args = mention.getFilteredArgs().get();
        }
        args = args.replace(event.getMember().getAsMention(), "").trim();
        if (!args.isEmpty()) {
            quote = "\n\n>>> " + args;
        }
        EmbedBuilder eb = generateEmbed(event.getMember(), mention, selfReference);

        if (!mentionPresent && !selfReference) {
            EntitySelectMenu selectMenu = EntitySelectMenu.create("members", EntitySelectMenu.SelectTarget.USER)
                    .setPlaceholder(TextManager.getString(getLocale(), Category.INTERACTIONS, "selectmenu_placeholder"))
                    .setRequiredRange(1, SelectMenu.OPTIONS_MAX_AMOUNT)
                    .build();
            setComponents(selectMenu);
        }
        drawMessage(eb).exceptionally(ExceptionLogger.get());

        if (!mentionPresent && !selfReference) {
            registerEntitySelectMenuListener(event.getMember(), false);
        }
        return true;
    }

    public boolean onTriggerNonInteractive(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        if (!args.isEmpty()) {
            quote = "\n\n>>> " + args;
        }

        String desc = getString("template", "**" + StringUtil.escapeMarkdown(event.getMember().getEffectiveName()) + "**") + quote;
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, desc).setImage(gifUrl);
        drawMessage(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    @Override
    public boolean onEntitySelectMenu(@NotNull EntitySelectInteractionEvent event) {
        deregisterListeners();
        selectMenuMemberMentions = event.getMentions().getMembers().stream()
                .filter(member -> member.getIdLong() != event.getUser().getIdLong())
                .collect(Collectors.toList());
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        return generateEmbed(
                member,
                MentionUtil.getMentionedStringOfMembers(getLocale(), selectMenuMemberMentions),
                selectMenuMemberMentions.isEmpty()
        );
    }

    protected EmbedBuilder generateEmbed(Member member, Mention mention, boolean onlySelfReference) throws ExecutionException, InterruptedException {
        String authorString = "**" + StringUtil.escapeMarkdown(member.getEffectiveName()) + "**";
        if (onlySelfReference) {
            return getAloneGif(authorString);
        }

        if (containsBlockedUsers(member.getIdLong(), mention.getElementList())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_blocked"));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_footer").replace("{PREFIX}", getPrefix()));
            return eb;
        }

        return generateEmbedSunshineCase(member, mention, authorString, quote);
    }

    protected EmbedBuilder generateEmbedSunshineCase(Member member, Mention mention, String authorString, String quote) throws ExecutionException, InterruptedException {
        EmbedBuilder eb;
        if (mention.getElementList().isEmpty()) {
            eb = EmbedFactory.getEmbedDefault(this, getString("template_single", authorString) + quote);
        } else {
            eb = EmbedFactory.getEmbedDefault(this, getString("template", mention.isMultiple(), mention.getMentionText(), authorString) + quote);
        }

        eb.setImage(gifUrl);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_footer").replace("{PREFIX}", getPrefix()));
        return eb;
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

    private EmbedBuilder getAloneGif(String authorString) {
        String gif;
        String text;
        if (this instanceof BiteCommand) {
            gif = "https://cdn.discordapp.com/attachments/499629904380297226/958012189875241061/bite.gif";
            text = getString("themselves", authorString);
        } else {
            gif = "https://cdn.discordapp.com/attachments/736277561373491265/736277600053493770/hug.gif";
            text = TextManager.getString(getLocale(), Category.INTERACTIONS, "alone");
        }
        return EmbedFactory.getEmbedDefault(this, text)
                .setImage(gif);
    }

}
