package ru.bmstu.iu9.lab5;

public class Answer {
    private final String url;
    private final Long time;

    public Answer(String url, Long time) {
        this.url = url;
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public Long getTime() {
        return time;
    }
}
