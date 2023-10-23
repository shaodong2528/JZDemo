package com.jz.util;

public class ItemBean {

    public ItemBean(String mac,String name){
        this.mac = mac;
        this.name = name;
    }

    private String name;

    private String mac;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }
}
