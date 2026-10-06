package com.reposcout.server;

import java.util.List;

public class RawSignal {
    private int line;
    private String file;
    private String text;
    private List<String> context;
    public RawSignal(){}

    public RawSignal (int line, String file, String text, List<String> context) {
        this.line = line;
        this.file = file;
        this.text = text;
        this.context = context;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }
}

