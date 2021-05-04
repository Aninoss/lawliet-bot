package websockets.syncserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import commands.Command;
import constants.AssetIds;
import constants.Language;
import core.ListGen;
import core.TextManager;
import net.dv8tion.jda.api.Permission;
import org.json.JSONObject;

public class SyncLocaleUtil {

    private SyncLocaleUtil() {
    }

    public static JSONObject getLanguagePack(String category, String key) {
        JSONObject jsonObject = new JSONObject();

        for (Language language : Language.values()) {
            Locale locale = language.getLocale();
            jsonObject.put(locale.getDisplayName(), TextManager.getString(locale, category, key).replace("{PREFIX}", "L."));
        }

        return jsonObject;
    }

    public static JSONObject getCommandPermissions(Command command) {
        JSONObject jsonObject = new JSONObject();

        for (Language language : Language.values()) {
            Locale locale = language.getLocale();
            ArrayList<Permission> permissionList = new ArrayList<>(Arrays.asList(command.getCommandProperties().userGuildPermissions()));
            permissionList.addAll(Arrays.asList(command.getCommandProperties().userChannelPermissions()));

            String permissionsList = new ListGen<Permission>().getList(
                    permissionList, "", ListGen.SLOT_TYPE_NONE,
                    permission -> TextManager.getString(locale, TextManager.PERMISSIONS, permission.name())
            );
            jsonObject.put(locale.getDisplayName(), permissionsList);
        }

        return jsonObject;
    }

    public static JSONObject getCommandSpecs(String commandCategory, String key, String commandTrigger) {
        JSONObject jsonObject = new JSONObject();

        for (Language language : Language.values()) {
            Locale locale = language.getLocale();
            String str = solveVariablesOfCommandText(TextManager.getString(locale, commandCategory, key));
            if (!str.isEmpty()) {
                str = ("\n" + str).replace("\n", "\nL." + commandTrigger + " ").substring(1);
            }

            jsonObject.put(locale.getDisplayName(), str);
        }

        return jsonObject;
    }

    private static String solveVariablesOfCommandText(String string) {
        return string
                .replace("{#CHANNEL}", "#welcome")
                .replace("{MESSAGE_ID}", "557961653975515168")
                .replace("{CHANNEL_ID}", "557953262305804310")
                .replace("{GUILD_ID}", String.valueOf(AssetIds.SUPPORT_SERVER_ID))
                .replace("{@USER}", "@Aninoss#7220")
                .replace("{@BOT}", "@Lawliet#5480")
                .replace("%Prefix", "L.");
    }

}
