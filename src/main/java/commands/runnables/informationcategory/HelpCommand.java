package commands.runnables.informationcategory;

import commands.*;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.runnables.ComponentMenuAbstract;
import commands.runnables.Pageable;
import commands.runnables.interactionscategory.CustomRolePlaySfwCommand;
import commands.runnables.nsfwinteractionscategory.CustomRolePlayNsfwCommand;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.Program;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.CustomRolePlayEntity;
import mysql.modules.commandusages.DBCommandUsages;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "help",
        emoji = "❕",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"commands"},
        requiresEmbeds = false
)
public class HelpCommand extends ComponentMenuAbstract {

    public static final String NSFW_SUBCATEGORY_GENERAL = "nsfw_0_general";
    public static final String NSFW_SUBCATEGORY_SEARCH = "nsfw_1_search";
    public static final String NSFW_SUBCATEGORY_TEMPLATES_HENTAI = "nsfw_2_templates_hentai";
    public static final String NSFW_SUBCATEGORY_TEMPLATES_REAL_LIFE = "nsfw_3_templates_real_life";
    public static final String RP_SUBCATEGORY_GENERAL = "rp_0_general";
    public static final String RP_SUBCATEGORY_CUSTOM = "rp_1_custom";
    public static final String RP_SUBCATEGORY_INTERACTIVE = "rp_2_interactive";
    public static final String RP_SUBCATEGORY_NON_INTERACTIVE = "rp_3_non_interactive";

    private static final String STATE_CATEGORY_ID = "category",
            STATE_COMMAND_ID = "command";
    private static final StateData STATE_CATEGORY = StateData.of(STATE_CATEGORY_ID, STATE_ROOT_ID, ""),
            STATE_COMMAND = StateData.of(STATE_COMMAND_ID, STATE_CATEGORY_ID, "");

    private Category currentCategory = null;
    private Command currentCommand = null;
    private Pageable<CommandEntry> categoryPageable = null;
    private final HashMap<Category, List<CommandEntry>> commandEntries = new HashMap<>();
    private final HashSet<CommandIcon> includedIcons = new HashSet<>();

    public HelpCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        if (!args.isEmpty()) {
            Boolean matchesCommand = checkCommands(event, args);
            if (matchesCommand == null) {
                return false;
            } else if (!matchesCommand && checkCategories(event, args) == null) {
                return false;
            }
            if (currentCategory == null) {
                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
            }
        }

        String error = getAttachment("error", String.class);
        if (error != null) {
            setLog(LogStatus.FAILURE, error);
            removeAttachment("error");
        }

