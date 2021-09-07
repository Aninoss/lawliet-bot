package commands;

import java.util.List;
import java.util.function.Function;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import commands.listeners.MessageInputResponse;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicTextChannel;
import core.atomicassets.MentionableAtomicAsset;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;

public class NavigationHelper<T> {

    private enum Type { Unknown, Role, TextChannel, Member }

    private final NavigationAbstract command;
    private final List<T> srcList;
    private final int max;
    private Type type = Type.Unknown;
    private String typeString = "";

    public NavigationHelper(NavigationAbstract command, List<T> srcList, Class<T> typeClass, int max) {
        this.command = command;
        this.srcList = srcList;
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
            if (type == Type.Role && !command.checkRolesWithLog(author, AtomicRole.to((List<AtomicRole>) newList))) {
                return MessageInputResponse.FAILED;
            }

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

            int n = 0;
            for (T t : newList) {
                if (!srcList.contains(t)) {
                    if (srcList.size() < max) {
                        srcList.add(t);
                        n++;
                    }
                }
            }

            command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_add" + typeString, n != 1, String.valueOf(n)));
            command.setState(stateBack);
            return MessageInputResponse.SUCCESS;
        }
    }

    public boolean removeData(int i, int stateBack) {
        if (i == -1) {
            command.setState(stateBack);
            return true;
        } else if (i >= 0 && i < srcList.size()) {
            srcList.remove(i);
            command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_remove" + typeString));
            if (srcList.size() == 0) command.setState(stateBack);
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

    public EmbedBuilder drawDataRemove() {
        return drawDataRemove(
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_remove_title" + typeString),
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_remove_desc" + typeString)
        );
    }

    public EmbedBuilder drawDataRemove(String title) {
        return drawDataRemove(
                title,
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_remove_desc" + typeString)
        );
    }

    public EmbedBuilder drawDataRemove(String title, String desc) {
        Function<T, String> nameFunction;
        if (type == Type.Unknown) {
            nameFunction = Object::toString;
        } else {
            nameFunction = obj -> ((MentionableAtomicAsset<?>) obj).getName();
        }

        String[] strings = new String[srcList.size()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = nameFunction.apply(srcList.get(i));
        }
        command.setComponents(strings);
        return EmbedFactory.getEmbedDefault(command, desc, title);
    }

    public void startDataAdd(int stateNext) {
        if (srcList.size() < max) {
            command.setState(stateNext);
        } else {
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_start_add_toomany" + typeString, String.valueOf(max)));
        }
    }

    public void startDataRemove(int stateNext) {
        if (srcList.size() > 0) {
            command.setState(stateNext);
        } else {
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_start_remove_none" + typeString, String.valueOf(max)));
        }
    }

}