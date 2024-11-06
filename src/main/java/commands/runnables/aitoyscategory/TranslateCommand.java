package commands.runnables.aitoyscategory;

import com.deepl.api.Formality;
import com.deepl.api.Language;
import com.deepl.api.TextResult;
import com.google.common.collect.Lists;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MainLogger;
import core.modals.ModalMediator;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.translate.DeepL;
import mysql.hibernate.entity.user.UserEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "translate",
        emoji = "💬",
        executableWithoutArgs = false,
        patreonRequired = true,
        aliases = {"translator"}
)
public class TranslateCommand extends NavigationAbstract {

    private static final int STATE_SET_SOURCE = 1,
            STATE_SET_TARGET = 2,
            STATE_SET_FORMALITY = 3;
    private static final Formality[] FORMALITY_ARRAY = new Formality[]{null, Formality.More, Formality.Less};

    private String textSource = null;
    private String sourceLanguageCode = null;
    private String sourceLanguageCodeEffectively = null;
    private Message message = null;

    public TranslateCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (Message.JUMP_URL_PATTERN.matcher(args).matches()) {
            deferReply();
            long messageIdIgnore = event.isMessageReceivedEvent() ? event.getMessageReceivedEvent().getMessage().getIdLong() : 0L;
            String messageContents = collectMessages(event.getMember(), args, messageIdIgnore);
            if (messageContents == null) {
                drawMessageNew(EmbedFactory.getEmbedError(this, getString("nolink")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }

            textSource = StringUtil.shortenString(messageContents, MessageEmbed.VALUE_MAX_LENGTH);
        } else {
            textSource = StringUtil.shortenString(args, MessageEmbed.VALUE_MAX_LENGTH);
        }
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        return switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                yield false;
            }
            case 0 -> {
                setState(STATE_SET_SOURCE);
                yield true;
            }
            case 1 -> {
                setState(STATE_SET_TARGET);
                yield true;
            }
            case 2 -> {
                setState(STATE_SET_FORMALITY);
                yield true;
            }
            case 3 -> {
                String id = "text";
                TextInput textInput = TextInput.create(id, getString("default_reply_label"), TextInputStyle.PARAGRAPH)
                        .setPlaceholder(getString("default_reply_placeholder"))
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("default_reply_title"), e -> {
                            String text = e.getValue(id).getAsString();
                            GuildMessageChannel channel = getGuild().get().getChannelById(GuildMessageChannel.class, message.getChannelIdLong());
                            if (channel == null) {
                                setLog(LogStatus.FAILURE, getString("default_reply_error"));
                                return null;
                            }

                            Language sourceLanguage;
                            Language targetLanguage;
                            try {
                                sourceLanguage = DeepL.getSourceLanguage(getUserEntity().getTranslateTargetLanguageCode().split("-")[0]);
                                targetLanguage = DeepL.getTargetLanguage(sourceLanguageCodeEffectively);
                                int formalityIndex = formalBooleanToIndex(getUserEntity().getTranslateFormal());
                                TextResult textResult = DeepL.translate(
                                        text,
                                        sourceLanguage.getCode(),
                                        targetLanguage.getCode(),
                                        targetLanguage.getSupportsFormality() != null && targetLanguage.getSupportsFormality() ? FORMALITY_ARRAY[formalityIndex] : null
                                );
                                text = textResult.getText();
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }

                            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                                    .setDescription(StringUtil.shortenString(text, MessageEmbed.VALUE_MAX_LENGTH))
                                    .setFooter(getString("default_reply_footer", e.getUser().getName(), sourceLanguage.getName(), targetLanguage.getName()));
                            EmbedUtil.setMemberAuthor(eb, e.getMember());

                            try {
                                channel.sendMessageEmbeds(eb.build())
                                        .setMessageReference(message)
                                        .complete();
                            } catch (Throwable e2) {
                                MainLogger.get().error("Translation reply exception", e2);
                                setLog(LogStatus.FAILURE, getString("default_reply_error"));
                                return null;
                            }

                            setLog(LogStatus.SUCCESS, getString("default_reply_success"));
                            return null;
                        })
                        .addActionRow(textInput)
                        .build();

                event.replyModal(modal).queue();
                yield false;
            }
            default -> false;
        };
    }

    @ControllerButton(state = STATE_SET_SOURCE)
    public boolean onButtonSetSource(ButtonInteractionEvent event, int i) throws Exception {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        } else if (i == 0) {
            sourceLanguageCode = null;
            setState(DEFAULT_STATE);
            return true;
        }

        Language language = DeepL.getSourceLanguages().get(i - 1);
        sourceLanguageCode = language.getCode();
        setState(DEFAULT_STATE);
        return true;
    }

    @ControllerButton(state = STATE_SET_TARGET)
    public boolean onButtonSetTarget(ButtonInteractionEvent event, int i) throws Exception {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        Language language = DeepL.getTargetLanguages().get(i);

        UserEntity userEntity = getUserEntity();
        userEntity.beginTransaction();
        userEntity.setTranslateTargetLanguageCode(language.getCode());
        userEntity.commitTransaction();

        setState(DEFAULT_STATE);
        return true;
    }

    @ControllerButton(state = STATE_SET_FORMALITY)
    public boolean onButtonSetFormality(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        UserEntity userEntity = getUserEntity();
        userEntity.beginTransaction();
        switch (i) {
            case 0 -> userEntity.setTranslateFormal(null);
            case 1 -> userEntity.setTranslateFormal(true);
            case 2 -> userEntity.setTranslateFormal(false);
        }
        userEntity.commitTransaction();

        setState(DEFAULT_STATE);
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) throws Exception {
        UserEntity userEntity = getUserEntity();
        String translateTargetLanguageCode = userEntity.getTranslateTargetLanguageCode();
        Language targetLanguage = DeepL.getTargetLanguage(translateTargetLanguageCode);
        int formalityIndex = formalBooleanToIndex(userEntity.getTranslateFormal());

        String[] options = getString("default_options").split("\n");
        if (message == null) {
            options[3] = "";
        }
        setComponents(options, Set.of(3), null, targetLanguage == null ? Set.of(3) : null);
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);

        if (targetLanguage != null) {
            TextResult textResult = DeepL.translate(
                    textSource,
                    sourceLanguageCode,
                    targetLanguage.getCode(),
                    targetLanguage.getSupportsFormality() != null && targetLanguage.getSupportsFormality() ? FORMALITY_ARRAY[formalityIndex] : null
            );
            Language sourceLanguage = DeepL.getSourceLanguage(textResult.getDetectedSourceLanguage());
            sourceLanguageCodeEffectively = sourceLanguage.getCode();
            eb.addField(sourceLanguageCode != null ? sourceLanguage.getName() : getString("default_detect2", sourceLanguage.getName()), textSource, false)
                    .addField(targetLanguage.getName(), StringUtil.shortenString(textResult.getText(), MessageEmbed.VALUE_MAX_LENGTH), false);
        } else {
            Language sourceLanguage = DeepL.getSourceLanguage(sourceLanguageCode);
            eb.addField(sourceLanguage != null ? sourceLanguage.getName() : getString("default_detect"), textSource, false);
        }

        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("default_formality_title", getString("default_formality").split("\n")[formalityIndex]), false);
        if (targetLanguage == null) {
            EmbedUtil.addLog(eb, getString("default_selecttarget"));
        }

        return eb;
    }

    @Draw(state = STATE_SET_SOURCE)
    public EmbedBuilder drawSetSource(Member member) throws Exception {
        ArrayList<String> options = DeepL.getSourceLanguages().stream()
                .map(Language::getName)
                .collect(Collectors.toCollection(ArrayList::new));
        options.add(0, getString("default_detect"));

        setComponents(options.toArray(String[]::new));
        return EmbedFactory.getEmbedDefault(this, null, getString("setsource_title"));
    }

    @Draw(state = STATE_SET_TARGET)
    public EmbedBuilder drawSetTarget(Member member) throws Exception {
        setComponents(DeepL.getTargetLanguages().stream().map(Language::getName).toArray(String[]::new));
        return EmbedFactory.getEmbedDefault(this, null, getString("settarget_title"));
    }

    @Draw(state = STATE_SET_FORMALITY)
    public EmbedBuilder drawSetFormality(Member member) throws Exception {
        setComponents(getString("default_formality").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("setformality_desc"), getString("setformality_title"));
    }

    private String collectMessages(Member member, String messageLink, long messageIdIgnore) {
        Matcher matcher = Message.JUMP_URL_PATTERN.matcher(messageLink);
        if (!matcher.find()) {
            return null;
        }
        long channelId = Long.parseLong(matcher.group("channel"));
        long messageId = Long.parseLong(matcher.group("message"));

        GuildMessageChannel channel = member.getGuild().getChannelById(GuildMessageChannel.class, channelId);
        if (channel == null) {
            return null;
        }
        if (!BotPermissionUtil.can(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY)) {
            setLog(LogStatus.FAILURE, getString("noaccess_bot", channel.getName()));
            return null;
        }
        if (!BotPermissionUtil.can(member, channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY)) {
            setLog(LogStatus.FAILURE, getString("noaccess_user", channel.getName()));
            return null;
        }

        MessageHistory messageHistory = channel.getHistoryAround(messageId, 100).complete();
        message = messageHistory.getMessageById(messageId);
        if (message == null) {
            return null;
        }

        List<Message> messages = Lists.reverse(messageHistory.getRetrievedHistory());
        return collectMessages(messages, messageId, messageIdIgnore, message.getAuthor().getIdLong());
    }

    private String collectMessages(List<Message> messages, long messageId, long messageIdIgnore, long authorId) {
        StringBuilder sb = new StringBuilder();

        boolean containsMessage = false;
        for (Message message : messages) {
            containsMessage = containsMessage || message.getIdLong() == messageId;
            if (containsMessage && message.getAuthor().getIdLong() != authorId) {
                break;
            }
            if (containsMessage && message.getIdLong() != messageIdIgnore) {
                sb.append(message.getContentRaw()).append("\n");
                appendEmbeds(sb, message.getEmbeds());
            }
        }

        return sb.toString();
    }

    private void appendEmbeds(StringBuilder sb, List<MessageEmbed> messageEmbeds) {
        for (MessageEmbed messageEmbed : messageEmbeds) {
            sb.append("---").append("\n");
            if (messageEmbed.getAuthor() != null) {
                sb.append(messageEmbed.getAuthor().getName()).append("\n");
            }
            if (messageEmbed.getTitle() != null) {
                sb.append(messageEmbed.getTitle()).append("\n");
            }
            if (messageEmbed.getDescription() != null) {
                sb.append(messageEmbed.getDescription()).append("\n");
            }
            for (MessageEmbed.Field field : messageEmbed.getFields()) {
                sb.append(field.getName())
                        .append("\n")
                        .append(field.getValue())
                        .append("\n");
            }
            if (messageEmbed.getFooter() != null) {
                sb.append(messageEmbed.getFooter().getText()).append("\n");
            }
            sb.append("---").append("\n");
        }
    }

    private int formalBooleanToIndex(Boolean formal) {
        if (formal == null) {
            return 0;
        } else if (formal) {
            return 1;
        } else {
            return 2;
        }
    }

}
