package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.atomicassets.AtomicTextChannel;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.Fishery;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

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
    public boolean onFisheryAccess(CommandEvent event, String args) {
        TextChannel channel = event.getTextChannel();
        MentionList<TextChannel> channelMention = MentionUtil.getTextChannels(event.getGuild(), args);
        if (channelMention.getList().size() > 0) {
            channel = channelMention.getList().get(0);
            args = channelMention.getFilteredArgs().trim();
        }

        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", new AtomicTextChannel(channel).getPrefixedNameInField(getLocale()));
            drawMessageNew(EmbedFactory.getEmbedError(this, error)).exceptionally(ExceptionLogger.get());
            return false;
        }

        int amount = 1;
        if (args.length() > 0) {
            if (StringUtil.stringIsInt(args)) {
                amount = Integer.parseInt(args);
                if (amount < 1 || amount > 30) {
                    drawMessageNew(EmbedFactory.getEmbedError(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")
                    )).exceptionally(ExceptionLogger.get());
                    return false;
                }
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(
                        this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")
                )).exceptionally(ExceptionLogger.get());
                return false;
            }
        }

        for (int i = 0; i < amount; i++) {
            Fishery.spawnTreasureChest(channel);
        }
        drawMessageNew(EmbedFactory.getEmbedDefault(this, getString("success", amount != 1, StringUtil.numToString(amount), new AtomicTextChannel(channel).getPrefixedNameInField(getLocale()))));
        return true;
    }

}
