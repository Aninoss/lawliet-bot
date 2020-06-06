package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import Commands.FisheryAbstract;
import Constants.Category;
import Constants.Permission;
import Constants.Settings;
import Core.EmbedFactory;
import Core.TextManager;
import Core.Utils.StringUtil;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "treasure",
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83C\uDFF4\u200D☠️",
        executable = true,
        patreonRequired = true,
        aliases = { "tresure", "treasurechest" }
)
public class TreasureCommand extends FisheryAbstract {

    @Override
    protected boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        int amount = 1;
        if (followedString.length() > 0) {
            if (StringUtil.stringIsInt(followedString)) {
                amount = Integer.parseInt(followedString);
                if (amount < 1 || amount > 30) {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30"))).get();
                    return false;
                }
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"))).get();
                return false;
            }
        }

        for(int i = 0; i < amount; i++) spawnTreasureChest(event.getServer().get().getId(), event.getServerTextChannel().get());
        return true;
    }

    public static void spawnTreasureChest(long serverId, ServerTextChannel channel) throws ExecutionException, InterruptedException {
        ServerBean serverBean = DBServer.getInstance().getBean(serverId);
        Locale locale = serverBean.getLocale();
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(locale, Category.FISHERY, "fishery_treasure_title") + Settings.EMPTY_EMOJI)
                .setDescription(TextManager.getString(locale, Category.FISHERY, "fishery_treasure_desription", FisheryCommand.keyEmoji))
                .setImage("https://cdn.discordapp.com/attachments/711665837114654781/711665915355201576/treasure_closed.png");

        Message message = channel.sendMessage(eb).get();
        message.addReaction(FisheryCommand.keyEmoji);
    }

}
