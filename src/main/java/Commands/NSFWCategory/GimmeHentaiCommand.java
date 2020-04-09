package Commands.NSFWCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.Tools.RandomTools;
import Core.Tools.StringTools;
import Core.Internet.Internet;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
    trigger = "gimmehentai",
    nsfw = true,
    withLoadingBar = true,
    emoji = "\uD83D\uDD1E",
    executable = true
)
public class GimmeHentaiCommand extends Command {
    
    private static ArrayList<Integer> picked = new ArrayList<>();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        String hentaiText = null;
        int n = RandomTools.pickFullRandom(picked,10);

        switch (n) {
            case 0:
                hentaiText = "Ail Maniax: Inma Seifukugari & Majogari no Yoru ni\nhttps://myanimelist.net/anime/2350/Ail_Maniax__Inma_Seifukugari___Majogari_no_Yoru_ni";
                break;
            case 1:
                hentaiText = "Euphoria\nhttps://myanimelist.net/anime/10851/Euphoria";
                break;
            case 2:
                hentaiText = "Blood Royale\nhttps://myanimelist.net/anime/1386/Blood_Royale";
                break;
            case 3:
                hentaiText = "Boku no Pico\nhttps://myanimelist.net/anime/1639/Boku_no_Pico";
                break;
            case 4:
                hentaiText = "Bondage Game: Shinsou no Reijoutachi - Shinsou no Doreitachi\nhttps://myanimelist.net/anime/6328/Bondage_Game__Shinsou_no_Reijoutachi_-_Shinsou_no_Doreitachi";
                break;
            case 5:
                hentaiText = "Houkago: Nureta Seifuku\nhttps://myanimelist.net/anime/2896/Houkago__Nureta_Seifuku";
                break;
            case 6:
                hentaiText = "Kurohime: Shikkoku no Yakata\nhttps://myanimelist.net/anime/1385/Kurohime__Shikkoku_no_Yakata";
                break;
            case 7:
                hentaiText = "Pigeon Blood\nhttps://myanimelist.net/anime/2351/Pigeon_Blood";
                break;
            case 8:
                hentaiText = "Shintaisou: Kari\nhttps://myanimelist.net/anime/1633/Shintaisou__Kari";
                break;
            case 9:
                hentaiText = "Uchiyama Aki\nhttps://myanimelist.net/anime/3644/Uchiyama_Aki";
                break;
        }

        String data = Internet.getData(hentaiText.split("\n")[1]).get().getContent().get();
        String cover = StringTools.extractGroups(data,"<meta property=\"og:image\" content=\"","\">")[0];
        String desc = StringTools.shortenString(StringTools.decryptString(StringTools.extractGroups(data,"<meta property=\"og:description\" content=\"","\">")[0]),1024);

        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this,
                getString("template",hentaiText))
                .setThumbnail(cover)
                .addField(getString("animetitle"),hentaiText.split("\n")[0],true)
                .addField(getString("url"),"[\uD83D\uDD17 MyAnimeList]("+hentaiText.split("\n")[1]+")",true)
                .addField(getString("synopsis"),desc)).get();
        return true;
    }
}
