package com.example.project.Activitis;

public class SecondDomain {
    private String day;
    private String picPath;
    private String status;
    private int highTemp;
    private int lowTemp;

    public SecondDomain(String day, int lowTemp, String picPath, String status, int highTemp) {
        this.day = day;
        this.lowTemp = lowTemp;
        this.picPath = picPath;
        this.status = status;
        this.highTemp = highTemp;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getHighTemp() {
        return highTemp;
    }

    public void setHighTemp(int highTemp) {
        this.highTemp = highTemp;
    }

    public int getLowTemp() {
        return lowTemp;
    }

    public void setLowTemp(int lowTemp) {
        this.lowTemp = lowTemp;
    }
}