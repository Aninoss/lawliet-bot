package commands.runnables;

import java.util.Locale;
import commands.Command;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;

public abstract class UserAccountAbstract extends Command {

    private boolean found = false;
    public UserAccountAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        MentionList<User> userMention = MentionUtil.getMembers(message,followedString);
        ArrayList<User> list = userMention.getList();

        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
            return false;
        }
        boolean userMentioned = true;
        if (list.size() == 0) {
            list.add(message.getUserAuthor().get());
            userMentioned = false;
        }

        init(event, userMention.getResultMessageString());
        for(User user: list) {
            EmbedBuilder eb = generateUserEmbed(event.getServer().get(), user, user.getId() == event.getMessageAuthor().getId(), followedString);
            if (eb != null) {
                if (!userMentioned) {
                    EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                    if (followedString.length() > 0 && !found)
                        EmbedUtil.addNoResultsLog(eb, getLocale(), followedString);
                }

                Message messageNew = event.getChannel().sendMessage(eb).get();
                afterMessageSend(messageNew, user, user.getId() == event.getMessageAuthor().getId());
            }
        }
        return true;
    }

    protected void setFound() {
        found = true;
    }

    protected abstract EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable;

    protected void init(MessageCreateEvent event, String followedString) throws Throwable {}

    protected void afterMessageSend(Message message, User user, boolean userIsAuthor) throws Throwable { }

}
