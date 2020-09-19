package commands.runnables.informationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import constants.Settings;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.PatreonCache;
import core.utils.StringUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "patreon",
        emoji = "\uD83D\uDCB3",
        executable = true,
        aliases = {"donate", "donation", "premium"}
)
public class PatreonCommand extends Command {

    public PatreonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("info", Settings.PATREON_PAGE))
                .setImage("https://cdn.discordapp.com/attachments/499629904380297226/756955408115171348/Patreon_Banner_New.png")
                .addField(Settings.EMPTY_EMOJI, Settings.EMPTY_EMOJI);

        StringBuilder sb = new StringBuilder();
        for(int i = 4; i >= 2; i--)
            sb.append(getPatreonUsersString(i));
        sb.append(getString("andmanymore"));

        eb.addField(getString("slot_title"), sb.toString());
        EmbedFactory.addLog(eb, null, getString("status", PatreonCache.getInstance().getPatreonLevel(event.getMessageAuthor().getId())));
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private String getPatreonUsersString(int patreonTier) {
        StringBuilder donators = new StringBuilder();
        Role role = DiscordApiCollection.getInstance().getServerById(Settings.SUPPORT_SERVER_ID).get().getRoleById(Settings.PATREON_ROLE_IDS[patreonTier]).get();
        role.getUsers().forEach(user -> {
            String value = getString("slot_value", patreonTier);
            donators.append(getString("slot", StringUtil.escapeMarkdown(user.getDiscriminatedName()), value))
                    .append("\n");
        });

        return donators.toString();
    }

}
