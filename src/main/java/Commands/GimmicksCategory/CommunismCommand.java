package Commands.GimmicksCategory;

import CommandListeners.CommandProperties;
import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.RandomPicker;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Random;

@CommandProperties(
        trigger = "communism",
        emoji = "\uD83C\uDF39",
        executable = true,
        exlusiveServers = { 462405241955155979L },
        aliases = {"socialism"}
)
public class CommunismCommand extends Command {

    static final String[] FACTS = {
            "The word 'communism' is derived from the French word 'communisme' which originally was used to describe social situations as opposed to political or economic states.",
            "Hunter-gatherer societies were considered to be early forms of communist societies. The Bible refers to a type of common ownership that some consider to have been a type of primitive communism.", 
            "Karl Marx is believed to be the founder of modern communism, and his theories have also been referred to as Marxism.", 
            "Within Karl Marx ideas about communism he believed these practices would remove social classes, money, and the state.", 
            "In theory communism would have put everyone on equal footing, but in practice communism allowed governments to control citizens because the fundamental basics were distorted.", 
            "Governments that have used Marxism as a basis for their politics have used their power to treat citizens poorly, controlling them and even killing them when necessary to keep those who opposed the government from rising up against them.", 
            "The first communist leader to come into power was Vladimir Lenin, following the 1917 Russian Revolution.", 
            "China became a communist country in 1949, and Cuba became communist in 1959. Vietnam became communist in 1975.", 
            "The types of communism that have been used by different leaders, countries, and groups include anarchist communism, anti-revisionism, Castroism, Council, Euro, Guevarism, Ho Chi Minh Thought, Juche, Left, Leninism, Luxemburgism, Marxism-Leninism, Christian Religious, Socialism with Chinese characteristics, Titoism, and Trotskyism.", 
            "The Cold War began when the Western Allies (United States and NATO allies) and the Eastern Bloc (Soviet Union and its Warsaw Pact allies) became engaged in a power struggle that resulted in several crises such as the Cuban Missile Crisis in 1962. The struggle was essentially seen as a fight against communism by the Western Allies.", 
            "Russia, China, North Korea, Cuba, Vietnam, and Laos are considered to be communist countries.", 
            "Communism has failed in practice because the governments have forgotten about the democratic aspect of the ideology which results in the splitting of social classes - which was communism wanted to abolish in the first place.", 
            "Communism in theory is a type of society where people are all equal. In reality it became a type of dictatorship as the fundamental basics of communism and removing social class became lost.", 
            "Some of the most famous communist leaders and activists in history include Vladimir Lenin, Fidel Castro, Raul Castro, Leon Trotsky, Pol Pot, Nikita Khrushchev, Kim Il-Sung, Imre Nagy, Jiang Zemin, Ho Chi-Minh, and Joseph Stalin. "
    };

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        int n = RandomPicker.getInstance().pick(getTrigger(), event.getServer().get().getId(), FACTS.length);
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, FACTS[n]);
        event.getChannel().sendMessage(eb).get();
        return true;
    }

}
