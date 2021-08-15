package mysql.modules.subs;

import java.util.Locale;
import core.assets.UserAsset;

public class SubSlot implements UserAsset {

    private final DBSubs.Command command;
    private final long userId;
    private final Locale locale;

    public SubSlot(DBSubs.Command command, long userId, Locale locale) {
        this.command = command;
        this.userId = userId;
        this.locale = locale;
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

}
