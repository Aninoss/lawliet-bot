package commands.runnables.configurationcategory;

import commands.Command;
import commands.CommandContainer;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicTextChannel;
import core.modals.ModalMediator;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

    private AtomicTextChannel channel;
    private String trigger;

    public CommandChannelShortcutsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
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
                channel = null;
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
        AtomicTextChannel atomicTextChannel = new AtomicTextChannel(event.getGuild().getIdLong(), channelId);

        guildEntity.beginTransaction();
        guildEntity.getCommandChannelShortcuts().remove(channelId);
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.COMMAND_CHANNEL_SHORTCUTS_DELETE, event.getMember(), channelId);
        guildEntity.commitTransaction();

        setLog(LogStatus.SUCCESS, getString("log_deleted", StringUtil.escapeMarkdownInField(atomicTextChannel.getPrefixedName(getLocale()))));
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
                String textId = "trigger";
                TextInput message = TextInput.create(textId, getString("add_command"), TextInputStyle.SHORT)
                        .setValue(trigger)
                        .setMinLength(1)
                        .setMaxLength(30)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("add_adjustcommand"), e -> {
                            String trigger = e.getValue(textId).getAsString().toLowerCase();
                            Class<? extends Command> commandClass = CommandContainer.getCommandMap().get(trigger);
                            if (commandClass == null ||
                                    Command.getCommandProperties(commandClass).exclusiveGuilds().length != 0 ||
                                    Command.getCommandProperties(commandClass).exclusiveUsers().length != 0
                            ) {
                                setLog(LogStatus.FAILURE, getString("log_invalid_trigger", StringUtil.escapeMarkdownInField(trigger)));
                                return null;
                            }

                            this.trigger = Command.getCommandProperties(commandClass).trigger();
                            return null;
                        }).addActionRows(ActionRow.of(message))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                if (trigger == null || channel == null) {
                    return true;
                }

                GuildEntity guildEntity = getGuildEntity();
                Map<Long, String> shortcuts = guildEntity.getCommandChannelShortcuts();

                if (shortcuts.containsKey(channel.getIdLong())) {
                    setLog(LogStatus.FAILURE, getString("log_channel_exist"));
                    return true;
                }

                TextChannel textChannel = channel.get().orElse(null);
                if (textChannel == null) {
                    channel = null;
                    return true;
                }

                guildEntity.beginTransaction();
                shortcuts.put(channel.getIdLong(), trigger);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.COMMAND_CHANNEL_SHORTCUTS_ADD, event.getMember(), channel.getIdLong());
                guildEntity.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("log_add", StringUtil.escapeMarkdownInField(channel.getPrefixedName(getLocale()))));
                setState(DEFAULT_STATE);
                return true;
            }
        }

        return true;
    }

    @ControllerButton(state = STATE_ADJUST_CHANNEL)
    public boolean onButtonAdjustChannel(ButtonInteractionEvent event, int i) {
        setState(STATE_ADD);
        return true;
    }

    @ControllerEntitySelectMenu(state = STATE_ADJUST_CHANNEL)
    public boolean onEntitySelectMenuAdjustChannel(EntitySelectInteractionEvent event) {
        TextChannel channel = (TextChannel) event.getMentions().getChannels().get(0);
        if (!BotPermissionUtil.canWrite(channel)) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_send", "#" + StringUtil.escapeMarkdownInField(channel.getName())));
            return true;
        }

        this.channel = new AtomicTextChannel(channel);
        setState(STATE_ADD);
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

        String[] options = channel == null || trigger == null
                ? getString("add_options").split("\n")
                : getString("add_options2").split("\n");
        setComponents(options);

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString("add_desc"), getString("add_title")
        );
        eb.addField(getString("add_channel"), channel != null ? channel.getPrefixedNameInField(getLocale()) : notSet, true);
        eb.addField(getString("add_command"), trigger != null ? "`" + trigger + "`" : notSet, true);
        return eb;
    }

    @Draw(state = STATE_ADJUST_CHANNEL)
    public EmbedBuilder onDrawSetChannel(Member member) {
        EntitySelectMenu memberSelectMenu = EntitySelectMenu.create("add_channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(ChannelType.TEXT)
                .setRequiredRange(1, 1)
                .build();

        setComponents(memberSelectMenu);
        return EmbedFactory.getEmbedDefault(this)
                .setTitle(getString("channel_title"));
    }

    @Draw(state = STATE_DELETE)
    public EmbedBuilder onDrawEdit(Member member) {
        String[] options = getGuildEntity()
                .getCommandChannelShortcuts()
                .keySet()
                .stream()
                .map(channelId -> new AtomicTextChannel(member.getGuild().getIdLong(), channelId).getPrefixedName(getLocale()))
                .toArray(String[]::new);
        setComponents(options);

        return EmbedFactory.getEmbedDefault(
                this,
                getString("delete_desc"), getString("delete_title")
        );
    }

    private String generateShortcutList(long guildId) {
        return new ListGen<Map.Entry<Long, String>>().getList(getGuildEntity().getCommandChannelShortcuts().entrySet(), getLocale(), set -> {
            return new AtomicTextChannel(guildId, set.getKey()).getPrefixedNameInField(getLocale()) + " → `" + set.getValue() + "`";
        });
    }

}
