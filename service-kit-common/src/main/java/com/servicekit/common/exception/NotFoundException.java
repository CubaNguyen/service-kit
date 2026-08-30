package com.servicekit.common.exception;

public class NotFoundException extends BaseBusinessException {
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public NotFoundException(String resourceName, Object id) {
        super(ErrorCode.NOT_FOUND, String.format("%s with identifier '%s' not found", resourceName, id));
    }
}