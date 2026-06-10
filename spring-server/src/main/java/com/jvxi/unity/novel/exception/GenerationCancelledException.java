package com.jvxi.unity.novel.exception;

public class GenerationCancelledException extends RuntimeException {
    public GenerationCancelledException() {
        super("Generation cancelled.");
    }
}

