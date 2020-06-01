package DiscordEvents;

import DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import MySQL.Modules.BannedUsers.DBBannedUsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public abstract class DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(DiscordEventAbstract.class);

    private final DiscordEventAnnotation discordEventAnnotation;

    public DiscordEventAbstract() {
        discordEventAnnotation = this.getClass().getAnnotation(DiscordEventAnnotation.class);
    }

    public EventPriority getPriority() { return discordEventAnnotation.priority(); }

    public boolean isAllowingBannedUser() { return discordEventAnnotation.allowBannedUser(); }

    public static boolean userIsBanned(long userId) {
        try {
            return DBBannedUsers.getInstance().getBean().getUserIds().contains(userId);
        } catch (SQLException throwables) {
            LOGGER.error("SQL error", throwables);
            return true;
        }
    }

}
