package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import core.EmbedFactory;
import core.TextManager;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Fishery;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "treasure",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83C\uDFF4\u200D☠️",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "tresure", "treasurechest" }
)
public class TreasureCommand extends Command implements FisheryInterface {

    public TreasureCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) {
        TextChannel channel = event.getChannel();
        MentionList<TextChannel> channelMention = MentionUtil.getTextChannels(event.getMessage(), args);
        if (channelMention.getList().size() > 0) {
            channel = channelMention.getList().get(0);
            args = channelMention.getFilteredArgs().trim();
            if (!BotPermissionUtil.canWriteEmbed(channel)) {
                String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", channel.getAsMention());
                event.getChannel().sendMessage(
                        EmbedFactory.getEmbedError(this, error).build()
                ).queue();
                return false;
            }
        }

        int amount = 1;
        if (args.length() > 0) {
            if (StringUtil.stringIsInt(args)) {
                amount = Integer.parseInt(args);
                if (amount < 1 || amount > 30) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")
                            ).build()
                    ).queue();
                    return false;
                }
            } else {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(
                        this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")
                        ).build()
                ).queue();
                return false;
            }
        }

        for (int i = 0; i < amount; i++) {
            Fishery.spawnTreasureChest(channel);
        }
        return true;
    }

}