        for (Category category : Category.getEntries()) {
            if (category.isNSFW() && !JDAUtil.channelIsNsfw(event.getMessageChannel())) {
                continue;
            }

            Collection<Class<? extends Command>> commandClasses;
            if (category == Category.PATREON_ONLY) {
                commandClasses = CommandContainer.getFullCommandList().stream()
                        .filter(clazz -> Command.getCommandProperties(clazz).patreonRequired())
                        .collect(Collectors.toList());
            } else {
                commandClasses = Objects.requireNonNullElse(CommandContainer.getCommandCategoryMap().get(category), Collections.emptyList());
            }

            ArrayList<CommandEntry> newCommandEntries = new ArrayList<>();
            for (Class<? extends Command> clazz : commandClasses) {
                Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
                String commandTrigger = command.getTrigger();
                if (!commandTrigger.equals(getTrigger()) &&
                        CommandManager.commandIsEnabledEffectively(getGuildEntity(), command, event.getMember(), event.getMessageChannel()) &&
                        (!command.getCommandProperties().nsfw() || JDAUtil.channelIsNsfw(event.getMessageChannel())) &&
                        BotPermissionUtil.getMissingPermissions(event.getMember(), command.getAdjustedUserGuildPermissions()).isEmpty() &&
                        BotPermissionUtil.getMissingPermissions(event.getMessageChannel(), event.getMember(), command.getAdjustedUserChannelPermissions()).isEmpty()
                ) {
                    CommandLanguage commandLanguage = command.getCommandLanguage();
                    CommandProperties commandProperties = command.getCommandProperties();
                    CommandEntry entry = new CommandEntry(
                            command.getReleaseDate().orElse(LocalDate.MIN).isAfter(LocalDate.now()),
                            commandProperties.obsolete(),
                            commandProperties.trigger(),
                            commandProperties.emoji(),
                            getCommandIcons(command),
                            commandLanguage.getDescShort(),
                            commandProperties.subCategory(),
                            command
                    );
                    newCommandEntries.add(entry);
                }
            }

            CustomRolePlayCategory customRolePlayCategory = CustomRolePlayCategory.fromCommandCategory(category);
            if (customRolePlayCategory != null &&
                    CommandManager.commandIsEnabledEffectively(getGuildEntity(), customRolePlayCategory.getTemplateCommandClass(), event.getMember(), event.getMessageChannel())
            ) {
                for (Map.Entry<String, CustomRolePlayEntity> keyValueEntry : getGuildEntity().getCustomRolePlayCommandsEffectively().entrySet()) {
                    CustomRolePlayEntity customRolePlay = keyValueEntry.getValue();
                    if (customRolePlay.getNsfw() != customRolePlayCategory.getNsfw()) {
                        continue;
                    }
                    CommandEntry entry = new CommandEntry(
                            false,
                            false,
                            keyValueEntry.getKey(),
                            customRolePlay.getEmojiFormatted(),
                            customRolePlay.getNsfw() ? List.of(CommandIcon.NSFW, CommandIcon.PATREON) : List.of(CommandIcon.PATREON),
                            null,
                            RP_SUBCATEGORY_CUSTOM,
                            null
                    );
                    newCommandEntries.add(entry);
                }
            }

            if (!newCommandEntries.isEmpty()) {
                newCommandEntries.sort((a, b) -> {
                    int subCategoryCompare = a.subCategory.compareTo(b.subCategory);
                    if (subCategoryCompare != 0) {
                        return subCategoryCompare;
                    } else {
                        if (a.subCategory.equals(RP_SUBCATEGORY_CUSTOM)) {
                            return a.trigger.compareTo(b.trigger);
                        } else {
                            return Long.compare(
                                    DBCommandUsages.getInstance().retrieve(b.trigger).getValue(),
                                    DBCommandUsages.getInstance().retrieve(a.trigger).getValue()
                            );
                        }
                    }
                });
                commandEntries.put(category, newCommandEntries);
            }
        }

