package commands.runnables.utilitycategory;

import java.util.List;
import java.util.Locale;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticReactionAddListener;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import constants.Response;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.atomicassets.MentionableAtomicAsset;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Ticket;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

@CommandProperties(
        trigger = "ticket",
        botGuildPermissions = { Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL },
        userGuildPermissions = { Permission.MANAGE_CHANNEL },
        emoji = "🎟️",
        executableWithoutArgs = true
)
public class TicketCommand extends NavigationAbstract implements OnStaticReactionAddListener {

    private final static int MAX_ROLES = 10;
    private final static int
            MAIN = 0,
            ANNOUNCEMENT_CHANNEL = 1,
            ADD_STAFF_ROLE = 2,
            REMOVE_STAFF_ROLE = 3,
            CREATE_TICKET_MESSAGE = 4;

    private TicketData ticketData;
    private NavigationHelper<AtomicRole> staffRoleNavigationHelper;
    private CustomObservableList<AtomicRole> staffRoles;
    private TextChannel tempPostChannel = null;

    public TicketCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
        staffRoles = AtomicRole.transformIdList(event.getGuild(), ticketData.getStaffRoleIds());
        staffRoleNavigationHelper = new NavigationHelper<>(this, staffRoles, AtomicRole.class, MAX_ROLES);
        registerNavigationListener(7);
        return true;
    }

    @ControllerMessage(state = ADD_STAFF_ROLE)
    public Response onMessageAddStaffRole(GuildMessageReceivedEvent event, String input) {
        List<Role> roleList = MentionUtil.getRoles(event.getMessage(), input).getList();
        return staffRoleNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMember(), MAIN);
    }

    @ControllerMessage(state = ANNOUNCEMENT_CHANNEL)
    public Response onMessageAnnouncementChannel(GuildMessageReceivedEvent event, String input) {
        List<TextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), input).getList();
        if (channelList.size() == 0) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            return Response.FALSE;
        } else {
            TextChannel textChannel = channelList.get(0);

            String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), textChannel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS);
            if (channelMissingPerms != null) {
                setLog(LogStatus.FAILURE, channelMissingPerms);
                return Response.FALSE;
            }

            ticketData.setAnnouncementTextChannelId(textChannel.getIdLong());
            setLog(LogStatus.SUCCESS, getString("announcement_set"));
            setState(MAIN);
            return Response.TRUE;
        }
    }

    @ControllerMessage(state = CREATE_TICKET_MESSAGE)
    public Response onMessageCreateTicketMessage(GuildMessageReceivedEvent event, String input) {
        List<TextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), input).getList();
        if (channelList.size() == 0) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            return Response.FALSE;
        } else {
            tempPostChannel = channelList.get(0);
            return Response.TRUE;
        }
    }

    @ControllerReaction(state = MAIN)
    public boolean onReactionMain(GenericGuildMessageReactionEvent event, int i) {
        switch (i) {
            case -1:
                removeNavigationWithMessage();
                return false;

            case 0:
                setState(ANNOUNCEMENT_CHANNEL);
                return true;

            case 1:
                staffRoleNavigationHelper.startDataAdd(ADD_STAFF_ROLE);
                return true;

            case 2:
                staffRoleNavigationHelper.startDataRemove(REMOVE_STAFF_ROLE);
                return true;

            case 3:
                setState(CREATE_TICKET_MESSAGE);
                return true;

            default:
                return false;
        }
    }

    @ControllerReaction(state = ANNOUNCEMENT_CHANNEL)
    public boolean onReactionAnnouncementChannel(GenericGuildMessageReactionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(MAIN);
                return true;
            }
            case 0 -> {
                ticketData.setAnnouncementTextChannelId(null);
                setLog(LogStatus.SUCCESS, getString("announcement_set"));
                setState(MAIN);
                return true;
            }
        }
        return false;
    }

    @ControllerReaction(state = ADD_STAFF_ROLE)
    public boolean onReactionAddStaffRole(GenericGuildMessageReactionEvent event, int i) {
        if (i == -1) {
            setState(0);
            return true;
        }
        return false;
    }

    @ControllerReaction(state = REMOVE_STAFF_ROLE)
    public boolean onReactionRemoveStaffRole(GenericGuildMessageReactionEvent event, int i) {
        return staffRoleNavigationHelper.removeData(i, MAIN);
    }

    @ControllerReaction(state = CREATE_TICKET_MESSAGE)
    public boolean onReactionCreateTicketMessage(GenericGuildMessageReactionEvent event, int i) {
        if (i == -1) {
            setState(0);
            return true;
        } else if (i == 0 && tempPostChannel != null) {
            tempPostChannel = tempPostChannel.getGuild().getTextChannelById(tempPostChannel.getIdLong());
            if (tempPostChannel != null) {
                String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), tempPostChannel,
                        Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY);
                if (channelMissingPerms != null) {
                    setLog(LogStatus.FAILURE, channelMissingPerms);
                    return true;
                }

                Category parent = tempPostChannel.getParent();
                if (parent != null) {
                    String categoryMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), parent, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL);
                    if (categoryMissingPerms != null) {
                        setLog(LogStatus.FAILURE, categoryMissingPerms);
                        return true;
                    }
                }

                String emoji = getCommandProperties().emoji();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("message_content", emoji));
                eb.setFooter(null);

                tempPostChannel.sendMessage(eb.build()).queue(message -> {
                    message.addReaction(emoji).queue();
                    DBStaticReactionMessages.getInstance().retrieve(message.getGuild().getIdLong())
                            .put(message.getIdLong(), new StaticReactionMessageData(message, getTrigger()));
                });

                setLog(LogStatus.SUCCESS, getString("message_sent"));
                setState(MAIN);
            }
            return true;
        }
        return false;
    }

    @Draw(state = MAIN)
    public EmbedBuilder onDrawMain() {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setOptions(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mannouncement"), StringUtil.escapeMarkdown(ticketData.getAnnouncementTextChannel().map(GuildChannel::getAsMention).orElse(notSet)), true)
                .addField(getString("state0_mstaffroles"), new ListGen<AtomicRole>().getList(staffRoles, getLocale(), MentionableAtomicAsset::getAsMention), true);
    }

    @Draw(state = ANNOUNCEMENT_CHANNEL)
    public EmbedBuilder onDrawAnnouncementChannel() {
        setOptions(getString("state1_options").split("\n"));
        return staffRoleNavigationHelper.drawDataAdd(getString("state1_title"), getString("state1_description"));
    }

    @Draw(state = ADD_STAFF_ROLE)
    public EmbedBuilder onDrawAddStaffRole() {
        return staffRoleNavigationHelper.drawDataAdd(getString("state2_title"), getString("state2_description"));
    }

    @Draw(state = REMOVE_STAFF_ROLE)
    public EmbedBuilder onDrawRemoveStaffRole() {
        return staffRoleNavigationHelper.drawDataRemove(getString("state3_title"), getString("state3_description"));
    }

    @Draw(state = CREATE_TICKET_MESSAGE)
    public EmbedBuilder onDrawCreateTicketMessage() {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (tempPostChannel != null) {
            setOptions(getString("state4_options").split("\n"));
        }
        return EmbedFactory.getEmbedDefault(this,
                getString("state4_description", tempPostChannel != null ? tempPostChannel.getAsMention() : notSet),
                getString("state4_title")
        );
    }

    @Override
    public void onStaticReactionAdd(Message message, GuildMessageReactionAddEvent event) throws Throwable {
        Ticket.createTicketChannel(event.getChannel(), event.getMember());
    }

}
