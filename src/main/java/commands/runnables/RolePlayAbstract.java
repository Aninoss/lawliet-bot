package commands.runnables;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.OnEntitySelectMenuListener;
import commands.runnables.interactionscategory.BiteCommand;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.RandomPicker;
import core.TextManager;
import core.mention.Mention;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import kotlin.Pair;
import mysql.hibernate.entity.user.RolePlayBlockEntity;
import mysql.hibernate.entity.user.RolePlayGender;
import mysql.hibernate.entity.user.UserEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class RolePlayAbstract extends Command implements OnEntitySelectMenuListener {

    private final boolean interactive;
    private final boolean symmetrical;
    private final HashMap<Pair<RolePlayGender, RolePlayGender>, String[]> gifs = new HashMap<>();
    private String gifUrl;
    private String quote = "";
    private List<Member> selectMenuMemberMentions;

    public RolePlayAbstract(Locale locale, String prefix, boolean interactive, String... otoGifs) {
        this(locale, prefix, interactive, false, otoGifs);
    }

    public RolePlayAbstract(Locale locale, String prefix, boolean interactive, boolean symmetrical, String... newGifs) {
        super(locale, prefix);
        this.interactive = interactive;
        this.symmetrical = symmetrical;
        setAtaGifs(newGifs);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        UserEntity userEntity = getUserEntityReadOnly();
        Mention mention = MentionUtil.getMentionedString(getLocale(), event.getGuild(), args, event.getMember(), event.getRepliedMember());

        selectGif(userEntity, mention.getElementList(), event.getGuild().getIdLong());
        if (userEntity.rolePlayGenderIsUnspecified()) {
            setLog(LogStatus.WARNING, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_gender", getPrefix()));
        }

        if (interactive) {
            return onTriggerInteractive(event, args, mention);
        } else {
            return onTriggerNonInteractive(event, args);
        }
    }

    public boolean isInteractive() {
        return interactive;
    }

    public boolean onTriggerInteractive(CommandEvent event, String args, Mention mention) throws ExecutionException, InterruptedException {
        boolean mentionPresent = !mention.getElementList().isEmpty();
        boolean selfReference = !mentionPresent && mention.containedBlockedUser();

        if (mentionPresent) {
            args = mention.getFilteredArgs().get();
        }
        args = args.replace(event.getMember().getAsMention(), "").trim();
        if (!args.isEmpty()) {
            quote = "\n\n>>> " + args;
        }
        EmbedBuilder eb = generateEmbed(event.getMember(), mention, selfReference);

        if (!mentionPresent && !selfReference) {
            EntitySelectMenu selectMenu = EntitySelectMenu.create("members", EntitySelectMenu.SelectTarget.USER)
                    .setPlaceholder(TextManager.getString(getLocale(), Category.INTERACTIONS, "selectmenu_placeholder"))
                    .setRequiredRange(1, SelectMenu.OPTIONS_MAX_AMOUNT)
                    .build();
            setComponents(selectMenu);
        }
        drawMessage(eb).exceptionally(ExceptionLogger.get());

        if (!mentionPresent && !selfReference) {
            registerEntitySelectMenuListener(event.getMember(), false);
        }
        return true;
    }

    public boolean onTriggerNonInteractive(CommandEvent event, String args) {
        if (!args.isEmpty()) {
            quote = "\n\n>>> " + args;
        }

        String desc = getString("template", "**" + StringUtil.escapeMarkdown(event.getMember().getEffectiveName()) + "**") + quote;
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, desc).setImage(gifUrl);
        drawMessage(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    @Override
    public boolean onEntitySelectMenu(@NotNull EntitySelectInteractionEvent event) throws ExecutionException, InterruptedException {
        deregisterListeners();
        selectMenuMemberMentions = event.getMentions().getMembers().stream()
                .filter(member -> member.getIdLong() != event.getUser().getIdLong())
                .collect(Collectors.toList());
        selectGif(getUserEntityReadOnly(), selectMenuMemberMentions, event.getGuild().getIdLong());
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        return generateEmbed(
                member,
                MentionUtil.getMentionedStringOfMembers(getLocale(), selectMenuMemberMentions),
                selectMenuMemberMentions.isEmpty()
        );
    }

    protected void setFtfGifs(String... newGifs) {
        gifs.put(new Pair<>(RolePlayGender.FEMALE, RolePlayGender.FEMALE), newGifs);
    }

    protected void setFtmGifs(String... newGifs) {
        gifs.put(new Pair<>(RolePlayGender.FEMALE, RolePlayGender.MALE), newGifs);
    }

    protected void setFtaGifs(String... newGifs) {
        gifs.put(new Pair<>(RolePlayGender.FEMALE, RolePlayGender.ANY), newGifs);
    }

    protected void setMtfGifs(String... newGifs) {
        gifs.put(new Pair<>(RolePlayGender.MALE, RolePlayGender.FEMALE), newGifs);
    }

    protected void setMtmGifs(String... newGifs) {
        gifs.put(new Pair<>(RolePlayGender.MALE, RolePlayGender.MALE), newGifs);
    }

    protected void setMtaGifs(String... newGifs) {
        gifs.put(new Pair<>(RolePlayGender.MALE, RolePlayGender.ANY), newGifs);
    }

    protected void setAtfGifs(String... newGifs) {
        gifs.put(new Pair<>(RolePlayGender.ANY, RolePlayGender.FEMALE), newGifs);
    }

    protected void setAtmGifs(String... newGifs) {
        gifs.put(new Pair<>(RolePlayGender.ANY, RolePlayGender.MALE), newGifs);
    }

    protected void setAtaGifs(String... newGifs) {
        gifs.put(new Pair<>(RolePlayGender.ANY, RolePlayGender.ANY), newGifs);
    }

    protected EmbedBuilder generateEmbed(Member member, Mention mention, boolean onlySelfReference) throws ExecutionException, InterruptedException {
        String authorString = "**" + StringUtil.escapeMarkdown(member.getEffectiveName()) + "**";
        if (onlySelfReference) {
            return getAloneGif(authorString);
        }

        if (containsBlockedUsers(member.getIdLong(), mention.getElementList())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_blocked"));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_footer").replace("{PREFIX}", getPrefix()));
            return eb;
        }

        return generateEmbedSunshineCase(member, mention, authorString, quote);
    }

    protected EmbedBuilder generateEmbedSunshineCase(Member member, Mention mention, String authorString, String quote) throws ExecutionException, InterruptedException {
        EmbedBuilder eb;
        if (mention.getElementList().isEmpty()) {
            eb = EmbedFactory.getEmbedDefault(this, getString("template_single", authorString) + quote);
        } else {
            eb = EmbedFactory.getEmbedDefault(this, getString("template", mention.isMultiple(), mention.getMentionText(), authorString) + quote);
        }

        eb.setImage(gifUrl);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.INTERACTIONS, "roleplay_footer").replace("{PREFIX}", getPrefix()));
        return eb;
    }

    private void selectGif(UserEntity userEntity, List<? extends ISnowflake> mentionedUserIds, long guildId) throws ExecutionException, InterruptedException {
        RolePlayGender selfGender = userEntity.getRolePlayGender();
        List<RolePlayGender> otherGenders = mentionedUserIds.stream()
                .map(user -> getEntityManager().findUserEntityReadOnly(user.getIdLong()).getRolePlayGender())
                .distinct()
                .collect(Collectors.toList());

        RolePlayGender otherGender = RolePlayGender.ANY;
        if (otherGenders.size() == 1 && otherGenders.contains(RolePlayGender.FEMALE)) {
            otherGender = RolePlayGender.FEMALE;
        } else if (otherGenders.size() == 1 && otherGenders.contains(RolePlayGender.MALE)) {
            otherGender = RolePlayGender.MALE;
        }

        String usedCommandTrigger = getAttachment("trigger", String.class);
        if (usedCommandTrigger != null) {
            if (usedCommandTrigger.toLowerCase().contains("yaoi")) {
                selfGender = RolePlayGender.MALE;
                otherGender = RolePlayGender.MALE;
            } else if (usedCommandTrigger.toLowerCase().contains("yuri")) {
                selfGender = RolePlayGender.FEMALE;
                otherGender = RolePlayGender.FEMALE;
            }
        }

        HashSet<String> gifSet = new HashSet<>();
        if (selectGif(gifSet, selfGender, otherGender, guildId)) {
            return;
        }
        if (selectGif(gifSet, otherGender, selfGender, guildId)) {
            return;
        }
        if (selectGif(gifSet, selfGender, RolePlayGender.ANY, guildId)) {
            return;
        }
        if (selectGif(gifSet, RolePlayGender.ANY, otherGender, guildId)) {
            return;
        }
        selectGif(gifSet, RolePlayGender.ANY, RolePlayGender.ANY, guildId, true);
    }

    private boolean selectGif(HashSet<String> gifSet, RolePlayGender selfGender, RolePlayGender otherGender, long guildId) throws ExecutionException, InterruptedException {
        return selectGif(gifSet, selfGender, otherGender, guildId, false);
    }

    private boolean selectGif(HashSet<String> gifSet, RolePlayGender selfGender, RolePlayGender otherGender, long guildId, boolean ignoreThreshold) throws ExecutionException, InterruptedException {
        gifSet.addAll(getValidGifs(selfGender, otherGender));
        if (symmetrical) {
            gifSet.addAll(getValidGifs(otherGender, selfGender));
        }
        List<String> validGifs = new ArrayList<>(gifSet);

        if (validGifs.size() >= 3 || ignoreThreshold) {
            gifUrl = validGifs.get(RandomPicker.pick(getTrigger() + "_" + selfGender.getId() + otherGender.getId(), guildId, validGifs.size()).get());
            return true;
        }
        return false;
    }

    private HashSet<String> getValidGifs(RolePlayGender selfGender, RolePlayGender otherGender) {
        HashSet<String> validGifs = new HashSet<>();

        for (RolePlayGender selfGenderLookup : getFittingGenders(selfGender)) {
            for (RolePlayGender otherGenderLookup : getFittingGenders(otherGender)) {
                String[] validGifsArray = gifs.getOrDefault(new Pair<>(selfGenderLookup, otherGenderLookup), new String[0]);
                validGifs.addAll(List.of(validGifsArray));
            }
        }

        return validGifs;
    }

    private RolePlayGender[] getFittingGenders(RolePlayGender gender) {
        return switch (gender) {
            case FEMALE -> new RolePlayGender[] { RolePlayGender.FEMALE, RolePlayGender.ANY, };
            case MALE -> new RolePlayGender[] { RolePlayGender.MALE, RolePlayGender.ANY, };
            case ANY -> new RolePlayGender[] { RolePlayGender.FEMALE, RolePlayGender.MALE, RolePlayGender.ANY, };
        };
    }

    private boolean containsBlockedUsers(long callerUserId, List<ISnowflake> users) {
        for (ISnowflake user : users) {
            RolePlayBlockEntity rolePlayBlock = getEntityManager().findUserEntityReadOnly(user.getIdLong()).getRolePlayBlock();
            if (rolePlayBlock.getBlockedUserIds().contains(callerUserId) ||
                    (rolePlayBlock.getBlockByDefault() && !rolePlayBlock.getAllowedUserIds().contains(callerUserId))
            ) {
                return true;
            }
        }
        return false;
    }

    private EmbedBuilder getAloneGif(String authorString) {
        String gif;
        String text;
        if (this instanceof BiteCommand) {
            gif = "https://cdn.discordapp.com/attachments/499629904380297226/958012189875241061/bite.gif";
            text = getString("themselves", authorString);
        } else {
            gif = "https://cdn.discordapp.com/attachments/736277561373491265/736277600053493770/hug.gif";
            text = TextManager.getString(getLocale(), Category.INTERACTIONS, "alone");
        }
        return EmbedFactory.getEmbedDefault(this, text)
                .setImage(gif);
    }

}
