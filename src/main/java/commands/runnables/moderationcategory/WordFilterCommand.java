package commands.runnables.moderationcategory;

import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.MembersStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.modals.ModalMediator;
import core.utils.CollectionUtil;
import core.utils.StringUtil;
import modules.automod.WordFilter;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.WordFilterEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "wordfilter",
        botGuildPermissions = Permission.MESSAGE_MANAGE,
        userGuildPermissions = {Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS},
        emoji = "️🚧️",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        usesExtEmotes = true,
        aliases = {"wordsfilter", "badwordfilter", "badwordsfilter", "bannedwords"}
)
public class WordFilterCommand extends NavigationAbstract {

    public static int MAX_EXCLUDED_MEMBERS = 25;
    public static int MAX_LOG_RECEIVERS = 10;
    public static final int MAX_WORDS = 40;
    public static final int MAX_LETTERS_PER_WORD = 20;

    private static final int STATE_SET_EXCLUDED_MEMBERS = 1,
            STATE_SET_LOG_RECEIVERS = 2,
            STATE_ADD_WORDS = 3,
            STATE_REMOVE_WORDS = 4;

    private NavigationHelper<String> wordsNavigationHelper;

    public WordFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        wordsNavigationHelper = new NavigationHelper<>(this, guildEntity -> guildEntity.getWordFilter().getWords(), String.class, MAX_WORDS);
        registerNavigationListener(event.getMember(), List.of(
                new MembersStateProcessor(this, STATE_SET_EXCLUDED_MEMBERS, DEFAULT_STATE, getString("state0_mignoredusers"))
                        .setMinMax(0, MAX_EXCLUDED_MEMBERS)
                        .setLogEvent(BotLogEntity.Event.WORD_FILTER_EXCLUDED_MEMBERS)
                        .setGetter(() -> getGuildEntity().getWordFilter().getExcludedMemberIds())
                        .setSetter(userIds -> CollectionUtil.replace(getGuildEntity().getWordFilter().getExcludedMemberIds(),userIds)),
                new MembersStateProcessor(this, STATE_SET_LOG_RECEIVERS, DEFAULT_STATE, getString("state0_mlogreciever"))
                        .setMinMax(0, MAX_LOG_RECEIVERS)
                        .setLogEvent(BotLogEntity.Event.WORD_FILTER_LOG_RECEIVERS)
                        .setGetter(() -> getGuildEntity().getWordFilter().getLogReceiverUserIds())
                        .setSetter(userIds -> CollectionUtil.replace(getGuildEntity().getWordFilter().getLogReceiverUserIds(), userIds))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                WordFilterEntity wordFilter = getGuildEntity().getWordFilter();
                wordFilter.beginTransaction();
                wordFilter.setActive(!wordFilter.getActive());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WORD_FILTER_ACTIVE, event.getMember(), null, wordFilter.getActive());
                wordFilter.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("onoffset", !wordFilter.getActive()));
                return true;

            case 1:
                setState(STATE_SET_EXCLUDED_MEMBERS);
                return true;

            case 2:
                setState(STATE_SET_LOG_RECEIVERS);
                return true;

            case 3:
                String id = "text";
                TextInput textInput = TextInput.create(id, getString("state0_mwords"), TextInputStyle.SHORT)
                        .setRequiredRange(1, MAX_LETTERS_PER_WORD)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("state3_title"), e -> {
                            String input = e.getValue(id).getAsString();

                            String[] wordArray = WordFilter.translateString(input).split(" ");
                            List<String> wordList = Arrays.stream(wordArray)
                                    .filter(str -> !str.isEmpty())
                                    .map(str -> str.substring(0, Math.min(MAX_LETTERS_PER_WORD, str.length())))
                                    .collect(Collectors.toList());
                            wordsNavigationHelper.addData(wordList, input, event.getMember(), 0, BotLogEntity.Event.WORD_FILTER_WORDS);
                            return null;
                        })
                        .addActionRow(textInput)
                        .build();

                event.replyModal(modal).queue();
                return false;

            case 4:
                wordsNavigationHelper.startDataRemove(STATE_REMOVE_WORDS);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_ADD_WORDS)
    public boolean onButtonAddWords(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_REMOVE_WORDS)
    public boolean onButtonRemoveWords(ButtonInteractionEvent event, int i) {
        return wordsNavigationHelper.removeData(i, event.getMember(), 0, BotLogEntity.Event.WORD_FILTER_WORDS);
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        WordFilterEntity wordFilter = getGuildEntity().getWordFilter();
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), wordFilter.getActive()), true)
                .addField(getString("state0_mignoredusers"), new ListGen<AtomicMember>().getList(wordFilter.getExcludedMembers(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true)
                .addField(getString("state0_mlogreciever"), new ListGen<AtomicMember>().getList(wordFilter.getLogReceivers(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true)
                .addField(getString("state0_mwords"), getWordsString(wordFilter.getWords()), true);
    }

    @Draw(state = STATE_ADD_WORDS)
    public EmbedBuilder drawAddWords(Member member) {
        return wordsNavigationHelper.drawDataAdd(getString("state3_title"), getString("state3_description"));
    }

    @Draw(state = STATE_REMOVE_WORDS)
    public EmbedBuilder drawRemoveWords(Member member) {
        return wordsNavigationHelper.drawDataRemove(getString("state4_title"), getString("state4_description"), getLocale());
    }

    private String getWordsString(List<String> words) {
        if (words.isEmpty()) {
            return TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        } else {
            StringBuilder sb = new StringBuilder();

            for (String word : words) {
                sb.append(" ").append("`").append(word).append("`");
            }

            return sb.toString();
        }
    }

}
