package com.meetruly.core.exception;

public class MeetrulyException extends RuntimeException {

    public MeetrulyException(String message) {
        super(message);
    }

    public MeetrulyException(String message, Throwable cause) {
        super(message, cause);
    }
}
