package ru.bmstu.iu9.lab5;

public class Result {
    private final static String CONNECTOR = " : ";

    private final String url;
    private final Long time;

    public Result(String url, Long time) {
        this.url = url;
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public Long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "Result{" +
                "url='" + url + '\'' +
                '}';
    }
}
