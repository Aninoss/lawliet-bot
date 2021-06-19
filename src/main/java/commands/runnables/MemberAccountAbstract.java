package commands.runnables;

import java.util.Locale;
import commands.Command;
import core.TextManager;
import core.mention.MentionList;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class MemberAccountAbstract extends Command {

    private final boolean includeNotInGuild;
    private boolean found = false;

    public MemberAccountAbstract(Locale locale, String prefix) {
        this(locale, prefix, false);
    }

    public MemberAccountAbstract(Locale locale, String prefix, boolean includeNotInGuild) {
        super(locale, prefix);
        this.includeNotInGuild = includeNotInGuild;
    }

    protected void setFound() {
        found = true;
    }

    protected EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        return null;
    }

    protected EmbedBuilder processUser(GuildMessageReceivedEvent event, User user, boolean userIsAuthor, String args) throws Throwable {
        return null;
    }

    protected void sendMessage(TextChannel channel, MessageEmbed eb) {
        channel.sendMessageEmbeds(eb).queue();
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        boolean userMentioned = false;
        EmbedBuilder eb;

        if (includeNotInGuild) {
            User user = event.getMember().getUser();
            MentionList<User> userMention = MentionUtil.getUsers(event.getMessage(), args);

            if (userMention.getList().size() > 0) {
                user = userMention.getList().get(0);
                userMentioned = true;
            } else if (args.length() > 0) {
                userMention = MentionUtil.getUsersFromString(args, true).get();
                if (userMention.getList().size() > 0) {
                    user = userMention.getList().get(0);
                    userMentioned = true;
                }
            }

            eb = processUser(event, user, user.getIdLong() == event.getMember().getIdLong(), userMention.getFilteredArgs());
        } else {
            Member member = event.getMember();
            MentionList<Member> memberMention = MentionUtil.getMembers(event.getMessage(), args);

            if (memberMention.getList().size() > 0) {
                member = memberMention.getList().get(0);
                userMentioned = true;
            }

            eb = processMember(event, member, member.getIdLong() == event.getMember().getIdLong(), memberMention.getFilteredArgs());
        }

        if (eb != null) {
            if (!userMentioned) {
                EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                if (args.length() > 0 && !found) {
                    EmbedUtil.addNoResultsLog(eb, getLocale(), args);
                }
            }

            sendMessage(event.getChannel(), eb.build());
        }

        return true;
    }

}
