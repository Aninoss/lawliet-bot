package commands.runnables;

import java.util.Locale;
import commands.Command;
import core.EmbedFactory;
import core.RandomPicker;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class EmoteAbstract extends Command {

    private final String[] gifs;

    public EmoteAbstract(Locale locale, String prefix) {
        super(locale, prefix);
        this.gifs = getGifs();
    }

    protected abstract String[] getGifs();

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        String gifUrl = gifs[RandomPicker.getInstance().pick(getTrigger(), event.getGuild().getIdLong(), gifs.length)];

        String quote = "";
        if (args.length() > 0)
            quote = "\n\n> " + args.replace("\n", "\n> ");

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this,
                getString("template", "**" + StringUtil.escapeMarkdown(event.getMessage().getMember().getEffectiveName()) + "**") + quote
        ).setImage(gifUrl);

        event.getMessage().getChannel().sendMessage(eb.build()).queue();
        return true;
    }

}
