package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "fuck",
        emoji = "\uD83D\uDECF️",
        executable = false,
        nsfw = true
)
public class FuckCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://www.rencontresanslendemain.net/wp-content/uploads/2018/02/gif-sexe-hentai.gif",
                "http://hentai.bestsexphotos.eu/wp-content/uploads/2017/01/tumblr_ofkrfhY8dG1u2uu04o1_500.gif",
                "https://thumb-p0.xhcdn.com/a/ZE1ZEIk98Qy-nrbUAc6Z7g/000/127/270/110_1000.gif",
                "https://www.rencontresanslendemain.net/wp-content/uploads/2018/02/hentai-levrette.gif",
                "https://commentseduire.net/wp-content/uploads/2017/06/hentai-gif-10.gif",
                "http://img1.joyreactor.com/pics/post/Tengen-Toppa-Gurren-Lagann-anime-Yoko-Littner-Hentai-Gif-1901175.gif",
                "http://hentai.bestsexphotos.eu/wp-content/uploads/2017/04/tumblr_ogjr4lgzyQ1vkefi9o1_500.gif",
                "http://xxxpicz.com/xxx/persona-hentai-gif-xxx-1.gif",
                "https://66.media.tumblr.com/tumblr_ma9ipxSuWp1rdw7hvo1_500.gif",
                "https://www.mydirtybook.de/media/com_easysocial/photos/9656/15358/122-bb15_original.gif",
                "https://i.pinimg.com/originals/c9/ad/a5/c9ada543f2bad0bed7f2ddf10e13f085.gif",
                "https://multporn.net/sites/default/files/gif/hentai/1673089_-_dragons_crown_saltyicecream_sorceress_animated.gif",
                "https://sinnercomics.com/wp-content/uploads/hentai-onahole-moe-sex-gif.gif",
                "https://i.pinimg.com/originals/36/96/e6/3696e653a443bda81b84dbce082093d0.gif",
                "https://cdn.discordapp.com/attachments/503690734000668712/528285268265140251/97177d6512d83031026ffa4c71ed7859.gif",
                "https://cdn.discordapp.com/attachments/503690734000668712/528285918906548235/uncensored-hentai-gif-wet-pussy-fucking-brunette-hair.gif",
                "https://cdn.discordapp.com/attachments/503690734000668712/528285927202750485/n.gif",
                "https://cdn.discordapp.com/attachments/503690734000668712/528286892085608468/anime-masturbation-hentaiii-pinsex-15364.gif",
                "https://cdn.discordapp.com/attachments/503690734000668712/528287036839297033/orgasm-uncensored-hentai-gif.gif",
                "https://66.media.tumblr.com/9caf0c4f727b98735c130a385c0e2d6e/tumblr_pfkvgnRoMC1vuqxq6o1_1280.gif",
                "https://66.media.tumblr.com/12d317da7ef53995c36b3eab33483334/tumblr_op6sq1bwTb1tjgwy0o2_540.gif",
                "https://66.media.tumblr.com/78915b8ff45ff1d54693780558d1847b/tumblr_ov6ebgUiaC1tjgwy0o1_540.gif",
                "https://img2.gelbooru.com//images/35/58/3558eab0004c64b0d29966219b0d2056.gif",
                "https://img2.gelbooru.com//images/01/de/01def549529e582db36c90ec5727ce42.gif",
                "https://img2.gelbooru.com//images/87/0e/870ea91467fd0ee0abeef12ceb8d6d63.gif",
                "https://img2.gelbooru.com//images/84/84/848422e55b8b2778e4fb73cad8725fdc.gif",
                "https://img2.gelbooru.com//images/18/57/1857365cffdeaabaef57258d4ac94430.gif",
                "https://img2.gelbooru.com//images/77/fe/77fe9ba14dea38888e08d9e82c567d27.gif",
                "https://img2.gelbooru.com//images/75/df/75df1bacc950caa318e87d6b48d040f9.gif",
                "https://img2.gelbooru.com//images/10/f1/10f1bd78ae4da6600238fa87025698c7.gif",
                "https://img2.gelbooru.com//images/71/47/7147b94c1180cd3a0bacb4d8610520aa.gif",
                "https://img2.gelbooru.com//images/da/7d/da7ddc248f53bd2145495ec8e748bd5a.gif",
                "https://img2.gelbooru.com//images/9c/68/9c6821120e7ab527f3686fae1ddc05c7.gif",
                "https://img2.gelbooru.com//images/8e/7e/8e7e5078e4bb6381df69378f7e15ab2d.gif",
                "https://img2.gelbooru.com//images/b4/08/b408b81e11e099474a89c33fd611fb03.gif",
                "https://img2.gelbooru.com//images/07/39/0739a815b263eaba2eae6f10f7413fdb.gif",
                "https://img2.gelbooru.com//images/f5/37/f5378865cecd2232dbc18c601c98a963.gif",
                "https://img2.gelbooru.com//images/dd/c7/ddc7163b5ab871f69c377a8d796cd69d.gif",
                "https://img2.gelbooru.com//images/33/57/335759f705e97c35001c50f08923c27b.gif",
                "https://img2.gelbooru.com//images/cd/8e/cd8ea33ace8040834321beb3d3b3be04.gif",
                "https://img2.gelbooru.com//images/4f/f7/4ff73373fcb15e7e1371ab086d262860.gif",
                "https://img2.gelbooru.com//images/75/4a/754ae364fd7434d16bff143b3c34a8f9.gif",
                "https://img2.gelbooru.com//images/5c/11/5c11902c260b1331a354a8c5238129dd.gif",
                "https://img2.gelbooru.com//images/0e/39/0e390708d82d27497b0037f2ea7cc7ab.gif",
                "https://img2.gelbooru.com//images/8f/ab/8fabb3244e4d60a1a980b824f0e47551.gif",
                "https://img2.gelbooru.com//images/b2/aa/b2aa6ee2f7cfb53b2883816f4551b048.gif",
                "https://img2.gelbooru.com//images/da/05/da05e0c4687980126c8d3811d1af8ddc.gif",
                "https://img2.gelbooru.com//images/5a/99/5a997dbf972d7c894bcc95962497e364.gif",
                "https://img2.gelbooru.com//images/46/fb/46fbf211a900be0410d880bfa7a2a666.gif",
                "https://img2.gelbooru.com//images/f1/56/f156346cec38d61ddb8269de327e545e.gif",
                "https://img2.gelbooru.com//images/d6/9f/d69f8049cfa898a58eb078d5b0f0b540.gif",
                "https://img2.gelbooru.com//images/c1/f3/c1f36bbd19795ba46d2d842fb5ae6728.gif"
        };
    }

}
