package com.aol.cyclops.internal.monads;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import com.aol.cyclops.control.AnyM;
import com.aol.cyclops.control.For;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.Xor;
import com.aol.cyclops.internal.Monad;
import com.aol.cyclops.types.Value;
import com.aol.cyclops.types.anyM.AnyMSeq;
import com.aol.cyclops.types.anyM.AnyMValue;
import com.aol.cyclops.types.applicative.ApplicativeFunctor;

public class AnyMValueImpl<T> extends BaseAnyMImpl<T>implements AnyMValue<T> {

    @Override
    public Xor<AnyMValue<T>, AnyMSeq<T>> matchable() {
        return Xor.secondary(this);
    }

    public AnyMValueImpl(final Monad<T> monad, final Class initialType) {
        super(monad, initialType);

    }

    private <T> AnyMValueImpl<T> with(final Monad<T> anyM) {

        return new AnyMValueImpl<T>(
                                    anyM, initialType);
    }

    private <T> AnyMValueImpl<T> with(final AnyM<T> anyM) {

        return (AnyMValueImpl<T>) anyM;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.anyM.AnyMValue#ap(com.aol.cyclops.types.Value, java.util.function.BiFunction)
     */
    @Override
    public <T2, R> AnyMValue<R> combine(final Value<? extends T2> app, final BiFunction<? super T, ? super T2, ? extends R> fn) {
        if (this.unwrap() instanceof ApplicativeFunctor) {
            return AnyM.<R> ofValue(((ApplicativeFunctor) unwrap()).combine(app, fn));
        }
        return with((AnyM) AnyMValue.super.combine(app, fn));
    }

    @Override
    public <T2, R> AnyMValue<R> zip(final Iterable<? extends T2> app, final BiFunction<? super T, ? super T2, ? extends R> fn) {
        if (this.unwrap() instanceof ApplicativeFunctor) {
            return AnyM.<R> ofValue(((ApplicativeFunctor) unwrap()).zip(app, fn));
        }
        return (AnyMValue<R>) AnyMValue.super.zip(app, fn);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#zip(java.util.function.BiFunction, org.reactivestreams.Publisher)
     */
    @Override
    public <T2, R> AnyMValue<R> zip(final BiFunction<? super T, ? super T2, ? extends R> fn, final Publisher<? extends T2> app) {
        if (this.unwrap() instanceof ApplicativeFunctor) {
            return AnyM.<R> ofValue(((ApplicativeFunctor) unwrap()).zip(fn, app));
        }
        return (AnyMValue<R>) AnyMValue.super.zip(fn, app);
    }

    @Override
    public <R> AnyMValue<R> flatMapFirst(final Function<? super T, ? extends Iterable<? extends R>> fn) {
        return with(super.flatMapInternal(fn.andThen(it -> fromIterable(it))));
    }

    @Override
    public <R> AnyMValue<R> flatMapFirstPublisher(final Function<? super T, ? extends Publisher<? extends R>> fn) {
        return with(super.flatMapInternal(fn.andThen(it -> fromPublisher(it))));
    }

    @Override
    public ReactiveSeq<T> reactiveSeq() {
        return stream();
    }

    @Override
    public T get() {
        return super.get();
    }

    @Override
    public boolean isPresent() {
        if (monad.unwrap() instanceof Value) {
            return ((Value<T>) monad.unwrap()).isPresent();
        }
        return AnyMValue.super.isPresent();
    }

    @Override
    public <T> AnyMValue<T> emptyUnit() {
        return new AnyMValueImpl(
                                 monad.empty(), initialType);
    }

    @Override
    public AnyMValue<List<T>> replicateM(final int times) {

        return monad.replicateM(times)
                    .anyMValue();
    }

    @Override
    public AnyMValue<T> filter(final Predicate<? super T> p) {
        return with(super.filterInternal(p));
    }

    @Override
    public AnyMValue<T> peek(final Consumer<? super T> c) {
        return with(super.peekInternal(c));
    }

    @Override
    public AnyMValue<List<T>> aggregate(final AnyM<T> next) {
        return (AnyMValue<List<T>>) super.aggregate(next);
    }

    @Override
    public <T> AnyMValue<T> unit(final T value) {
        return AnyM.ofValue(monad.unit(value));
    }

    @Override
    public <T> AnyMValue<T> empty() {
        return with(new AnyMValueImpl(
                                      monad.empty(), initialType));
    }

    @Override
    public <NT> ReactiveSeq<NT> toReactiveSeq(final Function<? super T, ? extends Stream<? extends NT>> fn) {
        return super.toReactiveSeq(fn);
    }

    @Override
    public ReactiveSeq<T> stream() {
        return super.stream();
    }

    @Override
    public <R> AnyMValue<R> map(final Function<? super T, ? extends R> fn) {
        return with(super.mapInternal(fn));
    }

    @Override
    public <R> AnyMValue<R> bind(final Function<? super T, ?> fn) {

        return with(super.bindInternal(fn));
    }

    @Override
    public <T1> AnyMValue<T1> flatten() {
        return with(super.flattenInternal());
    }

    @Override
    public <R1, R> AnyMValue<R> forEach2(final Function<? super T, ? extends AnyMValue<R1>> monad,
            final Function<? super T, Function<? super R1, ? extends R>> yieldingFunction) {
        return AnyM.ofValue(For.anyM((AnyM<T>) this)
                               .anyM(u -> monad.apply(u))
                               .yield(yieldingFunction)
                               .unwrap());
    }

    @Override
    public <R1, R> AnyMValue<R> forEach2(final Function<? super T, ? extends AnyMValue<R1>> monad,
            final Function<? super T, Function<? super R1, Boolean>> filterFunction,
            final Function<? super T, Function<? super R1, ? extends R>> yieldingFunction) {
        return AnyM.ofValue(For.anyM((AnyM<T>) this)
                               .anyM(u -> monad.apply(u))
                               .filter(filterFunction)
                               .yield(yieldingFunction)
                               .unwrap());

    }

    @Override
    public <R1, R2, R> AnyMValue<R> forEach3(final Function<? super T, ? extends AnyMValue<R1>> monad1,
            final Function<? super T, Function<? super R1, ? extends AnyMValue<R2>>> monad2,
            final Function<? super T, Function<? super R1, Function<? super R2, Boolean>>> filterFunction,
            final Function<? super T, Function<? super R1, Function<? super R2, ? extends R>>> yieldingFunction) {

        return AnyM.ofValue(For.anyM((AnyM<T>) this)
                               .anyM(u -> monad1.apply(u))
                               .anyM(a -> b -> monad2.apply(a)
                                                     .apply(b))
                               .filter(filterFunction)
                               .yield(yieldingFunction)
                               .unwrap());
    }

    @Override
    public <R1, R2, R> AnyMValue<R> forEach3(final Function<? super T, ? extends AnyMValue<R1>> monad1,
            final Function<? super T, Function<? super R1, ? extends AnyMValue<R2>>> monad2,
            final Function<? super T, Function<? super R1, Function<? super R2, ? extends R>>> yieldingFunction) {
        return AnyM.ofValue(For.anyM((AnyM<T>) this)
                               .anyM(u -> monad1.apply(u))
                               .anyM(a -> b -> monad2.apply(a)
                                                     .apply(b))
                               .yield(yieldingFunction)
                               .unwrap());
    }

    @Override
    public <R> AnyMValue<R> flatMap(final Function<? super T, ? extends AnyMValue<? extends R>> fn) {
        return with(super.flatMapInternal(fn));
    }

    @Override
    public <T> T unwrap() {
        return super.unwrap();
    }

    @Override
    public String toString() {
        return mkString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(unwrap());
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AnyMValue))
            return false;
        final AnyMValue v2 = (AnyMValue) obj;
        return unwrap().equals(v2.unwrap());

    }

    @Override
    public <R> AnyMValue<R> applyM(final AnyMValue<Function<? super T, ? extends R>> fn) {
        return monad.applyM(((AnyMValueImpl<Function<? super T, ? extends R>>) fn).monad)
                    .anyMValue();

    }

    @Override
    public <NT> ReactiveSeq<NT> toSequence() {
        return super.toSequence();
    }
}
