package commands.runnables.informationcategory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.PatreonData;
import core.ShardManager;
import core.cache.PatreonCache;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "premium",
        emoji = "\uD83D\uDCB3",
        executableWithoutArgs = true,
        aliases = { "donate", "donation", "patreon" }
)
public class PremiumCommand extends Command {

    private final long[] USER_ID_NOT_VISIBLE = {
            397209883793162240L,
            272037078919938058L
    };

    private PatreonData patreonData;

    public PremiumCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(CommandEvent event, String args) {
        patreonData = PatreonCache.getInstance().getAsync();

        String content = getString("info",
                StringUtil.getOnOffForBoolean(event.getChannel(), getLocale(), PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), false)),
                StringUtil.getOnOffForBoolean(event.getChannel(), getLocale(), PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong()))
        );

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, content)
                .setImage("https://cdn.discordapp.com/attachments/499629904380297226/763202405474238464/Patreon_Banner_New.png")
                .addBlankField(false);

        addLoadingReactionInstantly();

        eb.addField(getString("slot_title"), getPatreonUsersString() + getString("andmanymore"), false);
        setComponents(EmbedFactory.getPatreonBlockButtons(getLocale()));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private String getPatreonUsersString() {
        StringBuilder patreonUsers = new StringBuilder();

        patreonData.getHighPayingUserList().stream()
                .filter(userId -> Arrays.stream(USER_ID_NOT_VISIBLE).noneMatch(uid -> uid == userId))
                .map(userId -> ShardManager.fetchUserById(userId).join())
                .filter(Objects::nonNull)
                .forEach(user -> {
                    patreonUsers.append(getString("slot", StringUtil.escapeMarkdown(user.getAsTag())))
                            .append("\n");
                });

        return patreonUsers.toString();
    }

}
