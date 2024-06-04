package commands.runnables.configurationcategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicGuildChannel;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "whitelist",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "✅",
        executableWithoutArgs = true,
        aliases = {"wl"},
        obsolete = true
)
public class WhiteListCommand extends NavigationAbstract {

    public static final int MAX_CHANNELS = EntitySelectMenu.OPTIONS_MAX_AMOUNT;

    public static final int STATE_CHANNELS = 1;

    public WhiteListCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        setLog(LogStatus.WARNING, TextManager.getString(getLocale(), Category.CONFIGURATION, "cperms_obsolete", getPrefix()));

        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_CHANNELS, DEFAULT_STATE, getString("state0_mchannel"))
                        .setMinMax(1, MAX_CHANNELS)
                        .setChannelTypes(Collections.emptyList())
                        .setLogEvent(BotLogEntity.Event.CHANNEL_WHITELIST)
                        .setGetter(() -> getGuildEntity().getWhitelistedChannelIds())
                        .setSetter(channelIds -> {
                            List<Long> whitelistedChannelIds = getGuildEntity().getWhitelistedChannelIds();
                            whitelistedChannelIds.clear();
                            whitelistedChannelIds.addAll(channelIds);
                        })
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                setState(1);
                return true;
            }
            case 1 -> {
                List<Long> whitelistedChannelIds = getGuildEntity().getWhitelistedChannelIds();
                if (!whitelistedChannelIds.isEmpty()) {
                    getEntityManager().getTransaction().begin();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.CHANNEL_WHITELIST, event.getMember(), null, whitelistedChannelIds);
                    whitelistedChannelIds.clear();
                    getEntityManager().getTransaction().commit();

                    setLog(LogStatus.SUCCESS, getString("channelcleared"));
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "element_start_remove_none_channel"));
                }
                return true;
            }
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        String everyChannel = getString("all");
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(
                        getString("state0_mchannel"),
                        new ListGen<Long>().getList(getGuildEntity().getWhitelistedChannelIds(), everyChannel, channelId -> new AtomicGuildChannel(member.getGuild().getIdLong(), channelId).getPrefixedNameInField(getLocale())),
                        true
                );
    }

}
