package com.example.bluetooth;

public class Message {
    private String content;
    private boolean isFromMe; // true = јас (сино), false = тие (сиво)

    public Message(String content, boolean isFromMe) {
        this.content = content;
        this.isFromMe = isFromMe;
    }

    public String getContent() {
        return content;
    }

    public boolean isFromMe() {
        return isFromMe;
    }
}