package com.example.gatherly.service;

/** Signals that a user action violates a Gatherly business rule. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
