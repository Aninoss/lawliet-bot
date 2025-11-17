package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.FileManager;
import core.LocalFile;
import core.RandomPicker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "nsfwtopic",
        emoji = "\uD83E\uDD60",
        executableWithoutArgs = true,
        nsfw = true,
        aliases = { "nsfwtopics" }
)
public class NSFWTopicCommand extends TopicCommand {

    public NSFWTopicCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder getEmbed(GuildMessageChannel channel) throws IOException, ExecutionException, InterruptedException {
        List<String> topicList = FileManager.readInList(new LocalFile(LocalFile.Directory.RESOURCES, "nsfw_topics_" + getLocale().getDisplayName() + ".txt"));
        int n = RandomPicker.pick(getTrigger(), channel.getGuild().getIdLong(), topicList.size()).get();
        String topic = topicList.get(n);

        return EmbedFactory.getEmbedDefault(this, topic);
    }

}