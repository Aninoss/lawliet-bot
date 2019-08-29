package GUIPackage;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public class CustomOutputStream extends OutputStream {
    private JTextArea textArea;
    private int maxLength;

    public CustomOutputStream(JTextArea textArea, int maxLength) {
        this.textArea = textArea;
        this.maxLength = maxLength;
    }

    @Override
    public void write(int b) throws IOException {
        String text = textArea.getText();
        if (text.length() >= maxLength) text = text.substring(1);
        textArea.setText(text + (char) b);

        //textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}
