package commands.runnables.informationcategory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.ExternalLinks;
import constants.Settings;
import core.EmbedFactory;
import core.PatreonData;
import core.ShardManager;
import core.cache.PatreonCache;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "patreon",
        emoji = "\uD83D\uDCB3",
        executableWithoutArgs = true,
        aliases = { "donate", "donation", "premium" }
)
public class PatreonCommand extends Command {

    private final long[] USER_ID_NOT_VISIBLE = {
            397209883793162240L,
            272037078919938058L
    };

    private PatreonData patreonData;

    public PatreonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        patreonData = PatreonCache.getInstance().getAsync();

        String content = getString("info",
                ExternalLinks.PATREON_PAGE,
                ExternalLinks.UNLOCK_SERVER_WEBSITE,
                getString("status", PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), false)),
                StringUtil.getEmojiForBoolean(PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong()))
        );

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, content)
                .setImage("https://cdn.discordapp.com/attachments/499629904380297226/763202405474238464/Patreon_Banner_New.png")
                .addBlankField(false);

        addLoadingReactionInstantly();
        StringBuilder sb = new StringBuilder();
        for (int i = Settings.PATREON_ROLE_IDS.length - 1; i >= 3; i--) {
            sb.append(getPatreonUsersString(i));
        }
        sb.append(getString("andmanymore"));

        eb.addField(getString("slot_title"), sb.toString(), false);
        event.getChannel().sendMessage(eb.build()).queue();
        return true;
    }

    private String getPatreonUsersString(int patreonTier) {
        StringBuilder patreonUsers = new StringBuilder();

        patreonData.getUserMap().keySet().stream()
                .filter(userId -> patreonData.getUserMap().get(userId) == patreonTier + 1 && Arrays.stream(USER_ID_NOT_VISIBLE).noneMatch(uid -> uid == userId))
                .map(userId -> ShardManager.getInstance().fetchUserById(userId).join())
                .filter(Objects::nonNull)
                .forEach(user -> {
                    String value = getString("slot_value", patreonTier);
                    patreonUsers.append(getString("slot", StringUtil.escapeMarkdown(user.getAsTag()), value))
                            .append("\n");
                });

        return patreonUsers.toString();
    }

}
