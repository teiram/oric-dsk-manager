package com.grelobites.oric.dsk.model;

public class ArchiveOperationException extends RuntimeException {
    private String messageKey;

    public ArchiveOperationException(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
