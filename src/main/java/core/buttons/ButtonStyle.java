package core.buttons;

public enum ButtonStyle {

    PRIMARY(1, false),
    SECONDARY(2, false),
    SUCCESS(3, false),
    DANGER(4, false),
    LINK(5, true);

    private final int value;
    private final boolean useLink;

    ButtonStyle(int value, boolean useLink) {
        this.value = value;
        this.useLink = useLink;
    }

    public int getValue() {
        return value;
    }

    public boolean usesLink() {
        return useLink;
    }
}
