package MySQL.Donators;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Observable;

public class DonatorBean extends Observable {

    private long userId;
    private LocalDate donationEnd;

    public DonatorBean(long userId, LocalDate donationEnd) {
        this.userId = userId;
        this.donationEnd = donationEnd;
    }


    /* Getters */

    public long getUserId() {
        return userId;
    }

    public LocalDate getDonationEnd() {
        return donationEnd;
    }

    public boolean isValid() {
        return donationEnd.isAfter(LocalDate.now());
    }


    /* Setters */

    public void setDonationEnd(LocalDate donationEnd) {
        this.donationEnd = donationEnd;
        setChanged();
        notifyObservers();
    }

    public void addWeeks(int weeks) { setDonationEnd(donationEnd.plus(weeks, ChronoUnit.WEEKS)); }

}
