package com.hearthgames.client.ui;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public class TextAreaOutputStream extends OutputStream {

    private final JTextArea textArea;

    public TextAreaOutputStream(final JTextArea textArea) {
        this.textArea = textArea;
    }

    public void write(int b) throws IOException {
        textArea.append(new String(new char[]{(char) b}));
        textArea.setCaretPosition(textArea.getText().length());
    }

    public void write(byte b[]) throws IOException {
        textArea.append(new String(b));
        textArea.setCaretPosition(textArea.getText().length());
    }

    public void write(byte b[], int off, int len) throws IOException {
        textArea.append(new String(b, off, len));
        textArea.setCaretPosition(textArea.getText().length());
    }

    public void flush() throws IOException {
        super.flush();
    }

    public void close() throws IOException {
        textArea.setEditable(false);
        super.close();
    }
}