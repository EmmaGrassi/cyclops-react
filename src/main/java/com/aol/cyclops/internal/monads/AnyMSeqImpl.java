package com.aol.cyclops.internal.monads;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import com.aol.cyclops.control.AnyM;
import com.aol.cyclops.control.For;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.Xor;
import com.aol.cyclops.internal.Monad;
import com.aol.cyclops.types.anyM.AnyMSeq;
import com.aol.cyclops.types.anyM.AnyMValue;

public class AnyMSeqImpl<T> extends BaseAnyMImpl<T>implements AnyMSeq<T> {

    protected AnyMSeqImpl(Monad<T> monad, Class initialType) {
        super(monad, initialType);

    }

    public static <T> AnyMSeqImpl<T> from(AnyMValue<T> value) {
        AnyMValueImpl<T> impl = (AnyMValueImpl<T>) value;
        return new AnyMSeqImpl<T>(
                                  impl.monad, impl.initialType);
    }

    private <T> AnyMSeqImpl<T> with(Monad<T> anyM) {

        return new AnyMSeqImpl<>(
                                 anyM, initialType);
    }

    private <T> AnyMSeqImpl<T> with(AnyM<T> anyM) {

        return (AnyMSeqImpl<T>) anyM;
    }

    @Override
    public AnyMSeq<T> peek(Consumer<? super T> c) {
        return with(super.peekInternal(c));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.IterableFunctor#unitIterator(java.util.Iterator)
     */
    @Override
    public <U> AnyMSeq<U> unitIterator(Iterator<U> it) {
        return AnyM.fromIterable(() -> it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#emptyUnit()
     */
    @Override
    public <T> AnyMSeq<T> emptyUnit() {
        return new AnyMSeqImpl(
                               monad.empty(), initialType);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#stream()
     */
    @Override
    public ReactiveSeq<T> stream() {
        return super.asSequence();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#unwrap()
     */
    @Override
    public <R> R unwrap() {
        return (R) super.unwrap();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#filter(java.util.function.Predicate)
     */
    @Override
    public AnyMSeq<T> filter(Predicate<? super T> p) {
        return with(super.filterInternal(p));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#map(java.util.function.Function)
     */
    @Override
    public <R> AnyMSeq<R> map(Function<? super T, ? extends R> fn) {
        return with(super.mapInternal(fn));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#bind(java.util.function.Function)
     */
    @Override
    public <R> AnyMSeq<R> bind(Function<? super T, ?> fn) {
        return with(super.bindInternal(fn));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#flatten()
     */
    @Override
    public <T1> AnyMSeq<T1> flatten() {
        return with(super.flattenInternal());
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#aggregate(com.aol.cyclops.control.AnyM)
     */
    @Override
    public AnyMSeq<List<T>> aggregate(AnyM<T> next) {
        return with(super.aggregate(next));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#forEach2(java.util.function.Function, java.util.function.Function)
     */
    @Override
    public <R1, R> AnyMSeq<R> forEach2(Function<? super T, ? extends AnyM<R1>> monad,
            Function<? super T, Function<? super R1, ? extends R>> yieldingFunction) {
        return For.anyM((AnyM<T>) this)
                  .anyM(u -> monad.apply(u))
                  .yield(yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#forEach2(java.util.function.Function, java.util.function.Function, java.util.function.Function)
     */
    @Override
    public <R1, R> AnyMSeq<R> forEach2(Function<? super T, ? extends AnyM<R1>> monad,
            Function<? super T, Function<? super R1, Boolean>> filterFunction,
            Function<? super T, Function<? super R1, ? extends R>> yieldingFunction) {
        return For.anyM((AnyM<T>) this)
                  .anyM(u -> monad.apply(u))
                  .filter(filterFunction)
                  .yield(yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#forEach3(java.util.function.Function, java.util.function.Function, java.util.function.Function, java.util.function.Function)
     */
    @Override
    public <R1, R2, R> AnyMSeq<R> forEach3(Function<? super T, ? extends AnyM<R1>> monad1,
            Function<? super T, Function<? super R1, ? extends AnyM<R2>>> monad2,
            Function<? super T, Function<? super R1, Function<? super R2, Boolean>>> filterFunction,
            Function<? super T, Function<? super R1, Function<? super R2, ? extends R>>> yieldingFunction) {
        return For.anyM((AnyM<T>) this)
                  .anyM(u -> monad1.apply(u))
                  .anyM(a -> b -> monad2.apply(a)
                                        .apply(b))
                  .filter(filterFunction)
                  .yield(yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#forEach3(java.util.function.Function, java.util.function.Function, java.util.function.Function)
     */
    @Override
    public <R1, R2, R> AnyMSeq<R> forEach3(Function<? super T, ? extends AnyM<R1>> monad1,
            Function<? super T, Function<? super R1, ? extends AnyM<R2>>> monad2,
            Function<? super T, Function<? super R1, Function<? super R2, ? extends R>>> yieldingFunction) {
        return For.anyM((AnyM<T>) this)
                  .anyM(u -> monad1.apply(u))
                  .anyM(a -> b -> monad2.apply(a)
                                        .apply(b))
                  .yield(yieldingFunction);

    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#flatMap(java.util.function.Function)
     */
    @Override
    public <R> AnyMSeq<R> flatMap(Function<? super T, ? extends AnyM<? extends R>> fn) {
        return with(super.flatMapInternal(fn));

    }

    @Override
    public <R> AnyMSeq<R> flatMapFirst(Function<? super T, ? extends Iterable<? extends R>> fn) {
        return with(super.flatMapInternal(fn.andThen(it -> fromIterable(it))));

    }

    @Override
    public <R> AnyMSeq<R> flatMapFirstPublisher(Function<? super T, ? extends Publisher<? extends R>> fn) {
        return with(super.flatMapInternal(fn.andThen(it -> fromPublisher(it))));

    }

    public Xor<AnyMValue<T>, AnyMSeq<T>> matchable() {
        return Xor.primary(this);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#unit(java.lang.Object)
     */
    @Override
    public <T> AnyMSeq<T> unit(T value) {
        return AnyM.ofSeq(monad.unit(value));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#empty()
     */
    @Override
    public <T> AnyMSeq<T> empty() {
        return with(new AnyMSeqImpl(
                                    monad.empty(), initialType));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.super.AnyMSeq#replicateM(int)
     */
    @Override
    public AnyMSeq<T> replicateM(int times) {
        return monad.replicateM(times)
                    .anyMSeq();
    }

    public <R> AnyMSeq<R> applyM(AnyM<Function<? super T, ? extends R>> fn) {
        return monad.applyM(((AnyMSeqImpl<Function<? super T, ? extends R>>) fn).monad())
                    .anyMSeq();

    }

    @Override
    public <NT> ReactiveSeq<NT> toReactiveSeq(Function<? super T, ? extends Stream<? extends NT>> fn) {
        return super.toReactiveSeq(fn);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.reactive.ReactiveStreamsTerminalOperations#forEachX(long, java.util.function.Consumer)
     */
    @Override
    public <X extends Throwable> Subscription forEachX(long numberOfElements, Consumer<? super T> consumer) {
        return this.stream()
                   .forEachX(numberOfElements, consumer);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.reactive.ReactiveStreamsTerminalOperations#forEachXWithError(long, java.util.function.Consumer, java.util.function.Consumer)
     */
    @Override
    public <X extends Throwable> Subscription forEachXWithError(long numberOfElements, Consumer<? super T> consumer,
            Consumer<? super Throwable> consumerError) {
        return this.stream()
                   .forEachXWithError(numberOfElements, consumer, consumerError);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.reactive.ReactiveStreamsTerminalOperations#forEachXEvents(long, java.util.function.Consumer, java.util.function.Consumer, java.lang.Runnable)
     */
    @Override
    public <X extends Throwable> Subscription forEachXEvents(long numberOfElements, Consumer<? super T> consumer,
            Consumer<? super Throwable> consumerError, Runnable onComplete) {
        return this.stream()
                   .forEachXEvents(numberOfElements, consumer, consumerError, onComplete);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.reactive.ReactiveStreamsTerminalOperations#forEachWithError(java.util.function.Consumer, java.util.function.Consumer)
     */
    @Override
    public <X extends Throwable> void forEachWithError(Consumer<? super T> consumerElement, Consumer<? super Throwable> consumerError) {
        this.stream()
            .forEachWithError(consumerElement, consumerError);

    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.reactive.ReactiveStreamsTerminalOperations#forEachEvent(java.util.function.Consumer, java.util.function.Consumer, java.lang.Runnable)
     */
    @Override
    public <X extends Throwable> void forEachEvent(Consumer<? super T> consumerElement, Consumer<? super Throwable> consumerError,
            Runnable onComplete) {
        this.stream()
            .forEachEvent(consumerElement, consumerError, onComplete);

    }

    @Override
    public String toString() {
        return String.format("AnyMSeq[%s]", monad);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(unwrap());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AnyMSeq))
            return false;
        return unwrap().equals(((AnyMSeq) o).unwrap());
    }

}
