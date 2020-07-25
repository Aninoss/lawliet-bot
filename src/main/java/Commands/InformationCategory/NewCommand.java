package Commands.InformationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import Constants.Settings;
import Constants.TrackerResult;
import Core.EmbedFactory;
import Core.TextManager;
import Core.Utils.BotUtil;
import Core.Utils.StringUtil;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import MySQL.Modules.Version.DBVersion;
import MySQL.Modules.Version.VersionBean;
import MySQL.Modules.Version.VersionBeanSlot;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "new",
        emoji = "\uD83C\uDD95",
        executable = true,
        aliases = {"changelog"}
)
public class NewCommand extends Command implements OnTrackerRequestListener {

    VersionBean versionBean;

    public NewCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

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

    private EmbedBuilder getEmbedNormal(List<VersionBeanSlot> versions, boolean showEmptyFooter) {
        EmbedBuilder eb = getVersionsEmbed(versions, showEmptyFooter);
        EmbedFactory.addTrackerNote(getLocale(), eb, getPrefix(), getTrigger());
        return eb;
    }

    private EmbedBuilder getVersionsEmbed(VersionBeanSlot slot) {
        ArrayList<VersionBeanSlot> versions = new ArrayList<>();
        versions.add(slot);
        return getVersionsEmbed(versions, false);
    }

    private EmbedBuilder getVersionsEmbed(List<VersionBeanSlot> versions, boolean showEmptyFooter) {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);
        if (showEmptyFooter) eb.setFooter(getString("footer"));
        for(int i = versions.size() - 1; i >= 0; i--) {
            VersionBeanSlot slot = versions.get(i);
            eb.addField(slot.getVersion(), ("• "+TextManager.getString(getLocale(), TextManager.VERSIONS, slot.getVersion())).replace("\n", "\n• ").replace("%PREFIX", getPrefix()));
        }
        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        if (!slot.getArgs().isPresent() || !slot.getArgs().get().equals(BotUtil.getCurrentVersion())) {
            VersionBeanSlot newestSlot = DBVersion.getInstance().getBean().getCurrentVersion();

            if (slot.getServerId() != Settings.SUPPORT_SERVER_ID)
                slot.getChannel().get().sendMessage(getVersionsEmbed(newestSlot));
            else {
                Role role = slot.getServer().get().getRoleById(703879430799622155L).get();
                slot.getChannel().get().sendMessage(role.getMentionTag(), getVersionsEmbed(newestSlot));
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
