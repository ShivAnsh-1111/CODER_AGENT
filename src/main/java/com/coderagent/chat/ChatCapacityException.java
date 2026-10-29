package com.coderagent.chat;

public class ChatCapacityException extends RuntimeException {

    public ChatCapacityException() {
        super("The maximum number of active chat sessions has been reached.");
    }
}
