package core;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class EmbedWithContent extends MessageBuilder {

    public EmbedWithContent(String content, MessageEmbed eb) {
        setContent(content);
        setEmbed(eb);
    }

}
