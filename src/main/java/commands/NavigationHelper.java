package commands;

import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public class NavigationHelper<T> {

    private enum Type { Unknown, Role, TextChannel, User };

    private Command command;
    private List<T> srcList;
    private int max;
    private Type type = Type.Unknown;
    private String typeString = "";

    public NavigationHelper(Command command, List<T> srcList, Class<T> typeClass, int max) {
        this.command = command;
        this.srcList = srcList;
        this.max = max;

        if (typeClass == Role.class) {
            this.type = Type.Role;
            this.typeString = "_role";
        } else if (typeClass == ServerTextChannel.class) {
            this.type = Type.TextChannel;
            this.typeString = "_channel";
        } else if (typeClass == User.class) {
            this.type = Type.User;
            this.typeString = "_user";
        }
    }

    public Response addData(List<T> newList, String inputString, User author, int stateBack) throws IOException {
        if (newList.size() == 0) {
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_results_description", inputString));
            return Response.FALSE;
        } else {
            if (type == Type.Role && !command.checkRolesWithLog((List<Role>) newList, author)) return Response.FALSE;

            int existingRoles = 0;
            for(T t: newList) {
                if (srcList.contains(t)) existingRoles ++;
            }

            if (existingRoles >= newList.size()) {
                command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_exists" + typeString, newList.size() != 1));
                return Response.FALSE;
            }

            int n = 0;
            for(T t: newList) {
                if (!srcList.contains(t)) {
                    if (srcList.size() < max) {
                        srcList.add(t);
                        n++;
                    }
                }
            }

            command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_add" + typeString, n != 1, String.valueOf(n)));
            command.setState(stateBack);
            return Response.TRUE;
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

    public EmbedBuilder drawDataAdd() throws IOException {
        return drawDataAdd(
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_add_title" + typeString),
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_add_desc" + typeString)
        );
    }

    public EmbedBuilder drawDataAdd(String title, String desc) throws IOException {
        return EmbedFactory.getEmbedDefault(command, desc, title);
    }

    public EmbedBuilder drawDataRemove() throws IOException {
        return drawDataRemove(
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_remove_title" + typeString),
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "element_draw_remove_desc" + typeString)
        );
    }

    public EmbedBuilder drawDataRemove(String title, String desc) throws IOException {
        Function<T, String> nameFunction;
        if (type == Type.Unknown) nameFunction = Object::toString;
        else nameFunction = obj -> ((Mentionable)obj).getMentionTag();

        String[] strings = new String[srcList.size()];
        for(int i = 0; i < strings.length; i++) {
            strings[i] = nameFunction.apply(srcList.get(i));
        }
        command.setOptions(strings);
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