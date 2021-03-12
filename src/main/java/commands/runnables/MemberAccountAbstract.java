package commands.runnables;

import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import core.TextManager;
import core.mention.MentionList;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class MemberAccountAbstract extends Command {

    private boolean found = false;

    public MemberAccountAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected void setFound() {
        found = true;
    }

    protected abstract EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable;

    protected void sendMessage(TextChannel channel, MessageEmbed eb) {
        channel.sendMessage(eb).queue();
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        Message message = event.getMessage();
        MentionList<Member> userMention = MentionUtil.getMembers(message, args);
        ArrayList<Member> list = new ArrayList<>(userMention.getList());

        boolean userMentioned = true;
        if (list.size() == 0) {
            list.add(message.getMember());
            userMentioned = false;
        }

        Member member = list.get(0);
        EmbedBuilder eb = processMember(event, member, member.getIdLong() == event.getMember().getIdLong(), userMention.getFilteredArgs());
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
