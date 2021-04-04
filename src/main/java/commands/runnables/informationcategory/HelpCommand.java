package commands.runnables.informationcategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.runnables.NavigationAbstract;
import commands.runnables.PornPredefinedAbstract;
import commands.runnables.PornSearchAbstract;
import constants.*;
import core.Program;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.emojiconnection.BackEmojiConnection;
import core.emojiconnection.EmojiConnection;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import mysql.modules.commandmanagement.CommandManagementBean;
import mysql.modules.commandmanagement.DBCommandManagement;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "help",
        emoji = "❕",
        executableWithoutArgs = true,
        aliases = { "commands" }
)
public class HelpCommand extends NavigationAbstract {

    private final String[] LIST = new String[] {
            Category.GIMMICKS,
            Category.AI_TOYS,
            Category.CONFIGURATION,
            Category.UTILITY,
            Category.MODERATION,
            Category.INFORMATION,
            Category.FISHERY_SETTINGS,
            Category.FISHERY,
            Category.CASINO,
            Category.EMOTES,
            Category.INTERACTIONS,
            Category.EXTERNAL,
            Category.NSFW,
            Category.PATREON_ONLY,
            Category.SPLATOON_2
    };

    private ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
    private String searchTerm;
    private CommandManagementBean commandManagementBean;
    private String currentCategory = null;

