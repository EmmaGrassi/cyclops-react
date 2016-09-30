
package com.aol.cyclops.types;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import com.aol.cyclops.control.FutureW;

import lombok.Value;

/**
 * Interface that represents a single value that can be converted into a List, Stream or Optional
 * 
 * @author johnmcclean
 *
 * @param <T> Type of this convertable
 */
public interface Convertable<T> extends Iterable<T>, Supplier<T>, Visitable<T> {

    default <R, A> R collect(Collector<? super T, A, R> collector) {
        return toStream().collect(collector);
    }

    default <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return toStream().collect(supplier, accumulator, combiner);
    }

    /* Present is executed and it's return value returned if the value is both present, otherwise absent is called and its return value returned
     * 
     * (non-Javadoc)
     * @see com.aol.cyclops.types.Visitable#visit(java.util.function.Function, java.util.function.Supplier)
     */
    default <R> R visit(Function<? super T, ? extends R> present, Supplier<? extends R> absent) {

        if (isPresent()) {
            try {
                T value = get();
                if (value != null)
                    return present.apply(value);
                return absent.get();
            } catch (NoSuchElementException e) {
                return absent.get();
            }
        }
        return absent.get();
    }

    /**
     * @return True if value exists and is non-null
     */
    default boolean isPresent() {
        try {
            T value = get();
            if (value != null)
                return true;
            return false;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Construct a Convertable from a Supplier
     * 
     * @param supplier That returns the convertable value
     * @return Convertable
     */
    public static <T> Convertable<T> fromSupplier(Supplier<T> supplier) {
        return new SupplierToConvertable<>(
                                           supplier);
    }

    @Value
    public static class SupplierToConvertable<T> implements Convertable<T> {
        private final Supplier<T> delegate;

        public T get() {
            return delegate.get();
        }
    }

    /**
     * @return Contained value, maybe null
     */
    public T get();

    default T orElseGet(Supplier<? extends T> value) {
        return toOptional().orElseGet(value);

    }

    /**
     * @return Optional that wraps contained value, Optional.empty if value is null
     */
    default Optional<T> toOptional() {

        return visit(p -> Optional.ofNullable(p), () -> Optional.empty());
    }

    /**
     * @return Stream containing value returned by get(), Empty Stream if null
     */
    default Stream<T> toStream() {
        return Stream.of(toOptional())
                     .filter(Optional::isPresent)
                     .map(Optional::get);
    }

    /**
     * @return An AtomicReference containing value returned by get()
     */
    default AtomicReference<T> toAtomicReference() {
        return new AtomicReference<T>(
                                      get());
    }

    /**
     * @return An Optional AtomicReference containing value returned by get(), Optional.empty if get() returns null
     */
    default Optional<AtomicReference<T>> toOptionalAtomicReference() {
        return toOptional().map(u -> new AtomicReference<T>(
                                                            u));
    }

    /**Get the contained value or else the provided alternative
     * 
     * @param value
     * @return the value of this convertable (if not empty) or else the specified value
     */
    default T orElse(T value) {
        return toOptional().orElse(value);
    }

    /**
     * Get the contained value or throw an exception if null
     * 
     * @param ex Supplier that returns an exception if this value is empty
     * @return Value of this value if present
     * @throws X Exception type returned by provided Supplier
     */
    default <X extends Throwable> T orElseThrow(Supplier<? extends X> ex) throws X {
        return toOptional().orElseThrow(ex);
    }

    /**
     * @return A List containing value returned by get(), if get() returns null an Empty List is returned
     */
    default List<T> toList() {
        Optional<T> opt = toOptional();
        if (opt.isPresent())
            return Arrays.asList(get());
        return Arrays.asList();
    }

    /* An Iterator over the list returned from toList()
     * 
     *  (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    default Iterator<T> iterator() {
        return toList().iterator();
    }

    default FutureW<T> toFutureW() {
        return FutureW.of(toCompletableFuture());
    }

    default FutureW<T> toFutureWAsync() {
        return FutureW.of(toCompletableFutureAsync());
    }

    default FutureW<T> toFutureWAsync(Executor ex) {
        return FutureW.of(toCompletableFutureAsync(ex));
    }

    /**
     * @return A CompletableFuture, populated immediately by a call to get
     */
    default CompletableFuture<T> toCompletableFuture() {
        try {
            return CompletableFuture.completedFuture(get());
        } catch (Throwable t) {
            CompletableFuture<T> res = new CompletableFuture<>();
            res.completeExceptionally(t);
            return res;
        }
    }

    /**
     * @return A CompletableFuture populated asynchronously on the Common ForkJoinPool by calling get
     */
    default CompletableFuture<T> toCompletableFutureAsync() {
        return CompletableFuture.supplyAsync(this);
    }

    /**
     * @param exec Executor to asyncrhonously populate the CompletableFuture
     * @return  A CompletableFuture populated asynchronously on the supplied Executor by calling get
     */
    default CompletableFuture<T> toCompletableFutureAsync(Executor exec) {
        return CompletableFuture.supplyAsync(this, exec);
    }
}
