package core.emoji;

/**
 *
 * @author udhansingh
 *
 */
public class UnicodePointEntry {
    private final int row;
    private final String name;
    private final String[] codes;

    public UnicodePointEntry(int row, String name, String codes) {
        this.row = row;
        this.name = toName(name);
        this.codes = toCodes(codes);
    }

    public int getRow() {
        return row;
    }

    public String[] getCodes() {
        return codes;
    }

    public String getCodesAsCSV() {
        if(codes == null || codes.length == 0) return null;

        if(codes.length == 1) {
            return codes[0];
        } else {
            final StringBuilder stringBuilder = new StringBuilder();
            for(int index = 0; index < codes.length - 1; index++) {
                stringBuilder.append(codes[index]).append(", ");
            }
            stringBuilder.append(codes[codes.length - 1]);

            return stringBuilder.toString();
        }
    }

    public String getName() {
        return name;
    }

    public String toEmoji() {
        final StringBuilder stringBuilder = new StringBuilder();
        for(String code : codes) {
            final int intCode = Integer.decode(code.trim());
            for(Character character : Character.toChars(intCode)) {
                stringBuilder.append(character);
            }
        }
        return stringBuilder.toString();
    }

    private String[] toCodes(String text) {
        return text.replace("U+", "0x").split(" ");
    }

    private String toName(String text) {
        // Take out characters that invalidate java naming convention
        return text.replace(" ", "_")
                .replace(":", "")
                .replace("-", "_")
                .replace(".", "")
                .replace("&", "_and_")
                .replace("“", "")
                .replace("”", "")
                .replace("’", "_")
                .replace(",_", "_or_")
                .replace("(", "")
                .replace(")", "")
                .replace("#", "hash")
                .replace("*", "asterik")
                .replace("!", "bang")
                .replace("1st", "first")
                .replace("2nd", "second")
                .replace("3rd", "third")
                .replace("⊛_", "")
                .replace("package", "package_box")
                // do this at end
                .replaceAll("__", "_")
                .toLowerCase()
                ;
    }
}
