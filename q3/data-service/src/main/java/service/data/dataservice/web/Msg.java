package service.data.dataservice.web;

import java.io.Serializable;

public class Msg implements Serializable {
    private int index;
    private int size;
    private String uuid;

    public Msg() {
    }

    public Msg(int index, String uuid, int size) {
        this.index = index;
        this.uuid = uuid;
        this.size = size;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "Msg{" +
                "index=" + index +
                ", uuid=" + uuid +
                ", size=" + size +
                '}';
    }
}