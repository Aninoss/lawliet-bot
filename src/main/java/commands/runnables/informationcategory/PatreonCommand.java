package commands.runnables.informationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import constants.Emojis;
import constants.ExternalLinks;
import constants.Settings;
import core.ShardManager;
import core.EmbedFactory;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

@CommandProperties(
        trigger = "patreon",
        emoji = "\uD83D\uDCB3",
        executableWithoutArgs = true,
        aliases = { "donate", "donation" }
)
public class PatreonCommand extends Command {

    private final long[] USER_ID_NOT_VISIBLE = {
            397209883793162240L
    };

    private HashMap<Long, Integer> userTiers;

    public PatreonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        userTiers = PatreonCache.getInstance().getAsync();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("info", ExternalLinks.PATREON_PAGE))
                .setImage("https://cdn.discordapp.com/attachments/499629904380297226/763202405474238464/Patreon_Banner_New.png")
                .addField(Emojis.EMPTY_EMOJI, Emojis.EMPTY_EMOJI);

        StringBuilder sb = new StringBuilder();
        for(int i = Settings.PATREON_ROLE_IDS.length - 1; i >= 3; i--)
            sb.append(getPatreonUsersString(i));
        sb.append(getString("andmanymore"));

        eb.addField(getString("slot_title"), sb.toString());
        EmbedUtil.addLog(eb, null, getString("status", PatreonCache.getInstance().getUserTier(event.getMessageAuthor().getId())));
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private String getPatreonUsersString(int patreonTier) {
        StringBuilder patreonUsers = new StringBuilder();

        userTiers.keySet().stream()
                .filter(userId -> userTiers.get(userId) == patreonTier + 1 && Arrays.stream(USER_ID_NOT_VISIBLE).noneMatch(uid -> uid == userId))
                .map(userId -> ShardManager.getInstance().fetchUserById(userId).join())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(user -> {
                    String value = getString("slot_value", patreonTier);
                    patreonUsers.append(getString("slot", StringUtil.escapeMarkdown(user.getDiscriminatedName()), value))
                            .append("\n");
                });

        return patreonUsers.toString();
    }

}
