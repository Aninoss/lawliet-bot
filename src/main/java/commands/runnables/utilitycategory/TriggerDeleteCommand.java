package commands.runnables.utilitycategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import commands.Command;
import core.EmbedFactory;
import core.utils.DiscordUtil;
import core.utils.StringUtil;
import mysql.modules.guild.DBGuild;

import java.util.Locale;

@CommandProperties(
        trigger = "triggerdelete",
        userPermissions = PermissionDeprecated.MANAGE_SERVER | PermissionDeprecated.MANAGE_MESSAGES,
        emoji = "\uD83D\uDDD1",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "triggerremove", "starterremove", "startermessagedelete", "startermessageremove", "messagedelete", "messageremove", "starterdelete" }
)
public class TriggerDeleteCommand extends Command implements OnReactionAddListener {

    private Message message;

    private final String[] activeArgs = new String[]{"off", "on"};

    public TriggerDeleteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        if (args.length() > 0) {
            int option = -1;
            for(int i=0; i < activeArgs.length; i++) {
                String str = activeArgs[i];
                if (args.equalsIgnoreCase(str)) option = i;
            }

            if (option == -1) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("invalid", args)));
                return false;
            }

            boolean active = option == 1;
            DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setCommandAuthorMessageRemove(active);
            event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("set", active))).get();
            return true;
        } else {
            String onOffText = StringUtil.getOnOffForBoolean(getLocale(), DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).isCommandAuthorMessageRemove());
            message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("reaction", onOffText))).get();
            for(int i = 0; i < 2; i++) {
                message.addReaction(StringUtil.getEmojiForBoolean(i == 1));
            }
            return true;
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        for(int i = 0; i < 2; i++) {
            String str = StringUtil.getEmojiForBoolean(i == 1);
            if (DiscordUtil.emojiIsString(event.getEmoji(), str)) {
                boolean active = i == 1;
                DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setCommandAuthorMessageRemove(active);
                getReactionMessage().edit(EmbedFactory.getEmbedDefault(this, getString("set", active))).get();
                removeReactionListener(getReactionMessage());
                return;
            }
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}

}
