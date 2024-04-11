package commands.runnables.configurationcategory;

import commands.Command;
import commands.CommandContainer;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicGuildMessageChannel;
import core.modals.StringModalBuilder;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@CommandProperties(
        trigger = "ccshortcuts",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🔗",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = {"commandchannelshortcuts"}
)
public class CommandChannelShortcutsCommand extends NavigationAbstract {

    private static final int
            STATE_ADD = 1,
            STATE_ADJUST_CHANNEL = 2,
            STATE_DELETE = 3;

    private AtomicGuildMessageChannel atomicChannel;
    private String trigger;

    public CommandChannelShortcutsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_ADJUST_CHANNEL, STATE_ADD, getString("add_channel"))
                        .setMinMax(1, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                        .setSingleGetter(() -> atomicChannel != null ? atomicChannel.getIdLong() : null)
                        .setSingleSetter(channelId -> atomicChannel = new AtomicGuildMessageChannel(event.getGuild().getIdLong(), channelId))
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
                atomicChannel = null;
                trigger = null;
                setState(STATE_ADD);
                return true;
            }
            case 1 -> {
                if (!getGuildEntity().getCommandChannelShortcuts().isEmpty()) {
                    setState(STATE_DELETE);
                } else {
                    setLog(LogStatus.FAILURE, getString("error_noentries"));
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = STATE_DELETE)
    public boolean onButtonEdit(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        GuildEntity guildEntity = getGuildEntity();
        long channelId = new ArrayList<>(guildEntity.getCommandChannelShortcuts().keySet()).get(i);
        AtomicGuildMessageChannel atomicChannel = new AtomicGuildMessageChannel(event.getGuild().getIdLong(), channelId);

        guildEntity.beginTransaction();
        guildEntity.getCommandChannelShortcuts().remove(channelId);
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.COMMAND_CHANNEL_SHORTCUTS_DELETE, event.getMember(), channelId);
        guildEntity.commitTransaction();

        setLog(LogStatus.SUCCESS, getString("log_deleted", StringUtil.escapeMarkdownInField(atomicChannel.getPrefixedName(getLocale()))));
        if (guildEntity.getCommandChannelShortcuts().isEmpty()) {
            setState(DEFAULT_STATE);
        }
        return true;
    }

    @ControllerButton(state = STATE_ADD)
    public boolean onButtonAdd(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(DEFAULT_STATE);
                return true;
            }
            case 0 -> {
                setState(STATE_ADJUST_CHANNEL);
                return true;
            }
            case 1 -> {
                Modal modal = new StringModalBuilder(this, getString("add_command"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, 30)
                        .setGetter(() -> trigger)
                        .setSetterOptionalLogs(value -> {
                            Class<? extends Command> commandClass = CommandContainer.getCommandMap().get(value);
                            if (commandClass == null ||
                                    Command.getCommandProperties(commandClass).exclusiveGuilds().length != 0 ||
                                    Command.getCommandProperties(commandClass).exclusiveUsers().length != 0
                            ) {
                                setLog(LogStatus.FAILURE, getString("log_invalid_trigger", StringUtil.escapeMarkdownInField(value)));
                                return false;
                            }

                            this.trigger = Command.getCommandProperties(commandClass).trigger();
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                if (trigger == null || atomicChannel == null) {
                    return true;
                }

                GuildEntity guildEntity = getGuildEntity();
                Map<Long, String> shortcuts = guildEntity.getCommandChannelShortcuts();

                if (shortcuts.containsKey(atomicChannel.getIdLong())) {
                    setLog(LogStatus.FAILURE, getString("log_channel_exist"));
                    return true;
                }

                GuildMessageChannel channel = atomicChannel.get().orElse(null);
                if (channel == null) {
                    atomicChannel = null;
                    return true;
                }

                guildEntity.beginTransaction();
                shortcuts.put(atomicChannel.getIdLong(), trigger);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.COMMAND_CHANNEL_SHORTCUTS_ADD, event.getMember(), atomicChannel.getIdLong());
                guildEntity.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("log_add", StringUtil.escapeMarkdownInField(atomicChannel.getPrefixedName(getLocale()))));
                setState(DEFAULT_STATE);
                return true;
            }
        }

        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawDefault(Member member) {
        setComponents(getString("default_options").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("default_desc")
        ).addField(getString("default_list_title"), StringUtil.shortenString(generateShortcutList(member.getGuild().getIdLong()), MessageEmbed.VALUE_MAX_LENGTH), false);
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder onDrawAdd(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        String[] options = getString("add_options").split("\n");
        if (atomicChannel == null || trigger == null) {
            options[2] = "";
        }
        setComponents(options, new int[]{2}, new int[0]);

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString("add_desc"), getString("add_title")
        );
        eb.addField(getString("add_channel"), atomicChannel != null ? atomicChannel.getPrefixedNameInField(getLocale()) : notSet, true);
        eb.addField(getString("add_command"), trigger != null ? "`" + trigger + "`" : notSet, true);
        return eb;
    }

    @Draw(state = STATE_DELETE)
    public EmbedBuilder onDrawEdit(Member member) {
        String[] options = getGuildEntity()
                .getCommandChannelShortcuts()
                .keySet()
                .stream()
                .map(channelId -> new AtomicGuildMessageChannel(member.getGuild().getIdLong(), channelId).getPrefixedName(getLocale()))
                .toArray(String[]::new);
        setComponents(options);

        return EmbedFactory.getEmbedDefault(
                this,
                getString("delete_desc"), getString("delete_title")
        );
    }

    private String generateShortcutList(long guildId) {
        return new ListGen<Map.Entry<Long, String>>().getList(getGuildEntity().getCommandChannelShortcuts().entrySet(), getLocale(), set -> {
            return new AtomicGuildMessageChannel(guildId, set.getKey()).getPrefixedNameInField(getLocale()) + " → `" + set.getValue() + "`";
        });
    }

}
