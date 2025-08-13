package com.onsku29.fileshare.storing;

import java.util.Objects;

public class PairedDevice {
    String ip;
    int port;
    String name;
    String token;

    public PairedDevice(String ip, int port, String name, String token){
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.token = token;
    }

    public void setIp(String ip){
        this.ip = ip;
    }

    public void setPort(int port){
        this.port = port;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setToken(String token){
        this.token = token;
    }

    public String getIp(){
        return this.ip;
    }

    public int getPort(){
        return this.port;
    }

    public String getName(){
        return this.name;
    }

    public String getToken(){
        return this.token;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PairedDevice)) return false;
        PairedDevice other = (PairedDevice) obj;

        // Consider two devices the same if IP, port, and token match
        return port == other.port &&
                Objects.equals(ip, other.ip) &&
                Objects.equals(token, other.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, token);
    }
}