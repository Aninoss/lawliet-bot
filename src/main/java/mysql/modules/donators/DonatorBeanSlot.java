package mysql.modules.donators;

import core.DiscordApiCollection;
import org.javacord.api.entity.user.User;
import java.time.LocalDate;
import java.util.Observable;
import java.util.Optional;

public class DonatorBeanSlot extends Observable {

    private final long userId;
    private double totalDollars;
    private final LocalDate donationEnd;

    public DonatorBeanSlot(long userId, LocalDate donationEnd, double totalDollars) {
        this.userId = userId;
        this.donationEnd = donationEnd;
        this.totalDollars = totalDollars;
    }


    /* Getters */

    public long getUserId() {
        return userId;
    }

    public Optional<User> getUser() { return DiscordApiCollection.getInstance().getUserById(userId); }

    public LocalDate getDonationEnd() {
        return donationEnd;
    }

    public double getTotalDollars() { return totalDollars; }

    public boolean isValid() {
        return donationEnd.isAfter(LocalDate.now());
    }


    /* Setters */

    public void addDollars(double dollars) {
        if (dollars > 0) {
            totalDollars += dollars;
            setChanged();
            notifyObservers();
        }
    }

}
