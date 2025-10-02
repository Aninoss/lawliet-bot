package commands.runnables.informationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.Emojis;
import constants.Settings;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.PatreonData;
import core.ShardManager;
import core.cache.PatreonCache;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

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
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        patreonData = PatreonCache.getInstance().getAsync();

        String content = getString("info",
                StringUtil.getOnOffForBoolean(event.getMessageChannel(), getLocale(), PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), false)),
                StringUtil.getOnOffForBoolean(event.getMessageChannel(), getLocale(), PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong()))
        ) + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, content)
                .setImage("https://cdn.discordapp.com/attachments/499629904380297226/763202405474238464/Patreon_Banner_New.png");

        deferReply();
        StringBuilder sb = new StringBuilder();
        for (int i = Settings.PATREON_ROLE_IDS.length - 1; i >= 3; i--) {
            sb.append(getPatreonUsersString(i));
        }
        sb.append(getString("andmanymore"));

        eb.addField(getString("slot_title"), sb.toString(), false);
        setComponents(EmbedFactory.getPatreonBlockButton(getLocale()));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private String getPatreonUsersString(int patreonTier) {
        StringBuilder patreonUsers = new StringBuilder();

        patreonData.getUserTierMap().keySet().stream()
                .filter(userId -> patreonData.getUserTierMap().get(userId) == patreonTier + 1 && Arrays.stream(USER_ID_NOT_VISIBLE).noneMatch(uid -> uid == userId))
                .map(userId -> ShardManager.fetchUserById(userId).join())
                .filter(Objects::nonNull)
                .forEach(user -> {
                    String value = getString("slot_value", patreonTier);
                    patreonUsers.append(getString("slot", StringUtil.escapeMarkdown(user.getName()), value))
                            .append("\n");
                });

        return patreonUsers.toString();
    }

}
