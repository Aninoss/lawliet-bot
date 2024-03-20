package commands.runnables;

import commands.Command;
import commands.CommandEvent;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.MentionList;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public abstract class MemberAccountAbstract extends Command {

    private final boolean includeNotInGuild;
    private final boolean requireMemberMention;
    private final boolean allowBots;
    private boolean found = false;

    public MemberAccountAbstract(Locale locale, String prefix) {
        this(locale, prefix, false, false, true);
    }

    public MemberAccountAbstract(Locale locale, String prefix, boolean includeNotInGuild, boolean requireMemberMention, boolean allowBots) {
        super(locale, prefix);
        this.includeNotInGuild = includeNotInGuild;
        this.requireMemberMention = requireMemberMention;
        this.allowBots = allowBots;
    }

    protected void setFound() {
        found = true;
    }

    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        return null;
    }

    protected EmbedBuilder processUser(CommandEvent event, User user, boolean userIsAuthor, String args) throws Throwable {
        return null;
    }

    protected void sendMessage(Member member, GuildMessageChannel channel, EmbedBuilder eb) throws Throwable {
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        boolean userMentioned = false;
        EmbedBuilder eb;

        if (includeNotInGuild) {
            User user = requireMemberMention ? null : event.getMember().getUser();
            MentionList<User> userMention = MentionUtil.getUsers(event.getGuild(), args, event.getRepliedMember());

            if (!userMention.getList().isEmpty()) {
                user = userMention.getList().get(0);
                userMentioned = true;
            } else if (!args.isEmpty()) {
                userMention = MentionUtil.getUsersFromString(args, true).get();
                if (!userMention.getList().isEmpty()) {
                    user = userMention.getList().get(0);
                    userMentioned = true;
                }
            }

            if (user == null) {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions_no_bots")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
            if (user.isBot() && !allowBots) {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_bots")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }

            eb = processUser(event, user, user.getIdLong() == event.getMember().getIdLong(), userMention.getFilteredArgs());
        } else {
            Member member = requireMemberMention ? null : event.getMember();
            MentionList<Member> memberMention = MentionUtil.getMembers(event.getGuild(), args, event.getRepliedMember());

            if (memberMention.getList().size() > 0) {
                member = memberMention.getList().get(0);
                userMentioned = true;
            }

            if (member == null) {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions_no_bots")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
            if (member.getUser().isBot() && !allowBots) {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_bots")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }

            eb = processMember(event, member, member.getIdLong() == event.getMember().getIdLong(), memberMention.getFilteredArgs());
        }

        if (eb != null) {
            if (!userMentioned) {
                EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                if (!args.isEmpty() && !found) {
                    EmbedUtil.addNoResultsLog(eb, getLocale(), args);
                }
            }

            sendMessage(event.getMember(), event.getMessageChannel(), eb);
        }

        return true;
    }

}
