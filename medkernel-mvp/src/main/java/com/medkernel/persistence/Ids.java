package com.medkernel.persistence;

import java.util.concurrent.atomic.AtomicLong;

public final class Ids {
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 1000000000000L);

    private Ids() {
    }

    public static long next() {
        return SEQUENCE.incrementAndGet();
    }
}
