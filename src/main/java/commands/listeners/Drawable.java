package commands.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;

public interface Drawable {

    EmbedBuilder draw(Member member) throws Throwable;

}
