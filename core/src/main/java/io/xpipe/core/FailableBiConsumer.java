package io.xpipe.core;

@FunctionalInterface
public interface FailableBiConsumer<T, U, E extends Throwable> {

    void accept(T var1, U var2) throws E;
}