    public HelpCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        searchTerm = args;
        commandManagementBean = DBCommandManagement.getInstance().retrieve(event.getGuild().getIdLong());
        EmbedBuilder commandEmbed = checkCommand(event.getChannel(), args);
        EmbedBuilder categoryEmbed = checkCategory(event.getChannel(), args);
        if (commandEmbed == null || categoryEmbed != null) {
            registerNavigationListener(LIST.length);
        } else {
            drawMessage(commandEmbed);
        }
        return true;
    }

    @ControllerMessage(state = DEFAULT_STATE)
    public Response onMessage(GuildMessageReceivedEvent event, String input) {
        searchTerm = input;
        if (emojiConnections.stream().anyMatch(c -> c.getConnection().equalsIgnoreCase(input))) {
            searchTerm = input;
            return Response.TRUE;
        }
        return null;
    }

    @ControllerReaction(state = DEFAULT_STATE)
    public boolean onReaction(GenericGuildMessageReactionEvent event, int i) {
        for (EmojiConnection emojiConnection : emojiConnections) {
            if (emojiConnection.isEmoji(event.getReactionEmote()) || (i == -1 && emojiConnection instanceof BackEmojiConnection)) {
                searchTerm = emojiConnection.getConnection();

                if (searchTerm.equals("quit")) {
                    removeNavigationWithMessage();
                    return false;
                }

                if (searchTerm.startsWith("exec:")) {
                    String className = searchTerm.split(":")[1];
                    Command command = CommandManager.createCommandByClassName(className, getLocale(), getPrefix());

                    CommandManager.manage(getGuildMessageReceivedEvent().get(), command, "", Instant.now());
                    return false;
                }

                return true;
            }
        }

        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDraw() {
        String arg = searchTerm.trim();
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1, arg.length() - 1);

        TextChannel channel = getTextChannel().get();
        setOptions(null);

        EmbedBuilder eb;
        if ((eb = checkCategory(channel, arg)) == null) {
            if ((eb = checkCommand(channel, arg)) == null) {
                eb = checkMainPage(channel);
                if (arg.length() > 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), arg));
                }
            }
        }

        return eb;
    }

    private EmbedBuilder checkCommand(TextChannel channel, String arg) {
        boolean noArgs = false;
        if (getAttachments().has("noargs")) {
            getAttachments().remove("noargs");
            noArgs = true;
        }

        for (Class<? extends Command> clazz : CommandContainer.getInstance().getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();

            if ((commandTrigger.equalsIgnoreCase(arg) || Arrays.stream(command.getCommandProperties().aliases()).anyMatch(arg::equalsIgnoreCase)) &&
                    !commandTrigger.equals(getTrigger())
            ) {
                if (currentCategory == null) {
                    currentCategory = command.getCategory();
                }

                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(BotPermissionUtil.can(channel, Permission.MESSAGE_EXT_EMOJI), currentCategory));

                StringBuilder usage = new StringBuilder();
                for (String line : TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_usage").split("\n")) {
                    usage.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                }

                StringBuilder examples = new StringBuilder();
                int exampleNumber = 0;
                for (String line : TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_examples").split("\n")) {
                    line = StringUtil.solveVariablesOfCommandText(line, getGuildMessageReceivedEvent().get().getMessage(), getPrefix());
                    examples.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                    exampleNumber++;
                }

                String addNotExecutable = "";
                if (command.getCommandProperties().executableWithoutArgs()) {
                    setOptions(getString("command_execute").split("\n"));
                    emojiConnections.add(new EmojiConnection(Emojis.LETTERS[0], "exec:" + command.getClass().getName()));
                }

                String permissionsList = new ListGen<Permission>().getList(
                        List.of(command.getUserPermissions()),
                        getLocale(),
                        ListGen.SLOT_TYPE_BULLET,
                        permission -> TextManager.getString(getLocale(), TextManager.PERMISSIONS, permission.name())
                );

                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(
                                TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                        TextManager.getString(getLocale(), TextManager.COMMANDS, currentCategory) + " » " +
                                        command.getCommandProperties().emoji() + " " + TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_title")
                        )
                        .setDescription(TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_helptext") + addNotExecutable)
                        .addField(Emojis.EMPTY_EMOJI, getString("command_usage") + "\n" + usage.toString(), true)
                        .addField(Emojis.EMPTY_EMOJI, getString("command_example", exampleNumber > 1) + "\n" + examples.toString(), true);
                EmbedUtil.setFooter(eb, this, getString("command_args"));

                if (command.getUserPermissions().length > 0) {
                    eb.addField(Emojis.EMPTY_EMOJI, getString("command_userpermissions") + "\n" + permissionsList, false);
                }
                if (noArgs) {
                    EmbedUtil.addLog(eb, LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
                }

                return eb;
            }
        }
        return null;
    }

    private EmbedBuilder checkCategory(TextChannel channel, String arg) {
        if (arg.length() > 0) {
            for (String category : LIST) {
                if ((category.toLowerCase().contains(arg.toLowerCase()) || TextManager.getString(getLocale(), TextManager.COMMANDS, category).toLowerCase().contains(arg.toLowerCase()))) {
                    currentCategory = category;

                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(
                                    TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                            TextManager.getString(getLocale(), TextManager.COMMANDS, category)
                            );
                    EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "reaction_navigation"));

                    emojiConnections = new ArrayList<>();
                    emojiConnections.add(new BackEmojiConnection(BotPermissionUtil.can(channel, Permission.MESSAGE_EXT_EMOJI), ""));

                    switch (category) {
                        case Category.INTERACTIONS:
                            categoryInteractionsEmotes(eb, Category.INTERACTIONS);
                            break;

                        case Category.EMOTES:
                            categoryInteractionsEmotes(eb, Category.EMOTES);
                            break;

                        case Category.NSFW:
                            categoryNSFW(eb);
                            break;

                        case Category.PATREON_ONLY:
                            categoryPatreon(eb);
                            break;

                        default:
                            categoryDefault(eb, category);
                    }

                    return eb;
                }
            }
        }

        return null;
    }

    private void categoryInteractionsEmotes(EmbedBuilder eb, String category) {
        eb.setDescription(getString("emotes_desc"));

        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (commandManagementBean.commandIsTurnedOn(command) ||
                    BotPermissionUtil.can(getMember().get(), Permission.ADMINISTRATOR)
            ) {
                stringBuilder
                        .append("• `")
                        .append(command.getCommandProperties().emoji())
                        .append("⠀")
                        .append(getPrefix())
                        .append(commandTrigger)
                        .append("`");

                if (command.getCommandProperties().nsfw()) {
                    stringBuilder.append(generateCommandIcons(command, true, true));
                }
                stringBuilder.append("\n");

                emojiConnections.add(new EmojiConnection("", command.getTrigger()));
                i++;
                if (i >= 10) {
                    if (stringBuilder.length() > 0) eb.addField(Emojis.EMPTY_EMOJI, stringBuilder.toString(), true);
                    stringBuilder = new StringBuilder();
                    i = 0;
                }
            }
        }
        if (stringBuilder.length() > 0) {
            eb.addField(Emojis.EMPTY_EMOJI, stringBuilder.toString(), true);
        }

        addIconDescriptions(eb, false, false, category.equals(Category.INTERACTIONS), false);
        if (category.equals(Category.INTERACTIONS)) {
            eb.setDescription(getString("interactions_desc"));
        } else {
            eb.setDescription(getString("emotes_desc"));
        }

    }

    private void categoryPatreon(EmbedBuilder eb) {
        boolean includeLocked = false;
        boolean includeAlerts = false;
        boolean includeNSFW = false;

        int i = 0;
        for (String category : Category.LIST) {
            for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(category)) {
                Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
                String commandTrigger = command.getTrigger();
                if (command.getCommandProperties().patreonRequired() &&
                        !commandTrigger.equals(getTrigger()) &&
                        (commandManagementBean.commandIsTurnedOn(command) || BotPermissionUtil.can(getMember().get(), Permission.ADMINISTRATOR))
                ) {
                    StringBuilder title = new StringBuilder();
                    title.append(command.getCommandProperties().emoji())
                            .append(" `")
                            .append(getPrefix())
                            .append(commandTrigger)
                            .append("`");

                    if (command.getReleaseDate().orElse(LocalDate.now()).isAfter(LocalDate.now())) {
                        title.append(" ").append(getString("beta"));
                    }
                    title.append(generateCommandIcons(command, true, false));

                    if (command.isModCommand()) includeLocked = true;
                    if (command instanceof OnAlertListener) includeAlerts = true;
                    if (command.getCommandProperties().nsfw()) includeNSFW = true;

                    emojiConnections.add(new EmojiConnection(Emojis.LETTERS[i], command.getTrigger()));
                    i++;
                    eb.addField(
                            title.toString(),
                            TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.EMPTY_EMOJI,
                            true
                    );
                }
            }
        }

        eb.setDescription(getString("premium", ExternalLinks.PATREON_PAGE) + "\n" + Emojis.EMPTY_EMOJI);
        addIconDescriptions(eb, includeLocked, includeAlerts, includeNSFW, false);
    }

    private void categoryDefault(EmbedBuilder eb, String category) {
        boolean includeLocked = false;
        boolean includeAlerts = false;
        boolean includeNSFW = false;
        boolean includePatreon = false;

        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (!commandTrigger.equals(getTrigger()) &&
                    (commandManagementBean.commandIsTurnedOn(command) || BotPermissionUtil.can(getMember().get(), Permission.ADMINISTRATOR))
            ) {
                StringBuilder title = new StringBuilder();
                title.append(command.getCommandProperties().emoji())
                        .append(" `")
                        .append(getPrefix())
                        .append(commandTrigger)
                        .append("`");

                if (command.getReleaseDate().orElse(LocalDate.now()).isAfter(LocalDate.now())) {
                    title.append(" ").append(getString("beta"));
                }
                title.append(generateCommandIcons(command, true, true));

                if (command.isModCommand()) includeLocked = true;
                if (command instanceof OnAlertListener) includeAlerts = true;
                if (command.getCommandProperties().nsfw()) includeNSFW = true;
                if (command.getCommandProperties().patreonRequired()) includePatreon = true;

                emojiConnections.add(new EmojiConnection(Emojis.LETTERS[i], command.getTrigger()));
                i++;
                eb.addField(
                        title.toString(),
                        TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.EMPTY_EMOJI,
                        true
                );
            }
        }

        addIconDescriptions(eb, includeLocked, includeAlerts, includeNSFW, includePatreon);
    }

    private void categoryNSFW(EmbedBuilder eb) {
        eb.setDescription(getString("nsfw"));

        StringBuilder withSearchKey = new StringBuilder();
        StringBuilder withoutSearchKey = new StringBuilder();

        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(Category.NSFW)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());

            if (commandManagementBean.commandIsTurnedOn(command) ||
                    BotPermissionUtil.can(getMember().get(), Permission.ADMINISTRATOR)
            ) {
                String title = TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title");

                StringBuilder extras = new StringBuilder(generateCommandIcons(command, false, true));
                if (command instanceof PornSearchAbstract) {
                    withSearchKey.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
                } else if (command instanceof PornPredefinedAbstract) {
                    withoutSearchKey.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
                }

                emojiConnections.add(new EmojiConnection("", command.getTrigger()));
            }
        }

        if (withSearchKey.length() > 0) {
            withSearchKey.append("\n\n").append(getString("nsfw_searchkey_on_eg"));
            eb.addField(getString("nsfw_searchkey_on"), withSearchKey.toString(), true);
        }

        if (withoutSearchKey.length() > 0) {
            eb.addField(getString("nsfw_searchkey_off"), withoutSearchKey.toString(), true);
        }

        addIconDescriptions(eb, false, true, false, true);
    }

    private String generateCommandIcons(Command command, boolean includeNsfw, boolean includePatreon) {
        StringBuilder sb = new StringBuilder();

        if (command.isModCommand()) sb.append(CommandIcon.LOCKED);
        if (command instanceof OnAlertListener) sb.append(CommandIcon.ALERTS);
        if (includeNsfw && command.getCommandProperties().nsfw()) sb.append(CommandIcon.NSFW);
        if (includePatreon && command.getCommandProperties().patreonRequired()) sb.append(CommandIcon.PATREON);

        return sb.length() == 0 ? "" : "┊" + sb.toString();
    }

    private void addIconDescriptions(EmbedBuilder eb, boolean includeLocked, boolean includeAlerts, boolean includeNSFW, boolean includePatreon) {
        StringBuilder sb = new StringBuilder(getString("commandproperties")).append("\n\n");
        if (includeLocked) {
            sb.append(getString("commandproperties_LOCKED", CommandIcon.LOCKED.toString())).append("\n");
        }
        if (includeAlerts) {
            sb.append(getString("commandproperties_ALERTS", CommandIcon.ALERTS.toString())).append("\n");
        }
        if (includeNSFW) sb.append(getString("commandproperties_NSFW", CommandIcon.NSFW.toString())).append("\n");
        if (includePatreon) {
            sb.append(getString("commandproperties_PATREON", CommandIcon.PATREON.toString(), ExternalLinks.PATREON_PAGE)).append("\n");
        }

        eb.addField(Emojis.EMPTY_EMOJI, sb.toString(), false);
    }

    private EmbedBuilder checkMainPage(TextChannel channel) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, "categories"));
        EmbedUtil.setFooter(eb, this);

        StringBuilder categoriesSB = new StringBuilder();
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(BotPermissionUtil.can(channel, Permission.MESSAGE_EXT_EMOJI), "quit"));


        int i = 0;
        for (String string : LIST) {
            if (!commandManagementBean.getSwitchedOffElements().contains(string) || BotPermissionUtil.can(getMember().get(), Permission.ADMINISTRATOR)) {
                categoriesSB.append(Emojis.LETTERS[i]).append(" → ").append(TextManager.getString(getLocale(), TextManager.COMMANDS, string)).append("\n");
                emojiConnections.add(new EmojiConnection(Emojis.LETTERS[i], string));
                i++;
            }
        }

        categoriesSB.append("\n").append(getString("sp")).append("\n").append(Emojis.EMPTY_EMOJI);
        eb.setDescription(categoriesSB.toString());

        if (Program.isPublicVersion()) {
            eb.addField(getString("links_title"), getString(
                    "links_content",
                    ExternalLinks.LAWLIET_WEBSITE,
                    ExternalLinks.SERVER_INVITE_URL,
                    ExternalLinks.BOT_INVITE_URL,
                    ExternalLinks.UPVOTE_URL,
                    ExternalLinks.PATREON_PAGE,
                    ExternalLinks.FEATURE_REQUESTS_WEBSITE
            ), true);
        }
        return eb;
    }


    private static class CommandIcon {

        public static final CommandIcon LOCKED = new CommandIcon(Emojis.COMMAND_ICON_LOCKED);
        public static final CommandIcon ALERTS = new CommandIcon(Emojis.COMMAND_ICON_ALERTS);
        public static final CommandIcon NSFW = new CommandIcon(Emojis.COMMAND_ICON_NSFW);
        public static final CommandIcon PATREON = new CommandIcon(Emojis.COMMAND_ICON_PATREON);

        private final String emojiTag;

        public CommandIcon(String emojiTag) {
            this.emojiTag = emojiTag;
        }

        @Override
        public String toString() {
            return emojiTag;
        }

    }

}
