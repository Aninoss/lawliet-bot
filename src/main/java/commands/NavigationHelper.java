package commands;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.*;
import core.utils.JDAUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NavigationHelper<T> {

    private enum Type {Unknown, Role, Channel, Member}

    private final NavigationAbstract command;
    private final Function<GuildEntity, List<T>> srcListSupplier;
    private final int max;
    private Type type = Type.Unknown;
    private String typeString = "";
    private boolean checkRolesHierarchy = true;

    public NavigationHelper(NavigationAbstract command, Function<GuildEntity, List<T>> srcListSupplier, Class<T> typeClass, int max, boolean checkRolesHierarchy) {
        this(command, srcListSupplier, typeClass, max);
        this.checkRolesHierarchy = checkRolesHierarchy;
    }

    public NavigationHelper(NavigationAbstract command, Function<GuildEntity, List<T>> srcListSupplier, Class<T> typeClass, int max) {
        this.command = command;
        this.srcListSupplier = srcListSupplier;
        this.max = max;

        if (typeClass == AtomicRole.class) {
            this.type = Type.Role;
            this.typeString = "_role";
        } else if (typeClass == AtomicGuildMessageChannel.class || typeClass == AtomicStandardGuildMessageChannel.class || typeClass == AtomicGuildChannel.class) {
            this.type = Type.Channel;
            this.typeString = "_channel";
        } else if (typeClass == AtomicMember.class) {
            this.type = Type.Member;
            this.typeString = "_user";
        }
    }

    public MessageInputResponse addData(List<T> newList, String inputString, Member author, int stateBack) {
        return addData(newList, inputString, author, stateBack, null);
    }

    public MessageInputResponse addData(List<T> newList, String inputString, Member member, int stateBack, BotLogEntity.Event logEvent) {
        if (newList.isEmpty()) {
            command.setLog(LogStatus.FAILURE, TextManager.getNoResultsString(command.getLocale(), inputString));
            return MessageInputResponse.FAILED;
        } else {
            if (type == Type.Role && !command.checkRolesWithLog(member, AtomicRole.to((List<AtomicRole>) newList), checkRolesHierarchy)) {
                return MessageInputResponse.FAILED;
            }

            GuildEntity guildEntity = command.getGuildEntity();
            List<T> srcList = srcListSupplier.apply(guildEntity);

            int existingRoles = 0;
            for (T t : newList) {
                if (srcList.contains(t)) {
                    existingRoles++;
                }
            }

            if (existingRoles >= newList.size()) {
                command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_exists" + typeString, newList.size() != 1));
                return MessageInputResponse.FAILED;
            }

            guildEntity.beginTransaction();
            ArrayList<T> added = new ArrayList<>();
            int n = 0;
            for (T t : newList) {
                if (!srcList.contains(t)) {
                    if (srcList.size() < max) {
                        srcList.add(t);
                        added.add(t);
                        n++;
                    }
                }
            }

            if (logEvent != null && !added.isEmpty()) {
                List<String> addedStrings;
                if (added.get(0) instanceof MentionableAtomicAsset<?>) {
                    addedStrings = JDAUtil.toIdList((List<MentionableAtomicAsset<?>>) added);
                } else {
                    addedStrings = added.stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
                }
                BotLogEntity.log(command.getEntityManager(), logEvent, member, addedStrings, null);
            }
            guildEntity.commitTransaction();

            command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_add" + typeString, n != 1, String.valueOf(n)));
            command.setState(stateBack);
            return MessageInputResponse.SUCCESS;
        }
    }

    public boolean removeData(int i, int stateBack) {
        return removeData(i, null, stateBack, null);
    }

    public boolean removeData(int i, Member member, int stateBack, BotLogEntity.Event logEvent) {
        List<T> srcList = srcListSupplier.apply(command.getGuildEntity());
        if (i == -1) {
            command.setState(stateBack);
            return true;
        } else if (i >= 0 && i < srcList.size()) {
            command.getGuildEntity().beginTransaction();
            T removed = srcList.remove(i);

            if (logEvent != null) {
                String removedString;
                if (removed instanceof MentionableAtomicAsset<?>) {
                    removedString = ((MentionableAtomicAsset<?>) removed).getId();
                } else {
                    removedString = removed.toString();
                }
                BotLogEntity.log(command.getEntityManager(), logEvent, member, null, removedString);
            }
            command.getGuildEntity().commitTransaction();

            command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_remove" + typeString));
            if (srcList.isEmpty()) {
                command.setState(stateBack);
            }
            return true;
        }

        return false;
    }

    public EmbedBuilder drawDataAdd() {
        return drawDataAdd(
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_add_title" + typeString),
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_add_desc" + typeString)
        );
    }

    public EmbedBuilder drawDataAdd(String title) {
        return drawDataAdd(
                title,
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_add_desc" + typeString)
        );
    }

    public EmbedBuilder drawDataAdd(String title, String desc) {
        return EmbedFactory.getEmbedDefault(command, desc, title);
    }

    public EmbedBuilder drawDataRemove(Locale locale) {
        return drawDataRemove(
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_remove_title" + typeString),
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_remove_desc" + typeString),
                locale
        );
    }

    public EmbedBuilder drawDataRemove(String title, Locale locale) {
        return drawDataRemove(
                title,
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_remove_desc" + typeString),
                locale
        );
    }

    public EmbedBuilder drawDataRemove(String title, String desc, Locale locale) {
        Function<T, String> nameFunction;
        if (type == Type.Unknown) {
            nameFunction = Object::toString;
        } else {
            nameFunction = obj -> ((MentionableAtomicAsset<?>) obj).getPrefixedName(locale);
        }

        List<T> srcList = srcListSupplier.apply(command.getGuildEntity());
        String[] strings = new String[srcList.size()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = nameFunction.apply(srcList.get(i));
        }
        command.setComponents(strings);
        return EmbedFactory.getEmbedDefault(command, desc, title);
    }

    public void startDataAdd(int stateNext) {
        List<T> srcList = srcListSupplier.apply(command.getGuildEntity());
        if (srcList.size() < max) {
            command.setState(stateNext);
        } else {
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_start_add_toomany" + typeString, String.valueOf(max)));
        }
    }

    public void startDataRemove(int stateNext) {
        List<T> srcList = srcListSupplier.apply(command.getGuildEntity());
        if (!srcList.isEmpty()) {
            command.setState(stateNext);
        } else {
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_start_remove_none" + typeString, String.valueOf(max)));
        }
    }

}