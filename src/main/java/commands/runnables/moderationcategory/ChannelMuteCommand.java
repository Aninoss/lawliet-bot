package commands.runnables.moderationcategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.mention.Mention;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import modules.Mod;
import modules.mute.MuteData;
import modules.mute.MuteManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "chmute",
        userGuildPermissions = { Permission.MANAGE_ROLES },
        botGuildPermissions = { Permission.MESSAGE_WRITE },
        emoji = "\uD83D\uDED1",
        executableWithoutArgs = false,
        aliases = { "channelmute", "mute" }
)
public class ChannelMuteCommand extends Command {

    private final boolean mute;

    public ChannelMuteCommand(Locale locale, String prefix) {
        super(locale, prefix);
        this.mute = true;
    }

    public ChannelMuteCommand(Locale locale, String prefix, boolean mute) {
        super(locale, prefix);
        this.mute = mute;
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Message message = event.getMessage();
        Guild guild = event.getGuild();

        TextChannel channel = event.getChannel();
        List<TextChannel> channelList = MentionUtil.getTextChannels(message, args).getList();
        if (channelList.size() > 0) {
            channel = channelList.get(0);
        }

        EmbedBuilder errorEmbed = BotPermissionUtil.getBotPermissionMissingEmbed(
                getLocale(),
                channel,
                getAdjustedBotGuildPermissions(),
                new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS }
        );
        if (errorEmbed != null) {
            message.getChannel().sendMessage(errorEmbed.build()).queue();
            return false;
        }

        List<Member> memberList = MentionUtil.getMembers(message, args).getList();
        if (memberList.size() == 0) {
            message.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions")).build()
            ).queue();
            return false;
        }

        ArrayList<Member> successfulMembers = new ArrayList<>();
        for (Member member : memberList) {
            if (!BotPermissionUtil.can(member, Permission.ADMINISTRATOR)) {
                successfulMembers.add(member);
            }
        }

        if (successfulMembers.size() == 0) {
            message.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "admin_block")).build()
            ).queue();
            return false;
        }

        MuteData muteData = new MuteData(channel, successfulMembers);
        boolean doneSomething = MuteManager.getInstance().executeMute(muteData, mute);

        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedMembers(getLocale(), memberList);
        EmbedBuilder actionEmbed = EmbedFactory.getEmbedDefault(
                this,
                getString("action", mention.isMultiple(), mention.getMentionText(), event.getMember().getAsMention(), channel.getAsMention())
        );

        if (doneSomething) {
            Mod.postLogMembers(this, actionEmbed, guild, memberList).join();
        }

        if (!mute || !successfulMembers.contains(guild.getSelfMember()) || channel.getIdLong() != event.getChannel().getIdLong()) {
            EmbedBuilder eb;

            if (doneSomething) {
                eb = EmbedFactory.getEmbedDefault(
                        this,
                        getString("success_description", mention.isMultiple(), mention.getMentionText(), channel.getAsMention())
                );
            } else {
                eb = EmbedFactory.getEmbedError(
                        this,
                        getString("nothingdone", mention.isMultiple(), mention.getMentionText(), channel.getAsMention())
                );
            }

            event.getChannel().sendMessage(eb.build()).queue();
        }

        return true;
    }

}