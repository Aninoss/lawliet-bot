package commands.runnables.gimmickscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.MainLogger;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.anilist.AnilistCharacter;
import modules.anilist.AnilistDownloader;
import mysql.hibernate.entity.SmashOrPassCharacterEntity;
import mysql.hibernate.entity.user.UserEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.RollbackException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "smashorpass",
        emoji = "🔥",
        executableWithoutArgs = true
)
public class SmashOrPassCommand extends Command implements OnButtonListener {

    private static final int MAX_CHARACTERS_PER_CYCLE = 30;
    private static final String BUTTON_ID_SMASH = "smash";
    private static final String BUTTON_ID_PASS = "pass";

    public SmashOrPassCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        registerButtonListener(event.getMember());
        return true;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        UserEntity userEntity = getUserEntity();
        int index = getIndex();
        SmashOrPassCharacterEntity character = getCharacter(index);

        getEntityManager().getTransaction().begin();
        if (event.getComponentId().equals(BUTTON_ID_SMASH)) {
            character.getSmashUserIds().add(event.getUser().getIdLong());
            character.getPassUserIds().remove(event.getUser().getIdLong());
        } else if (event.getComponentId().equals(BUTTON_ID_PASS)) {
            character.getSmashUserIds().remove(event.getUser().getIdLong());
            character.getPassUserIds().add(event.getUser().getIdLong());
        }
        userEntity.setSmashOrPassIndex(userEntity.getSmashOrPassIndex() + 1);
        getEntityManager().getTransaction().commit();
        return true;
    }

    @Nullable
    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        setComponents(
                Button.of(ButtonStyle.SUCCESS, BUTTON_ID_SMASH, getString("smash")).withEmoji(Emoji.fromUnicode("🔥")),
                Button.of(ButtonStyle.DANGER, BUTTON_ID_PASS, getString("pass")).withEmoji(Emoji.fromUnicode("✖️"))
        );

        int index = getIndex();
        SmashOrPassCharacterEntity character = getCharacter(index);

        String desc = getString(character.getGender() != null ? "desc" : "desc_nogender", character.getAge(), character.getGender() != null ? translateGender(character.getGender()) : "");
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, desc)
                .setTitle(character.getName())
                .setUrl(character.getCharacterUrl())
                .setImage(character.getImageUrl());
        EmbedUtil.setFooter(eb, this, getString("index", StringUtil.numToString(index + 1), StringUtil.numToString(MAX_CHARACTERS_PER_CYCLE)));
        if (character.getMediaName() != null) {
            eb.setAuthor(character.getMediaName(), character.getMediaUrl());
        }
        return eb;
    }

    private SmashOrPassCharacterEntity getCharacter(int index) throws ExecutionException, InterruptedException {
        List<SmashOrPassCharacterEntity> characters = SmashOrPassCharacterEntity.findAll(getEntityManager());
        if (index < characters.size()) {
            return characters.get(index);
        } else {
            boolean preferFemale = ThreadLocalRandom.current().nextInt(3) < 2;
            AnilistCharacter randomCharacter = getRandomCharacter(preferFemale);

            SmashOrPassCharacterEntity smashOrPassCharacterEntity = new SmashOrPassCharacterEntity(randomCharacter, index);
            try {
                getEntityManager().getTransaction().begin();
                getEntityManager().persist(smashOrPassCharacterEntity);
                getEntityManager().getTransaction().commit();
                return smashOrPassCharacterEntity;
            } catch (RollbackException e) {
                MainLogger.get().warn("Rollback exception on index {} for {}", index, SmashOrPassCharacterEntity.class);
                return getCharacter(index);
            }
        }
    }

    private int getIndex() {
        UserEntity userEntity = getUserEntity();
        int year = TimeUtil.getCurrentYear();
        int week = TimeUtil.getCurrentWeekInYear();

        if (year != userEntity.getSmashOrPassYear() || week != userEntity.getSmashOrPassWeek()) {
            userEntity.beginTransaction();
            userEntity.setSmashOrPassIndex(0);
            userEntity.setSmashOrPassYear(year);
            userEntity.setSmashOrPassWeek(week);
            userEntity.commitTransaction();
            return 0;
        }
        return userEntity.getSmashOrPassIndex();
    }

    private AnilistCharacter getRandomCharacter(boolean preferFemale) throws ExecutionException, InterruptedException {
        List<AnilistCharacter> newCharacters = AnilistDownloader.getCharacters(ThreadLocalRandom.current().nextInt(100));
        List<AnilistCharacter> filteredCharacters = newCharacters.stream()
                .filter(c -> c.getGender() == null ||
                        (!c.getGender().equals("Male") && preferFemale) ||
                        (!c.getGender().equals("Female") && !preferFemale)
                )
                .collect(Collectors.toList());

        if (filteredCharacters.isEmpty()) {
            return getRandomCharacter(preferFemale);
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(filteredCharacters.size());
        return filteredCharacters.get(randomIndex);
    }

    private String translateGender(String gender) {
        return switch (gender) {
            case "Male" -> getString("male");
            case "Female" -> getString("female");
            default -> gender;
        };
    }

}
