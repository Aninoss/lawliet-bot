package core.mention;

public class MentionValue<E> {

    private final E value;
    private final String filteredArgs;

    public MentionValue(String filteredArgs, E value) {
        this.filteredArgs = filteredArgs;
        this.value = value;
    }

    public E getValue() {
        return value;
    }

    public String getFilteredArgs() {
        return filteredArgs.trim();
    }

}
