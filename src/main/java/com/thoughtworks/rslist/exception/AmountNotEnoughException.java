package com.thoughtworks.rslist.exception;

import org.springframework.http.HttpStatus;

public class AmountNotEnoughException extends RuntimeException {

    public final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

    public AmountNotEnoughException() {
        super("amount not enough");
    }
}
