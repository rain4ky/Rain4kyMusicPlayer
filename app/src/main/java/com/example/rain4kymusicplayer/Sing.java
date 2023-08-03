package com.example.rain4kymusicplayer;

public class Sing {
    private String sing_name="暂无";
    String singer_name="佚名";
    String sing_length_txt="00:00";
    String path="/";

    public void setSing_name(String sing_name){this.sing_name=sing_name;}
    public String getSing_name(){return sing_name;}
    public void setSinger_name(String singer_name){this.singer_name=singer_name;}
    public String getSinger_name(){return singer_name;}
    public void setSing_length_txt(String sing_length_txt){this.sing_length_txt=sing_length_txt;}
    public String getSing_length_txt(){return sing_length_txt;}
    public void setPath(String path){this.path=path;}
    public String getPath(){return path;}
}
