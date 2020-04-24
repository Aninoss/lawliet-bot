package Commands.ModerationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.Mention.MentionTools;
import Core.TextManager;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
import MySQL.Modules.Warning.DBServerWarnings;
import MySQL.Modules.Warning.ServerWarningsBean;
import MySQL.Modules.Warning.ServerWarningsSlot;
import javafx.util.Pair;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@CommandProperties(
        trigger = "warnlog",
        emoji = "\uD83D\uDCDD",
        executable = true
)
public class WarnLogCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
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
            ServerWarningsBean serverWarningsBean = DBServerWarnings.getInstance().getBean(new Pair<>(server.getId(), user.getId()));

            StringBuilder latestWarnings = new StringBuilder();

            List<ServerWarningsSlot> slots = serverWarningsBean.getLatest(3);
            Collections.reverse(slots);
            for(ServerWarningsSlot serverWarningsSlot: slots) {
                Optional<User> requestor = serverWarningsSlot.getRequesterUser();
                Optional<String> reason = serverWarningsSlot.getReason();
                String userString = requestor.isPresent() ? (server.getMembers().contains(requestor.get()) ? requestor.get().getMentionTag() : String.format("**%s**", requestor.get().getName())) : getString("unknown_user");
                String timeDiffString = TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), serverWarningsSlot.getTime(), true);
                latestWarnings.append(getString("latest_slot", reason.isPresent(), userString, timeDiffString, reason.orElse(getString("noreason"))));
            }

            String latestWarningsString = latestWarnings.toString();
            if (latestWarningsString.isEmpty()) latestWarningsString = TextManager.getString(getLocale(), TextManager.GENERAL, "empty");

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                    .setAuthor(user)
                    .setThumbnail(user.getAvatar().getUrl().toString());
            eb.addField(getString("latest"), latestWarningsString, false);
            eb.addField(getString("amount"), getString("amount_template",
                    StringUtil.numToString(serverWarningsBean.getAmountLatest(24, ChronoUnit.HOURS).size()),
                    StringUtil.numToString(serverWarningsBean.getAmountLatest(7, ChronoUnit.DAYS).size()),
                    StringUtil.numToString(serverWarningsBean.getAmountLatest(30, ChronoUnit.DAYS).size()),
                    StringUtil.numToString(serverWarningsBean.getWarnings().size())
            ), false);
            if (!userMentioned) eb.setFooter(TextManager.getString(getLocale(),TextManager.GENERAL,"mention_optional"));
            event.getChannel().sendMessage(eb).get();
        }

        return true;
    }

}
