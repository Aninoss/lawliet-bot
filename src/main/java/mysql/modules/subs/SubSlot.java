package mysql.modules.subs;

import java.util.Locale;
import java.util.Observable;
import commands.Category;
import core.TextManager;
import core.assets.UserAsset;
import core.utils.JDAUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.Button;

public class SubSlot extends Observable implements UserAsset {

    private final DBSubs.Command command;
    private final long userId;
    private final Locale locale;
    private int errors;

    public SubSlot(DBSubs.Command command, long userId, Locale locale, int errors) {
        this.command = command;
        this.userId = userId;
        this.locale = locale;
        this.errors = errors;
    }

    public DBSubs.Command getCommand() {
        return command;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    public Locale getLocale() {
        return locale;
    }

    public int getErrors() {
        return errors;
    }

    public void sendEmbed(Locale locale, EmbedBuilder eb, Button... buttons) {
        eb.setFooter(TextManager.getString(locale, Category.FISHERY, "cooldowns_footer"));
        JDAUtil.sendPrivateMessage(userId, eb.build(), buttons).queue(v -> {
            if (errors > 0) {
                errors = 0;
                setChanged();
                notifyObservers();
            }
        }, e -> {
            if (++errors >= 3) {
                DBSubs.getInstance().retrieve(command).remove(userId);
            } else {
                setChanged();
                notifyObservers();
            }
        });
    }

}
