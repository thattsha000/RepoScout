package com.reposcout.server;

public class Opportunity {
    private String title;
    private String whatToDo;
    private String file;
    private int line;
    private String url;

    public Opportunity() {}

    public Opportunity(String title, String whatToDo, String file, int line, String url) {
        this.title = title;
        this.whatToDo = whatToDo;
        this.file = file;
        this.line = line;
        this.url = url;
    }

    public String getTitle() { return title; }
    public String getWhatToDo() { return whatToDo; }
    public String getFile() { return file; }
    public int getLine() { return line; }
    public String getUrl() { return url; }

    public void setTitle(String title) { this.title = title; }
    public void setWhatToDo(String whatToDo) { this.whatToDo = whatToDo; }
    public void setFile(String file) { this.file = file; }
    public void setLine(int line) { this.line = line; }
    public void setUrl(String url) { this.url = url; }
}
