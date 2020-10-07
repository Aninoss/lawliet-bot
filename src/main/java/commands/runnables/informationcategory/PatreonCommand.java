package commands.runnables.informationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import constants.AssetIds;
import constants.Emojis;
import constants.ExternalLinks;
import constants.Settings;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.PatreonCache;
import core.utils.StringUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Arrays;
import java.util.Locale;

@CommandProperties(
        trigger = "patreon",
        emoji = "\uD83D\uDCB3",
        executable = true,
        aliases = {"donate", "donation", "premium"}
)
public class PatreonCommand extends Command {

    private final long[] USER_ID_NOT_VISIBLE = {
            397209883793162240L
    };

    public PatreonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("info", ExternalLinks.PATREON_PAGE))
                .setImage("https://cdn.discordapp.com/attachments/499629904380297226/763202405474238464/Patreon_Banner_New.png")
                .addField(Emojis.EMPTY_EMOJI, Emojis.EMPTY_EMOJI);

        StringBuilder sb = new StringBuilder();
        for(int i = Settings.PATREON_ROLE_IDS.length - 1; i >= 3; i--)
            sb.append(getPatreonUsersString(i));
        sb.append(getString("andmanymore"));

        eb.addField(getString("slot_title"), sb.toString());
        EmbedFactory.addLog(eb, null, getString("status", PatreonCache.getInstance().getPatreonLevel(event.getMessageAuthor().getId())));
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private String getPatreonUsersString(int patreonTier) {
        StringBuilder patreonUsers = new StringBuilder();
        Role role = DiscordApiCollection.getInstance().getServerById(AssetIds.SUPPORT_SERVER_ID).get().getRoleById(Settings.PATREON_ROLE_IDS[patreonTier]).get();
        role.getUsers().stream()
                .filter(user -> Arrays.stream(USER_ID_NOT_VISIBLE).noneMatch(userIdBlock -> user.getId() == userIdBlock))
                .forEach(user -> {
            String value = getString("slot_value", patreonTier);
            patreonUsers.append(getString("slot", StringUtil.escapeMarkdown(user.getDiscriminatedName()), value))
                    .append("\n");
        });

        return patreonUsers.toString();
    }

}
