package commands.stateprocessor;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.LocalFile;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.FileUtil;
import core.utils.InternetUtil;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FileListStateProcessor extends AbstractStateProcessor<List<LocalFile>, List<LocalFile>, FileListStateProcessor> {

    private final String dir;
    private boolean allowGifs = true;
    private int maxFiles = Integer.MAX_VALUE;
    private boolean deleteRemovedFiles = false;
    private int currentImage = 0;
    private BotLogEntity.Event logEventAdd = null;
    private BotLogEntity.Event logEventRemove = null;

    public FileListStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, String dir) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_filelist_desc_empty"));
        this.dir = dir;
    }

    public FileListStateProcessor setAllowGifs(boolean supportGifs) {
        this.allowGifs = supportGifs;
        return this;
    }

    public FileListStateProcessor setMaxFiles(int max) {
        this.maxFiles = max;
        return this;
    }

    public FileListStateProcessor setLogEvents(BotLogEntity.Event logEventAdd, BotLogEntity.Event logEventRemove) {
        this.logEventAdd = logEventAdd;
        this.logEventRemove = logEventRemove;
        return this;
    }

    public FileListStateProcessor deleteRemovedFiles() {
        this.deleteRemovedFiles = true;
        return this;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input) {
        List<LocalFile> previousEntries = get();
        if (previousEntries.size() >= maxFiles) {
            NavigationAbstract command = getCommand();
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_filelist_toomanyentries", StringUtil.numToString(maxFiles)));
            return MessageInputResponse.FAILED;
        }

        List<LocalFile> newAttachments = event.getMessage().getAttachments().stream()
                .filter(attachment -> InternetUtil.uriIsImage(attachment.getUrl(), allowGifs))
                .limit(maxFiles - previousEntries.size())
                .map(attachment -> {
                    LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format(dir + "/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
                    FileUtil.downloadImageAttachment(attachment, tempFile);
                    return tempFile;
                })
                .collect(Collectors.toList());

        if (newAttachments.isEmpty()) {
            NavigationAbstract command = getCommand();
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, allowGifs ? "imagenotfound" : "imagenotfound_nogifs"));
            return MessageInputResponse.FAILED;
        }

        ArrayList<LocalFile> files = new ArrayList<>(previousEntries);
        files.addAll(newAttachments);
        set(files, false, logEventAdd != null ? entityManager -> BotLogEntity.log(entityManager, logEventAdd, getCommand().getGuildId().get(), getCommand().getMemberId().get()) : null);
        return MessageInputResponse.SUCCESS;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i) throws Throwable {
        switch (i) {
            case -1 -> {
                super.controllerButton(event, i);
                return true;
            }
            case 0 -> {
                currentImage -= 1;
                if (currentImage < 0) {
                    currentImage = get().size() - 1;
                }
                return true;
            }
            case 1 -> {
                int attachmentsSize = get().size();
                String textId = "page";
                String textLabel = TextManager.getString(getCommand().getLocale(), TextManager.COMMANDS, "stateprocessor_filelist_goto_label", String.valueOf(attachmentsSize));
                TextInput message = TextInput.create(textId, TextInputStyle.SHORT)
                        .setPlaceholder(String.valueOf(currentImage + 1))
                        .setMinLength(1)
                        .setMaxLength(2)
                        .build();

                String title = TextManager.getString(getCommand().getLocale(), TextManager.GENERAL, "list_goto");
                Modal modal = ModalMediator.createDrawableCommandModal(getCommand(), title, e -> {
                            String pageString = e.getValue(textId).getAsString();
                            if (StringUtil.stringIsInt(pageString)) {
                                currentImage = Math.min(attachmentsSize - 1, Math.max(0, Integer.parseInt(pageString) - 1));
                            }
                            return null;
                        }).addComponents(Label.of(textLabel, message))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                currentImage += 1;
                if (currentImage >= get().size()) {
                    currentImage = 0;
                }
                return true;
            }
            case 3 -> {
                ArrayList<LocalFile> files = new ArrayList<>(get());
                LocalFile removedFile = files.remove(currentImage);
                if (deleteRemovedFiles) {
                    removedFile.delete();
                }
                currentImage = Math.min(currentImage, files.size() - 1);
                set(files, files.isEmpty(), logEventRemove != null ? entityManager -> BotLogEntity.log(entityManager, logEventRemove, getCommand().getGuildId().get(), getCommand().getMemberId().get()) : null);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void goBack() {
        currentImage = 0;
        super.goBack();
    }

    @Override
    protected void addComponents(NavigationAbstract command) {
        if (get().isEmpty()) {
            return;
        }

        String[] options = new String[]{
                TextManager.getString(getCommand().getLocale(), TextManager.GENERAL, "list_previous"),
                TextManager.getString(getCommand().getLocale(), TextManager.GENERAL, "list_goto"),
                TextManager.getString(getCommand().getLocale(), TextManager.GENERAL, "list_next"),
                TextManager.getString(getCommand().getLocale(), TextManager.COMMANDS, "stateprocessor_filelist_delete")
        };

        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            buttons.add(Button.of(i == 3 ? ButtonStyle.DANGER : ButtonStyle.PRIMARY, String.valueOf(i), options[i]));
        }
        getCommand().setComponents(buttons);
    }

    @Override
    protected void modifyEmbed(EmbedBuilder eb, Locale locale) {
        List<LocalFile> files = get();
        if (files.isEmpty()) {
            return;
        }

        String desc = TextManager.getString(locale, TextManager.COMMANDS, "stateprocessor_filelist_desc_file", StringUtil.numToString(currentImage + 1), StringUtil.numToString(files.size()));
        eb.setDescription(getDescription() + "\n\n" + desc)
                .setImage(files.get(currentImage).cdnGetUrl());
    }

}
