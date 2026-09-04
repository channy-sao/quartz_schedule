package com.example.schedule.exception;

public class JobPermanentFailureException extends RuntimeException {
    public JobPermanentFailureException(String message, Throwable cause) { super(message, cause); }
}