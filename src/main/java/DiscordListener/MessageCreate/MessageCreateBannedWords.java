package DiscordListener.MessageCreate;

import CommandSupporters.CommandManager;
import Commands.ModerationCategory.BannedWordsCommand;
import Commands.ModerationCategory.ModSettingsCommand;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.PermissionCheck;
import Core.TextManager;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerPriority;
import DiscordListener.ListenerTypeAbstracts.MessageCreateAbstract;
import Modules.BannedWordsCheck;
import MySQL.Modules.BannedWords.BannedWordsBean;
import MySQL.Modules.BannedWords.DBBannedWords;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@DiscordListenerAnnotation(priority = ListenerPriority.HIGH)
public class MessageCreateBannedWords extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return BannedWordsCheck.check(event.getMessage());
    }

}
