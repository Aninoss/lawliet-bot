package Commands.InformationCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.TrackerResult;
import Core.*;
import Core.Utils.StringUtil;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import MySQL.Modules.Version.DBVersion;
import MySQL.Modules.Version.VersionBean;
import MySQL.Modules.Version.VersionBeanSlot;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "new",
        emoji = "\uD83C\uDD95",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/new-icon.png",
        executable = true,
        aliases = {"changelog"}
)
public class NewCommand extends Command implements OnTrackerRequestListener {

    VersionBean versionBean;

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        versionBean = DBVersion.getInstance().getBean();

        //Ohne Argumente
        if (followedString.length() == 0) {
            List<VersionBeanSlot> versions = versionBean.getCurrentVersions(3);
            event.getChannel().sendMessage(getEmbedNormal(versions, true)).get();
            return true;
        } else {
            //Anzahl
            if (StringUtil.stringIsLong(followedString)) {
                int i = Integer.parseInt(followedString);
                if (i >= 1) {
                    if (i <= 10) {
                        List<VersionBeanSlot> versions = versionBean.getCurrentVersions(i);
                        event.getChannel().sendMessage(getEmbedNormal(versions, false)).get();
                        return true;
                    } else {
                        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                                TextManager.getString(getLocale(), TextManager.GENERAL,"too_large", "10"))).get();
                        return false;
                    }
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL,"too_small", "1"))).get();
                    return false;
                }
            } else {
                List<String> askVersions = Arrays.stream(followedString.split(" ")).filter(str -> !str.isEmpty()).collect(Collectors.toList());
                List<VersionBeanSlot> versions = versionBean.getSlots().stream().filter(slot -> askVersions.contains(slot.getVersion())).collect(Collectors.toList());

                if (versions.size() > 0) {
                    event.getChannel().sendMessage(getEmbedNormal(versions, false)).get();
                    return true;
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString))).get();
                    return false;
                }
            }
        }
    }

    private EmbedBuilder getEmbedNormal(List<VersionBeanSlot> versions, boolean showEmptyFooter) throws Throwable {
        EmbedBuilder eb = getVersionsEmbed(versions, showEmptyFooter);
        EmbedFactory.addLog(eb, LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "tracker", getPrefix(), getTrigger()));
        return eb;
    }

    private EmbedBuilder getVersionsEmbed(VersionBeanSlot slot) throws Throwable {
        ArrayList<VersionBeanSlot> versions = new ArrayList<>();
        versions.add(slot);
        return getVersionsEmbed(versions, false);
    }

    private EmbedBuilder getVersionsEmbed(List<VersionBeanSlot> versions, boolean showEmptyFooter) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);
        if (showEmptyFooter) eb.setFooter(getString("footer"));
        for(VersionBeanSlot slot: versions) {
            eb.addField(slot.getVersion(), ("• "+TextManager.getString(getLocale(), TextManager.VERSIONS, slot.getVersion())).replace("\n", "\n• ").replace("%PREFIX", getPrefix()));
        }
        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        if (!slot.getArgs().isPresent() || !slot.getArgs().get().equals(StringUtil.getCurrentVersion())) {
            VersionBeanSlot newestSlot = DBVersion.getInstance().getBean().getCurrentVersion();

            slot.getChannel().get().sendMessage(getVersionsEmbed(newestSlot));
            slot.setArgs(newestSlot.getVersion());
            return TrackerResult.STOP_AND_SAVE;
        }

        return TrackerResult.STOP;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
