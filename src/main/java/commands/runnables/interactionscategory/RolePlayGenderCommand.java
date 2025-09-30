package commands.runnables.interactionscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStringSelectMenuListener;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import mysql.hibernate.entity.user.RolePlayGender;
import mysql.hibernate.entity.user.UserEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "rpgender",
        emoji = "♂️",
        executableWithoutArgs = true,
        aliases = { "roleplaygender" }
)
public class RolePlayGenderCommand extends Command implements OnStringSelectMenuListener {

    private boolean set = false;

    public RolePlayGenderCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        for (RolePlayGender gender : RolePlayGender.values()) {
            if (args.equalsIgnoreCase(gender.getId())) {
                UserEntity userEntity = getUserEntity();
                userEntity.beginTransaction();
                userEntity.setRolePlayGender(gender);
                userEntity.commitTransaction();

                set = true;
                drawMessageNew(draw(event.getMember()));
                return true;
            }
        }

        if (!args.isEmpty()) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
        }
        registerStringSelectMenuListener(event.getMember());
        return true;
    }

    @Override
    public boolean onStringSelectMenu(StringSelectInteractionEvent event) {
        RolePlayGender newGender = RolePlayGender.valueOf(event.getSelectedOptions().get(0).getValue());

        UserEntity userEntity = getUserEntity();
        userEntity.beginTransaction();
        userEntity.setRolePlayGender(newGender);
        userEntity.commitTransaction();
        set = true;
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        RolePlayGender currentGender = getUserEntityReadOnly().getRolePlayGender();
        if (!set) {
            StringSelectMenu.Builder builder = StringSelectMenu.create("gender");
            for (RolePlayGender gender : RolePlayGender.values()) {
                builder.addOption(
                        getString("gender_" + gender.getId()),
                        gender.name()
                );
            }
            builder.setDefaultValues(List.of(currentGender.name()));
            setComponents(builder.build());
        }

        String genderString = getString("gender_" + currentGender.getId());
        return EmbedFactory.getEmbedDefault(this, getString(set ? "set" : "select", genderString));
    }

}
