package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import Constants.Permission;
import Core.Mention.MentionList;
import Core.Mention.MentionUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Ban;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "unban",
        botPermissions = Permission.BAN_MEMBERS,
        userPermissions = Permission.BAN_MEMBERS,
        emoji = "\uD83C\uDF3C",
        executable = false
)
public class UnbanCommand extends WarnCommand  {

    private final static Logger LOGGER = LoggerFactory.getLogger(UnbanCommand.class);

    public UnbanCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected MentionList<User> getMentionList(Message message, String followedString) throws ExecutionException, InterruptedException {
        List<User> users = message.getServer().get().getBans().get().stream().map(Ban::getUser).collect(Collectors.toList());
        return MentionUtil.getUsers(message, followedString, users);
    }

    @Override
    public void process(Server server, User user) throws Throwable {
        try {
            server.unbanUser(user, reason).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Exception on ban", e);
            server.unbanUser(user).get();
        }
    }

    @Override
    protected boolean sendDM() {
        return false;
    }

    @Override
    protected boolean sendWarning() {
        return false;
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return true;
    }
    
}
