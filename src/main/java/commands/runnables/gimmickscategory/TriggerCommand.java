package commands.runnables.gimmickscategory;

import java.io.InputStream;
import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import modules.graphics.TriggerGraphics;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "trigger",
        botChannelPermissions = Permission.MESSAGE_ATTACH_FILES,
        emoji = "\uD83D\uDCA2",
        executableWithoutArgs = true,
        aliases = { "triggered" }
)
public class TriggerCommand extends MemberAccountAbstract {

    private InputStream inputStream;

    public TriggerCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        addLoadingReactionInstantly();
        inputStream = TriggerGraphics.createImageTriggered(member.getUser());
        return EmbedFactory.getEmbedDefault(this, getString("template", member.getEffectiveName()))
                .setImage("attachment://trigger.gif");
    }

    @Override
    protected void sendMessage(TextChannel channel, MessageEmbed eb) {
        channel.sendMessage(eb)
                .addFile(inputStream, "trigger.gif")
                .queue();
    }

}
