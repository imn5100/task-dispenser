package com.shaw;

/**
 * Created by Administrator on 2016/12/30 0030.
 */
public class TestClass {
    private String url;
    private String path;
    private String name;


    @Override
    public String toString() {
        return "TestClass{" +
                "url='" + url + '\'' +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
