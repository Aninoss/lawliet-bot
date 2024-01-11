package mysql.modules.subs;

import commands.Category;
import constants.AssetIds;
import core.Program;
import core.ShardManager;
import core.TextManager;
import core.assets.UserAsset;
import core.cache.UserBannedCache;
import core.components.ActionRows;
import core.utils.JDAUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.Locale;
import java.util.Observable;

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
        if ((!Program.publicInstance() && userId == AssetIds.OWNER_USER_ID) ||
                UserBannedCache.getInstance().isBanned(userId)
        ) {
            return;
        }

        eb.setFooter(TextManager.getString(locale, Category.FISHERY, "cooldowns_footer"));
        JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId)
                .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()).setComponents(ActionRows.of(buttons)))
                .queue(v -> {
                    if (errors > 0) {
                        errors = 0;
                        setChanged();
                        notifyObservers();
                    }
                }, e -> {
                    if (++errors >= 4) {
                        DBSubs.getInstance().retrieve(command)
                                .remove(userId);
                    } else {
                        setChanged();
                        notifyObservers();
                    }
                });
    }

}
