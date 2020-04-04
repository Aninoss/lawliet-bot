package MySQL.Donators;

import General.DiscordApiCollection;
import org.javacord.api.entity.user.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Observable;
import java.util.Optional;

public class DonatorBeanSlot extends Observable {

    private long userId;
    private LocalDate donationEnd;

    public DonatorBeanSlot(long userId, LocalDate donationEnd) {
        this.userId = userId;
        this.donationEnd = donationEnd;
    }


    /* Getters */

    public long getUserId() {
        return userId;
    }

    public Optional<User> getUser() { return DiscordApiCollection.getInstance().getUserById(userId); }

    public LocalDate getDonationEnd() {
        return donationEnd;
    }

    public boolean isValid() {
        return donationEnd.isAfter(LocalDate.now());
    }


    /* Setters */

    public void addWeeks(int weeks) {
        if (weeks > 0) {
            donationEnd = donationEnd.plus(weeks, ChronoUnit.WEEKS);
            setChanged();
            notifyObservers();
        }
    }

}
