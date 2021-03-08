package commands.runnables.gimmickscategory;

import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.RandomPicker;
import core.utils.MentionUtil;
import modules.graphics.ShipGraphics;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Random;

@CommandProperties(
    trigger = "ship",
    botPermissions = PermissionDeprecated.ATTACH_FILES,
    withLoadingBar = true,
    emoji = "\uD83D\uDC6B",
    executableWithoutArgs = false
)
public class ShipCommand extends Command {

    public ShipCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Message message = event.getMessage();
        ArrayList<User> list = MentionUtil.getMembers(message,followedString).getList();
        if (list.size() == 1 && list.get(0).getId() != event.getMessage().getUserAuthor().get().getId()) {
            list.add(event.getMessage().getUserAuthor().get());
        }
        if (list.size() != 2) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    getString( "not_2"))).get();
            return false;
        }

        list.sort(Comparator.comparingLong(DiscordEntity::getId));
        String idString = String.valueOf(list.get(0).getId() + list.get(1).getId());
        int randomNum = String.valueOf(idString.hashCode()).hashCode();
        int percentage = new Random(randomNum).nextInt(101);

        if (list.get(0).getId() == 272037078919938058L && list.get(1).getId() == 326714012022865930L)
            percentage = 100;
        if (list.get(0).getId() == 397209883793162240L && list.get(1).getId() == 710120672499728426L)
            percentage = 100;

        int n = RandomPicker.getInstance().pick(getTrigger(), event.getGuild().getIdLong(), 7);
        if (event.getGuild().getIdLong() == 580048842020487180L) n = 7;

        InputStream is = ShipGraphics.createImageShip(list.get(0), list.get(1), n, percentage);
        if (is == null) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("noavatar"))).get();
            return false;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setImage(is);
        event.getChannel().sendMessage(eb).get();

        return true;
    }

}
