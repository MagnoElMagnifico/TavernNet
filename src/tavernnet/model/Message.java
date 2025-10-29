package tavernnet.model;

import java.util.Date;

public class Message {
    private int sender;
    private int receiver;
    private int group;
    private Date date;
    private String text;
    private int roll;

    public Message(int sender, int receiver, String text, Date date) {
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
        this.date = date;
    }

    public Message(int sender, int receiver, int roll, Date date) {
        this.sender = sender;
        this.receiver = receiver;
        this.roll = roll;
        this.date = date;
    }

    public Message(int sender, Date date, String text, int roll, int receiver) {
        this.sender = sender;
        this.date = date;
        this.text = text;
        this.roll = roll;
        this.receiver = receiver;
    }

    public Message(int sender, int group, Date date, String text) {
        this.sender = sender;
        this.group = group;
        this.date = date;
        this.text = text;
    }

    public Message(int sender, int group, Date date, int roll) {
        this.sender = sender;
        this.group = group;
        this.date = date;
        this.roll = roll;
    }
}
