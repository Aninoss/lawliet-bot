package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import Commands.FisheryAbstract;
import Constants.Permission;
import Constants.Settings;
import Core.EmbedFactory;
import Core.Mention.MentionUtil;
import Core.TextManager;
import Core.Utils.StringUtil;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "treasure",
        userPermissions = Permission.MANAGE_SERVER,
        botPermissions = Permission.MANAGE_MESSAGES,
        emoji = "\uD83C\uDFF4\u200D☠️",
        executable = true,
        aliases = {"tresure", "treasurechest", "schatz"},
        exlusiveServers = { 619548671276744704L }
)
public class TreasureCommand extends FisheryAbstract {

    @Override
    protected boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        int amount = 1;
        if (followedString.length() > 0 && StringUtil.stringIsInt(followedString)) {
            amount = Integer.parseInt(followedString);
            if (amount < 1 || amount > 50) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "50"))).get();
                return false;
            }
        }

        event.getMessage().delete().get();
        for(int i = 0; i < amount; i++) spawnTreasureChest(event.getServer().get().getId(), event.getServerTextChannel().get());
        return true;
    }

    public static void spawnTreasureChest(long serverId, ServerTextChannel channel) throws ExecutionException, InterruptedException {
        ServerBean serverBean = DBServer.getInstance().getBean(serverId);
        Locale locale = serverBean.getLocale();
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_title") + Settings.EMPTY_EMOJI)
                .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_desription", FisheryCommand.keyEmoji))
                .setImage("https://cdn.discordapp.com/attachments/711665837114654781/711665915355201576/treasure_closed.png");

        Message message = channel.sendMessage(eb).get();
        message.addReaction(FisheryCommand.keyEmoji);
    }

}
