package com.jentsch.nrf51.sensortag;

public class SensorData {

    public static final int ACCELERATION = 1;
    public static final int GYROSCOPE = 2;
    public static final int PRESSURE = 3; // atmospheric pressure

    private int type;
    private int x;
    private int y;
    private int z;
    private long time;


    public SensorData(int type, int x, int y, int z, long time) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "{" +
                "type=" + type +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
