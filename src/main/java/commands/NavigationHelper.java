package commands;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicTextChannel;
import core.atomicassets.MentionableAtomicAsset;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class NavigationHelper<T> {

    private enum Type {Unknown, Role, TextChannel, Member}

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
        } else if (typeClass == AtomicTextChannel.class) {
            this.type = Type.TextChannel;
            this.typeString = "_channel";
        } else if (typeClass == AtomicMember.class) {
            this.type = Type.Member;
            this.typeString = "_user";
        }
    }

    public MessageInputResponse addData(List<T> newList, String inputString, Member author, int stateBack) {
        if (newList.size() == 0) {
            command.setLog(LogStatus.FAILURE, TextManager.getNoResultsString(command.getLocale(), inputString));
            return MessageInputResponse.FAILED;
        } else {
            if (type == Type.Role && !command.checkRolesWithLog(author, AtomicRole.to((List<AtomicRole>) newList), checkRolesHierarchy)) {
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
            int n = 0;
            for (T t : newList) {
                if (!srcList.contains(t)) {
                    if (srcList.size() < max) {
                        srcList.add(t);
                        n++;
                    }
                }
            }
            guildEntity.commitTransaction();

            command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_add" + typeString, n != 1, String.valueOf(n)));
            command.setState(stateBack);
            return MessageInputResponse.SUCCESS;
        }
    }

    public boolean removeData(int i, int stateBack) {
        List<T> srcList = srcListSupplier.apply(command.getGuildEntity());
        if (i == -1) {
            command.setState(stateBack);
            return true;
        } else if (i >= 0 && i < srcList.size()) {
            command.getGuildEntity().beginTransaction();
            srcList.remove(i);
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