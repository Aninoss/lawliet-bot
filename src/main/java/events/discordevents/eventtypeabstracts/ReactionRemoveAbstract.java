package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ReactionRemoveAbstract extends DiscordEventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReactionRemoveAbstract.class);
    
    public abstract boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable;

    public static void onReactionRemoveStatic(ReactionRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getMessage().isEmpty() && !event.getChannel().canYouReadMessageHistory())
            return;

        //todo debug
        User user = event.getUser();
        /*user user;
        if (event.getserver().ispresent() && event.getuser().isempty()) {
            user = discordapicollection.getinstance().fetchuserbyid(event.getserver().get(), event.getuser().getid()).get();
            logger.info("### user fetched {} ###", user.getid());
        } else {
            user =  event.getUser();
        }*/

        execute(listenerList, user, false,
                listener -> ((ReactionRemoveAbstract) listener).onReactionRemove(event)
        );
    }

}
