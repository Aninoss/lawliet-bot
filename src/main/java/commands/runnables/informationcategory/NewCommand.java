package commands.runnables.informationcategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.AssetIds;
import constants.TrackerResult;
import core.EmbedFactory;
import core.TextManager;
import core.utils.BotUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import mysql.modules.tracker.TrackerSlot;
import mysql.modules.version.DBVersion;
import mysql.modules.version.VersionBean;
import mysql.modules.version.VersionBeanSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "new",
        emoji = "\uD83C\uDD95",
        executableWithoutArgs = true,
        aliases = { "changelog" }
)
public class NewCommand extends Command implements OnAlertListener {

    VersionBean versionBean;

    public NewCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        versionBean = DBVersion.getInstance().retrieve();

        // without args
        if (args.length() == 0) {
            List<VersionBeanSlot> versions = versionBean.getCurrentVersions(3);
            event.getChannel().sendMessage(getEmbedNormal(event.getMember(), versions, true).build()).queue();
            return true;
        } else {
            // number
            if (StringUtil.stringIsLong(args)) {
                int i = Integer.parseInt(args);
                if (i >= 1) {
                    if (i <= 10) {
                        List<VersionBeanSlot> versions = versionBean.getCurrentVersions(i);
                        event.getChannel().sendMessage(
                                getEmbedNormal(event.getMember(), versions, false).build()
                        ).queue();
                        return true;
                    } else {
                        event.getChannel().sendMessage(
                                EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_large", "10")).build()
                        ).queue();
                        return false;
                    }
                } else {
                    event.getChannel().sendMessage(
                            EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1")).build()
                    ).queue();
                    return false;
                }
            } else {
                List<String> askVersions = Arrays.stream(args.split(" ")).filter(str -> !str.isEmpty()).collect(Collectors.toList());
                List<VersionBeanSlot> versions = versionBean.getSlots().stream().filter(slot -> askVersions.contains(slot.getVersion())).collect(Collectors.toList());

                if (versions.size() > 0) {
                    event.getChannel().sendMessage(
                            getEmbedNormal(event.getMember(), versions, false).build()
                    ).queue();
                    return true;
                } else {
                    event.getChannel().sendMessage(
                            EmbedFactory.getEmbedError(this, TextManager.getNoResultsString(getLocale(), args)).build()
                    ).queue();
                    return false;
                }
            }
        }
    }

    private EmbedBuilder getEmbedNormal(Member member, List<VersionBeanSlot> versions, boolean showEmptyFooter) {
        EmbedBuilder eb = getVersionsEmbed(versions, showEmptyFooter);
        EmbedUtil.addTrackerNoteLog(getLocale(), member, eb, getPrefix(), getTrigger());
        return eb;
    }

    private EmbedBuilder getVersionsEmbed(VersionBeanSlot slot) {
        ArrayList<VersionBeanSlot> versions = new ArrayList<>();
        versions.add(slot);
        return getVersionsEmbed(versions, false);
    }

    private EmbedBuilder getVersionsEmbed(List<VersionBeanSlot> versions, boolean showEmptyFooter) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        if (showEmptyFooter) EmbedUtil.setFooter(eb, this, getString("footer"));
        for (int i = versions.size() - 1; i >= 0; i--) {
            VersionBeanSlot slot = versions.get(i);
            String versionsString = TextManager.getString(getLocale(), TextManager.VERSIONS, slot.getVersion());
            eb.addField(
                    slot.getVersion(),
                    ("• " + versionsString).replace("\n", "\n• ").replace("%PREFIX", getPrefix()),
                    false
            );
        }
        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerSlot slot) throws Throwable {
        if (slot.getArgs().isEmpty() || !slot.getArgs().get().equals(BotUtil.getCurrentVersion())) {
            VersionBeanSlot newestSlot = DBVersion.getInstance().retrieve().getCurrentVersion();

            slot.sendMessage(getVersionsEmbed(newestSlot).build());

            if (slot.getGuildId() == AssetIds.SUPPORT_SERVER_ID) {
                Role role = slot.getGuild().get().getRoleById(703879430799622155L);
                slot.getTextChannel().get().sendMessage(role.getAsMention())
                        .flatMap(Message::delete).queue();
            }

            slot.setArgs(BotUtil.getCurrentVersion());
            return TrackerResult.STOP_AND_SAVE;
        }

        return TrackerResult.STOP;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
