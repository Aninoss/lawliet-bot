package General;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Locale;

public class ListGen {
    public static String getUserList(Locale locale, ArrayList<User> users) throws IOException {
        StringBuilder sb = new StringBuilder();
        for(User user: users) {
            //sb.append("• ");
            sb.append(user.getMentionTag());
            sb.append("\n");
        }
        if (sb.toString().length() == 0) return TextManager.getString(locale, TextManager.GENERAL, "notset");
        return sb.toString();
    }

    public static String getRoleList(Locale locale, ArrayList<Role> roles) throws IOException {
        StringBuilder sb = new StringBuilder();
        for(Role role: roles) {
            //sb.append("• ");
            sb.append(role.getMentionTag());
            sb.append("\n");
        }
        if (sb.toString().length() == 0) return TextManager.getString(locale, TextManager.GENERAL, "notset");
        return sb.toString();
    }

    public static String getRoleListNumbered(Locale locale, ArrayList<Role> roles) throws IOException {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for(Role role: roles) {
            sb.append(i).append(") ");
            sb.append(role.getMentionTag());
            sb.append("\n");
            i++;
        }
        if (sb.toString().length() == 0) return TextManager.getString(locale, TextManager.GENERAL, "notset");
        return sb.toString();
    }

    public static String getChannelList(Locale locale, ArrayList<ServerTextChannel> channels) throws IOException {
        return getChannelList(TextManager.getString(locale, TextManager.GENERAL, "notset"), channels);
    }

    public static String getChannelList(String notSet, ArrayList<ServerTextChannel> channels) {
        StringBuilder sb = new StringBuilder();
        for(ServerTextChannel channel: channels) {
            sb.append(channel.getMentionTag());
            sb.append("\n");
        }
        if (sb.toString().length() == 0) return notSet;
        return sb.toString();
    }
}
