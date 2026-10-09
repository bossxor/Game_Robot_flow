package com.bossxor.robotflow;

/** Seven deliberate taps in a short session; no visible unlock progress. */
final class SecretTapSequence {
    private int count;
    private long first, previous;
    boolean tap(long now) {
        if (count == 0 || now - previous > 1200 || now - first > 5000) { count = 0; first = now; }
        previous = now;
        if (++count == 7) { count = 0; return true; }
        return false;
    }
}