        registerListeners(event.getMember(), STATE_CATEGORY, STATE_COMMAND);
        return true;
    }

    @Draw(state = STATE_ROOT_ID)
    public List<ContainerChildComponent> drawRoot(Member member) {
        currentCategory = null;
        currentCommand = null;
        ArrayList<ContainerChildComponent> components = new ArrayList<>();

        String image = Program.publicInstance()
                ? "https://cdn.discordapp.com/attachments/692894361314131969/1496464852015644732/help_banner.png"
                : "https://cdn.discordapp.com/attachments/692894361314131969/1496465106702303365/help_banner_custom.png";
        MediaGalleryItem mediaGalleryItem = MediaGalleryItem.fromUrl(image);
        components.add(MediaGallery.of(mediaGalleryItem));

        components.add(Separator.createInvisible(Separator.Spacing.SMALL));
        components.add(TextDisplay.of(getString("root_disclaimer", ExternalLinks.COMMANDS_WEBSITE)));
        components.add(ActionRow.of(
                Button.of(ButtonStyle.LINK, ExternalLinks.DASHBOARD_WEBSITE, getString("root_button_dashboard")),
                Button.of(ButtonStyle.LINK, ExternalLinks.SERVER_INVITE_URL, getString("root_button_server")),
                Button.of(ButtonStyle.LINK, ExternalLinks.BOT_INVITE_URL, getString("root_button_add")),
                Button.of(ButtonStyle.LINK, ExternalLinks.PREMIUM_WEBSITE, getString("root_button_premium")),
                Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, getString("root_button_upvote"))
        ));

        components.addAll(generateCategoriesSelectMenu());
        return components;
    }

    @Draw(state = STATE_CATEGORY_ID)
    public List<ContainerChildComponent> drawCategory(Member member) {
        STATE_CATEGORY.setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, currentCategory.getId()));
        currentCommand = null;
        includedIcons.clear();
        ArrayList<ContainerChildComponent> components = new ArrayList<>();

        GuildMessageChannel channel = getGuildMessageChannel().get();
        AtomicReference<String> previousSubCommand = new AtomicReference<>(null);
        components.addAll(categoryPageable.getComponents(entry -> {
            includedIcons.addAll(entry.icons);

            String textKey = "category_command";
            if (entry.beta) {
                textKey = "category_command_beta";
            } else if (entry.obsolete) {
                textKey = "category_command_obsolete";
            }

            String text = getString(textKey,
                    entry.emoji,
                    entry.trigger,
                    commandIconsToString(channel, entry.icons)
            );
            if (entry.descriptionShort != null) {
                text += "\n> " + entry.descriptionShort;
            }

            ArrayList<ContainerChildComponent> entryComponents = new ArrayList<>();
            String commandSubCategory = entry.subCategory;
            if (!commandSubCategory.equals(previousSubCommand.get()) && !commandSubCategory.isEmpty()) {
                String prefix = previousSubCommand.get() != null ? (Emojis.ZERO_WIDTH_SPACE.getFormatted() + "\n") : "";
                TextDisplay textDisplay = TextDisplay.of( prefix + "-# " + getString("category_subcategory_" + commandSubCategory).toUpperCase());
                entryComponents.add(textDisplay);
            }
            previousSubCommand.set(commandSubCategory);

            if (entry.command != null) {
                Button browseButton = buttonSecondary(Emojis.MENU_SHORT_ARROW_RIGHT, event -> {
                    currentCommand = entry.command;
                    setState(STATE_COMMAND_ID);
                    return true;
                });
                entryComponents.add(Section.of(browseButton, TextDisplay.of(text)));
            } else {
                entryComponents.add(TextDisplay.of(text));
            }

            return entryComponents;
        }));

        StringBuilder sb = new StringBuilder();
        for (CommandIcon commandIcon : CommandIcon.values()) {
            if (!includedIcons.contains(commandIcon)) {
                continue;
            }
            sb.append(getString("category_icon_" + commandIcon.name(), commandIcon.get(channel), ExternalLinks.PREMIUM_WEBSITE)).append("\n");
        }
        if (!sb.isEmpty()) {
            components.add(TextDisplay.of(Emojis.ZERO_WIDTH_SPACE.getFormatted() + "\n" + sb));
        }

        components.addAll(generateCategoriesSelectMenu());
        return components;
    }

    @Draw(state = STATE_COMMAND_ID)
    public List<ContainerChildComponent> drawCommand(Member member) {
        STATE_CATEGORY.setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, currentCategory.getId()));
        STATE_COMMAND.setTitle(currentCommand.getCommandLanguage().getTitle());
        ArrayList<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of(currentCommand.getCommandLanguage().getDescLong()));
        components.add(Separator.createInvisible(Separator.Spacing.SMALL));

        StringBuilder usage = new StringBuilder();
        usage.append(getString("command_usage")).append("\n");
        for (String line : TextManager.getString(getLocale(), currentCommand.getCategory(), currentCommand.getTrigger() + "_usage").split("\n")) {
            usage.append("- ").append(getPrefix()).append(currentCommand.getTrigger()).append(" ").append(line).append("\n");
        }
        components.add(TextDisplay.of(usage.toString()));

        StringBuilder examples = new StringBuilder();
        examples.append(getString("command_examples")).append("\n");
        for (String line : TextManager.getString(getLocale(), currentCommand.getCategory(), currentCommand.getTrigger() + "_examples").split("\n")) {
            line = StringUtil.solveVariablesOfCommandText(line, getGuildMessageChannel().get(), member, getPrefix());
            examples.append("- ").append(getPrefix()).append(currentCommand.getTrigger()).append(" ").append(line).append("\n");
        }
        components.add(TextDisplay.of(examples.toString()));

        Arrays.stream(currentCommand.getUserPermissions())
                .map(permission -> TextManager.getString(getLocale(), TextManager.PERMISSIONS, permission.name()))
                .reduce((a, b) -> a + ", " + b)
                .ifPresent(permissions -> components.add(TextDisplay.of(getString("command_user_permissions", CommandIcon.LOCKED.get(getGuildMessageChannel().get()), permissions))));

        if (currentCommand.getCommandProperties().executableWithoutArgs()) {
            Button button = buttonPrimary(getString("command_run"), event -> {
                Command command = CommandManager.createCommandByClass(currentCommand.getClass(), getLocale(), getPrefix());
                CommandManager.manage(getCommandEvent(), command, "", getGuildEntity(), Instant.now(), false);
                return false;
            });
            components.add(Separator.createInvisible(Separator.Spacing.LARGE));
            components.add(ActionRow.of(button));
        }

        return components;
    }

    private Boolean checkCommands(@NotNull CommandEvent event, @NotNull String args) {
        for (Class<? extends Command> clazz : CommandContainer.getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();

            if ((commandTrigger.equalsIgnoreCase(args) || Arrays.stream(command.getCommandProperties().aliases()).anyMatch(args::equalsIgnoreCase)) &&
                    !commandTrigger.equals(getTrigger())
            ) {
                if (command.getCommandProperties().nsfw() && !JDAUtil.channelIsNsfw(event.getMessageChannel())) {
                    drawMessageNew(EmbedFactory.getNSFWBlockEmbed(this)).exceptionally(ExceptionLogger.get());
                    return null;
                }

                currentCategory = command.getCategory();
                currentCommand = command;
                loadPageable();
                setState(STATE_COMMAND_ID);
                if (hasAttachment("noargs")) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
                }
                return true;
            }
        }
        return false;
    }

    private Boolean checkCategories(@NotNull CommandEvent event, @NotNull String args) {
        args = args.replace("category:", "");
        for (Category value : Category.getEntries()) {
            if (value.isHidden()) {
                continue;
            }
            if ((value.getId().equalsIgnoreCase(args) || TextManager.getString(getLocale(), TextManager.COMMANDS, value.getId()).equalsIgnoreCase(args))) {
                currentCategory = value;
                break;
            } else if (value.getId().toLowerCase().contains(args.toLowerCase()) || TextManager.getString(getLocale(), TextManager.COMMANDS, value.getId()).toLowerCase().contains(args.toLowerCase())) {
                currentCategory = value;
            }
        }

        if (currentCategory != null) {
            if (currentCategory.isNSFW() && !JDAUtil.channelIsNsfw(event.getMessageChannel())) {
                drawMessageNew(EmbedFactory.getNSFWBlockEmbed(this)).exceptionally(ExceptionLogger.get());
                return null;
            }
            loadPageable();
            setState(STATE_CATEGORY_ID);
            return true;
        }
        return false;
    }

    private List<ContainerChildComponent> generateCategoriesSelectMenu() {
        StringSelectMenu.Builder builder = stringSelectMenu(event -> {
            currentCategory = Category.fromId(event.getValues().get(0));
            currentCommand = null;
            loadPageable();
            setState(STATE_CATEGORY_ID);
        });
        builder.setPlaceholder(getString("category_placeholder"));
        for (Category category : Category.getEntries()) {
            if (commandEntries.containsKey(category)) {
                String label = TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId());
                builder.addOption(label, category.getId(), Emoji.fromUnicode(category.getEmoji()));
                if (category == currentCategory) {
                    builder.setDefaultValues(List.of(category.getId()));
                }
            }
        }

        ActionRow actionRow = ActionRow.of(builder.build());
        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        components.add(Separator.createDivider(Separator.Spacing.LARGE));
        if (getState().equals(STATE_ROOT_ID)) {
            components.add(TextDisplay.of(getString("root_category_label")));
        }
        components.add(actionRow);
        return components;
    }

    private void loadPageable() {
        categoryPageable = new Pageable<>(this, 7, () -> commandEntries.get(currentCategory));
    }

    private List<CommandIcon> getCommandIcons(Command command) {
        ArrayList<CommandIcon> icons = new ArrayList<>();

        if (command.isModCommand()) icons.add(CommandIcon.LOCKED);
        if (command instanceof OnAlertListener) icons.add(CommandIcon.ALERTS);
        if (command.getCommandProperties().nsfw()) icons.add(CommandIcon.NSFW);
        if (command.getCommandProperties().patreonRequired()) icons.add(CommandIcon.PATREON);

        return icons;
    }

    private String commandIconsToString(GuildMessageChannel channel, List<CommandIcon> icons) {
        StringBuilder sb = new StringBuilder();
        for (CommandIcon icon : icons) {
            sb.append(icon.get(channel));
        }
        return sb.toString();
    }


    private enum CommandIcon {

        LOCKED(Emojis.COMMAND_ICON_LOCKED, "¹"),
        ALERTS(Emojis.COMMAND_ICON_ALERTS, "²"),
        NSFW(Emojis.COMMAND_ICON_NSFW, "³"),
        PATREON(Emojis.COMMAND_ICON_PREMIUM, "⁴");

        private final CustomEmoji customEmoji;
        private final String unicodeAlternative;

        CommandIcon(CustomEmoji customEmoji, String unicodeAlternative) {
           this.customEmoji = customEmoji;
            this.unicodeAlternative = unicodeAlternative;
        }

        public String get(GuildMessageChannel channel) {
            if (BotPermissionUtil.can(channel, Permission.MESSAGE_EXT_EMOJI)) {
                return customEmoji.getFormatted();
            } else {
                return unicodeAlternative;
            }
        }

    }

    private enum CustomRolePlayCategory {

        SFW(CustomRolePlaySfwCommand.class, false),
        NSFW(CustomRolePlayNsfwCommand.class, true);

        private final Class<? extends CustomRolePlaySfwCommand> templateCommandClass;
        private final boolean nsfw;

        CustomRolePlayCategory(Class<? extends CustomRolePlaySfwCommand> templateCommandClass, boolean nsfw) {
            this.templateCommandClass = templateCommandClass;
            this.nsfw = nsfw;
        }

        public Class<? extends Command> getTemplateCommandClass() {
            return templateCommandClass;
        }

        public boolean getNsfw() {
            return nsfw;
        }

        public static CustomRolePlayCategory fromCommandCategory(Category commandCategory) {
            return switch (commandCategory) {
                case INTERACTIONS -> SFW;
                case NSFW_INTERACTIONS -> NSFW;
                default -> null;
            };
        }

    }

    private class CommandEntry {

        private boolean beta;
        private boolean obsolete;
        private String trigger;
        private String emoji;
        private List<CommandIcon> icons;
        private String descriptionShort;
        private String subCategory;
        private Command command;

        public CommandEntry(boolean beta, boolean obsolete, String trigger, String emoji, List<CommandIcon> icons, String descriptionShort, String subCategory, Command command) {
            this.beta = beta;
            this.obsolete = obsolete;
            this.trigger = trigger;
            this.emoji = emoji;
            this.icons = icons;
            this.descriptionShort = descriptionShort;
            this.subCategory = subCategory;
            this.command = command;
        }

    }

}
