package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import Commands.EmoteAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "blush",
        emoji = "\uD83D\uDE0A",
        executable = true
)
public class BlushCommand extends EmoteAbstract {

    public BlushCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736260129418248242/736260135047135362/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260147059490906/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260154139476069/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260165745246278/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260172921438218/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260189581344908/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260196074258522/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260203049386102/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260210552864876/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260218740146287/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260222775066664/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260230584860792/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260236679315516/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260245785149541/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260250818183248/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260255016681472/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260274239176805/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260284490055701/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260297274163332/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260305075830814/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260315376910366/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260322247180348/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260328450687026/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260336511877130/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260343310844004/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260355264741376/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260358343491705/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260365134069948/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260369206739014/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260371517800598/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260375733076019/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260382468997260/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260399644540978/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260407290888304/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260419349512212/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260423002882088/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260429545996288/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260432062447706/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260435606634618/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260449913274508/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260463192703081/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260469995601981/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260473602703501/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260486722748485/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260494507114526/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260497627676732/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260508885319770/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260517336842300/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260521237676113/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260527180873848/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260532805304450/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260546692775976/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260558734622782/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260566364061746/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/736260573729259550/blush.gif",
                "https://media.discordapp.net/attachments/736260129418248242/750018308144758865/blush.gif"
        };
    }

}
