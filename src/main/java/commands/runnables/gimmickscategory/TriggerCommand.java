package commands.runnables.gimmickscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.utils.StringUtil;
import modules.graphics.TriggerGraphics;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.io.InputStream;
import java.util.Locale;

@CommandProperties(
        trigger = "trigger",
        botChannelPermissions = Permission.MESSAGE_ATTACH_FILES,
        emoji = "\uD83D\uDCA2",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "triggered" }
)
public class TriggerCommand extends MemberAccountAbstract {

    private InputStream inputStream;

    public TriggerCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        deferReply();
        inputStream = TriggerGraphics.createImageTriggered(member.getUser());
        return EmbedFactory.getEmbedDefault(this, getString("template", StringUtil.escapeMarkdown(member.getEffectiveName())))
                .setImage("attachment://trigger.gif");
    }

    @Override
    protected void sendMessage(Member member, GuildMessageChannel channel, EmbedBuilder eb) {
        addFileAttachment(inputStream, "trigger.gif");
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
    }

}
