package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.Mention.MentionTools;
import General.TextManager;
import General.StringTools;
import General.TimeTools;
import General.Warnings.UserWarnings;
import General.Warnings.WarningSlot;
import MySQL.DBServerOld;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

@CommandProperties(
        trigger = "warnlog",
        emoji = "\uD83D\uDCDD",
        executable = true
)
public class WarnLogCommand extends Command implements onRecievedListener {

    public WarnLogCommand() {
        super();
    }

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        Message message = event.getMessage();
        ArrayList<User> list = MentionTools.getUsers(message,followedString).getList();
        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
            return false;
        }
        boolean userMentioned = true;
        if (list.size() == 0) {
            list.add(message.getUserAuthor().get());
            userMentioned = false;
        }
        for (User user: list) {
            UserWarnings userWarnings = DBServerOld.getWarningsForUser(server, user);

            StringBuilder latestWarnings = new StringBuilder();
            for(WarningSlot warningSlot: userWarnings.getLatest(3)) {
                Optional<User> requestor = warningSlot.getRequestor();
                Optional<String> reason = warningSlot.getReason();
                String userString = requestor.isPresent() ? (server.getMembers().contains(requestor.get()) ? requestor.get().getMentionTag() : String.format("**%s**", requestor.get().getName())) : getString("unknown_user");
                String timeDiffString = TimeTools.getRemainingTimeString(getLocale(), Instant.now(), warningSlot.getTime(), true);
                latestWarnings.append(getString("latest_slot", reason.isPresent(), userString, timeDiffString, reason.orElse(getString("noreason"))));
            }

            String latestWarningsString = latestWarnings.toString();
            if (latestWarningsString.isEmpty()) latestWarningsString = TextManager.getString(getLocale(), TextManager.GENERAL, "empty");

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                    .setAuthor(user)
                    .setThumbnail(user.getAvatar().getUrl().toString());
            eb.addField(getString("latest"), latestWarningsString, false);
            eb.addField(getString("amount"), getString("amount_template",
                    StringTools.numToString(userWarnings.amountLatestHours(24)),
                    StringTools.numToString(userWarnings.amountLatestDays(7)),
                    StringTools.numToString(userWarnings.amountLatestDays(30)),
                    StringTools.numToString(userWarnings.amountTotal())
            ), false);
            if (!userMentioned) eb.setFooter(TextManager.getString(getLocale(),TextManager.GENERAL,"mention_optional"));
            event.getChannel().sendMessage(eb).get();
        }

        return true;
    }

}
