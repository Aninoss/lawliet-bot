package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionListener;
import core.EmbedFactory;
import core.utils.StringUtil;
import mysql.modules.autoclaim.AutoClaimBean;
import mysql.modules.autoclaim.DBAutoClaim;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "autoclaim",
        emoji = "\uD83E\uDD16",
        patreonRequired = true,
        executableWithoutArgs = true
)
public class AutoClaimCommand extends Command implements OnReactionListener {

    private final String[] activeArgs = new String[]{ "off", "on" };

    public AutoClaimCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Member member = event.getMember();
        AutoClaimBean autoClaimBean = DBAutoClaim.getInstance().retrieve();

        if (args.length() > 0) {
            int option = -1;
            for(int i = 0; i < activeArgs.length; i++) {
                String str = activeArgs[i];
                if (args.equalsIgnoreCase(str)) option = i;
            }

            if (option == -1) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("invalid", args)).build()).queue();
                return false;
            }

            boolean active = option == 1;
            autoClaimBean.setActive(member.getIdLong(), active);
            event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("set", active, member.getAsMention())).build()).queue();
            return true;
        } else {
            String onOffText = StringUtil.getOnOffForBoolean(getLocale(), autoClaimBean.isActive(member.getIdLong()));
            message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("reaction", onOffText)).build()).get();
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
                DBAutoClaim.getInstance().retrieve().setActive(event.getUserId(), active);
                getReactionMessage().edit(EmbedFactory.getEmbedDefault(this, getString("set", active, event.getUser().get().getMentionTag()))).get();
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
