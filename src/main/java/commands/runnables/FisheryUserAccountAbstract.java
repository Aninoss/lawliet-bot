package commands.runnables;

import core.EmbedFactory;
import core.TextManager;
import core.mention.MentionList;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;

public abstract class FisheryUserAccountAbstract extends FisheryAbstract {

    public FisheryUserAccountAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }
    private boolean found = false;

    @Override
    protected boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        MentionList<User> userMention = MentionUtil.getUsers(message,followedString);
        ArrayList<User> list = userMention.getList();

        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
            return false;
        }
        boolean userMentioned = true;
        boolean userBefore = list.size() > 0;
        list.removeIf(User::isBot);
        if (list.size() == 0) {
            if (userBefore) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "nobot"))).get();
                return false;
            } else {
                list.add(message.getUserAuthor().get());
                userMentioned = false;
            }
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
