package commands.runnables.informationcategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.runnables.NavigationAbstract;
import commands.runnables.PornPredefinedAbstract;
import commands.runnables.PornSearchAbstract;
import commands.runnables.RolePlayAbstract;
import constants.*;
import core.EmbedFactory;
import core.ListGen;
import core.Program;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import mysql.modules.commandmanagement.CommandManagementData;
import mysql.modules.commandmanagement.DBCommandManagement;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "help",
        emoji = "❕",
        executableWithoutArgs = true,
        usesExtEmotes = true,
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
            Category.INTERACTIONS,
            Category.EXTERNAL,
            Category.NSFW,
            Category.PATREON_ONLY,
            Category.SPLATOON_2
    };

    private final HashMap<Integer, String> buttonMap = new HashMap<>();
    private String searchTerm;
    private CommandManagementData commandManagementBean;
    private String currentCategory = null;

    public HelpCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        searchTerm = args;
        commandManagementBean = DBCommandManagement.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener();
        return true;
    }

    @ControllerMessage(state = DEFAULT_STATE)
    public Response onMessage(GuildMessageReceivedEvent event, String input) {
        searchTerm = input;
        if (buttonMap.values().stream().anyMatch(str -> str.equalsIgnoreCase(input))) {
            searchTerm = input;
            return Response.TRUE;
        }
        return null;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButton(ButtonClickEvent event, int i) {
        String key = buttonMap.get(i);
        if (key != null) {
            searchTerm = key;

            if (searchTerm.equals("quit")) {
                deregisterListenersWithButtonMessage();
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

        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDraw() {
        String arg = searchTerm.trim();
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1, arg.length() - 1);

        TextChannel channel = getTextChannel().get();
        setOptions((OptionButton[]) null);

        EmbedBuilder eb;
        if ((eb = checkCommand(arg)) == null) {
            if ((eb = checkCategory(channel, arg)) == null) {
                eb = checkMainPage();
                if (arg.length() > 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), arg));
                }
            }
        }

        return eb;
    }

    private EmbedBuilder checkCommand(String arg) {
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

                buttonMap.clear();
                buttonMap.put(-1, currentCategory);

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
                    buttonMap.put(0, "exec:" + command.getClass().getName());
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
                        .addField(Emojis.ZERO_WIDTH_SPACE, getString("command_usage") + "\n" + usage.toString(), true)
                        .addField(Emojis.ZERO_WIDTH_SPACE, getString("command_example", exampleNumber > 1) + "\n" + examples.toString(), true);
                EmbedUtil.setFooter(eb, this, getString("command_args"));

                if (command.getUserPermissions().length > 0) {
                    eb.addField(Emojis.ZERO_WIDTH_SPACE, getString("command_userpermissions") + "\n" + permissionsList, false);
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

                    buttonMap.clear();
                    buttonMap.put(-1, "");

                    switch (category) {
                        case Category.INTERACTIONS:
                            categoryRolePlay(eb);
                            break;

                        case Category.NSFW:
                            categoryNSFW(channel, eb);
                            break;

                        case Category.PATREON_ONLY:
                            categoryPatreon(channel, eb);
                            break;

                        default:
                            categoryDefault(channel, eb, category);
                    }

                    return eb;
                }
            }
        }

        return null;
    }

    private void categoryRolePlay(EmbedBuilder eb) {
        addRolePlayCommandList(eb, command -> !command.isInteractive() && !command.getCommandProperties().nsfw());
        eb.addBlankField(false);

        eb.addField(getString("roleplay_interactive_title"), getString("roleplay_interactive_desc"), false);
        addRolePlayCommandList(eb, command -> command.isInteractive() && !command.getCommandProperties().nsfw());
        eb.addBlankField(false);

        eb.addField(getString("roleplay_nsfwinteractive_title"), getString("interaction_nsfw_desc"), false);
        addRolePlayCommandList(eb, command -> command.isInteractive() && command.getCommandProperties().nsfw());
    }

    private void addRolePlayCommandList(EmbedBuilder eb, Function<RolePlayAbstract, Boolean> rolePlayAbstractFilter) {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(Category.INTERACTIONS)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (rolePlayAbstractFilter.apply((RolePlayAbstract) command) &&
                    (commandManagementBean.commandIsTurnedOn(command) || BotPermissionUtil.can(getMember().get(), Permission.ADMINISTRATOR))
            ) {
                stringBuilder
                        .append("• `")
                        .append(command.getCommandProperties().emoji())
                        .append("⠀")
                        .append(getPrefix())
                        .append(commandTrigger)
                        .append("`")
                        .append("\n");

                i++;
                if (i >= 10) {
                    if (stringBuilder.length() > 0) eb.addField(Emojis.ZERO_WIDTH_SPACE, stringBuilder.toString(), true);
                    stringBuilder = new StringBuilder();
                    i = 0;
                }
            }
        }
        if (stringBuilder.length() > 0) {
            eb.addField(Emojis.ZERO_WIDTH_SPACE, stringBuilder.toString(), true);
        }
    }

    private void categoryPatreon(TextChannel channel, EmbedBuilder eb) {
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
                    title.append(generateCommandIcons(channel, command, true, true, false));

                    if (command.isModCommand()) includeLocked = true;
                    if (command instanceof OnAlertListener) includeAlerts = true;
                    if (command.getCommandProperties().nsfw()) includeNSFW = true;

                    buttonMap.put(i, command.getTrigger());
                    i++;
                    eb.addField(
                            title.toString(),
                            TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.ZERO_WIDTH_SPACE,
                            true
                    );
                }
            }
        }

        eb.setDescription(getString("premium", ExternalLinks.PATREON_PAGE) + "\n" + Emojis.ZERO_WIDTH_SPACE);
        addIconDescriptions(channel, eb, includeLocked, includeAlerts, includeNSFW, false);
    }

    private void categoryDefault(TextChannel channel, EmbedBuilder eb, String category) {
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
                title.append(generateCommandIcons(channel, command, true, true, true));

                if (command.isModCommand()) includeLocked = true;
                if (command instanceof OnAlertListener) includeAlerts = true;
                if (command.getCommandProperties().nsfw()) includeNSFW = true;
                if (command.getCommandProperties().patreonRequired()) includePatreon = true;

                buttonMap.put(i, command.getTrigger());
                i++;
                eb.addField(
                        title.toString(),
                        TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.ZERO_WIDTH_SPACE,
                        true
                );
            }
        }

        addIconDescriptions(channel, eb, includeLocked, includeAlerts, includeNSFW, includePatreon);
    }

    private void categoryNSFW(TextChannel channel, EmbedBuilder eb) {
        eb.setDescription(getString("nsfw"));

        StringBuilder withSearchKey = new StringBuilder();
        StringBuilder withoutSearchKey = new StringBuilder();

        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(Category.NSFW)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());

            if (commandManagementBean.commandIsTurnedOn(command) ||
                    BotPermissionUtil.can(getMember().get(), Permission.ADMINISTRATOR)
            ) {
                String title = TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title");

                StringBuilder extras = new StringBuilder(generateCommandIcons(channel, command, false, false, true));
                if (command instanceof PornSearchAbstract) {
                    withSearchKey.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
                } else if (command instanceof PornPredefinedAbstract) {
                    withoutSearchKey.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
                }
            }
        }

        if (withSearchKey.length() > 0) {
            withSearchKey.append("\n\n").append(getString("nsfw_searchkey_on_eg"));
            eb.addField(getString("nsfw_searchkey_on"), withSearchKey.toString(), true);
        }

        if (withoutSearchKey.length() > 0) {
            eb.addField(getString("nsfw_searchkey_off"), withoutSearchKey.toString(), true);
        }

        addIconDescriptions(channel, eb, false, false, false, true);
    }

    private String generateCommandIcons(TextChannel channel, Command command, boolean includeAlert, boolean includeNsfw, boolean includePatreon) {
        StringBuilder sb = new StringBuilder();

        if (command.isModCommand()) sb.append(CommandIcon.LOCKED.get(channel));
        if (includeAlert && command instanceof OnAlertListener) sb.append(CommandIcon.ALERTS.get(channel));
        if (includeNsfw && command.getCommandProperties().nsfw()) sb.append(CommandIcon.NSFW.get(channel));
        if (includePatreon && command.getCommandProperties().patreonRequired()) sb.append(CommandIcon.PATREON.get(channel));

        return sb.length() == 0 ? "" : "┊" + sb;
    }

    private void addIconDescriptions(TextChannel channel, EmbedBuilder eb, boolean includeLocked, boolean includeAlerts, boolean includeNSFW, boolean includePatreon) {
        StringBuilder sb = new StringBuilder(getString("commandproperties")).append("\n\n");
        if (includeLocked) {
            sb.append(getString("commandproperties_LOCKED", CommandIcon.LOCKED.get(channel))).append("\n");
        }
        if (includeAlerts) {
            sb.append(getString("commandproperties_ALERTS", CommandIcon.ALERTS.get(channel))).append("\n");
        }
        if (includeNSFW) sb.append(getString("commandproperties_NSFW", CommandIcon.NSFW.get(channel))).append("\n");
        if (includePatreon) {
            sb.append(getString("commandproperties_PATREON", CommandIcon.PATREON.get(channel), ExternalLinks.PATREON_PAGE)).append("\n");
        }

        eb.addField(Emojis.ZERO_WIDTH_SPACE, sb.toString(), false);
    }

    private EmbedBuilder checkMainPage() {
        ArrayList<String> options = new ArrayList<>();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, "categories"))
                .setDescription(getString("sp"));
        EmbedUtil.setFooter(eb, this);

        buttonMap.clear();
        buttonMap.put(-1, "quit");

        int i = 0;
        for (String string : LIST) {
            if (!commandManagementBean.getSwitchedOffElements().contains(string) || BotPermissionUtil.can(getMember().get(), Permission.ADMINISTRATOR)) {
                String title = TextManager.getString(getLocale(), TextManager.COMMANDS, string);
                buttonMap.put(i, string);
                options.add(title);
                i++;
            }
        }

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

        setOptions(options.toArray(new String[0]));
        return eb;
    }


    private static class CommandIcon {

        public static final CommandIcon LOCKED = new CommandIcon(Emojis.COMMAND_ICON_LOCKED, "¹");
        public static final CommandIcon ALERTS = new CommandIcon(Emojis.COMMAND_ICON_ALERTS, "²");
        public static final CommandIcon NSFW = new CommandIcon(Emojis.COMMAND_ICON_NSFW, "³");
        public static final CommandIcon PATREON = new CommandIcon(Emojis.COMMAND_ICON_PATREON, "⁴");

        private final String emojiTag;
        private final String unicodeAlternative;

        public CommandIcon(String emojiTag, String unicodeAlternative) {
            this.emojiTag = emojiTag;
            this.unicodeAlternative = unicodeAlternative;
        }

        public String get(TextChannel channel) {
            if (BotPermissionUtil.can(channel, Permission.MESSAGE_EXT_EMOJI)) {
                return emojiTag;
            } else {
                return unicodeAlternative;
            }
        }

    }

}
