package com.meetruly.web.exception;

public class ResponseError {
    private int status;
    private String error;
    private String message;

    public ResponseError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ResponseError() {
    }
}
