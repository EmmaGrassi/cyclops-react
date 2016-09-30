package com.aol.cyclops.control;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.reactivestreams.Publisher;

import com.aol.cyclops.Matchables;
import com.aol.cyclops.Monoid;
import com.aol.cyclops.Reducer;
import com.aol.cyclops.Semigroup;
import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.Matchable.CheckValue2;
import com.aol.cyclops.data.collections.extensions.CollectionX;
import com.aol.cyclops.data.collections.extensions.standard.ListX;
import com.aol.cyclops.types.BiFunctor;
import com.aol.cyclops.types.Filterable;
import com.aol.cyclops.types.Functor;
import com.aol.cyclops.types.MonadicValue;
import com.aol.cyclops.types.MonadicValue2;
import com.aol.cyclops.types.Value;
import com.aol.cyclops.types.anyM.AnyMValue;
import com.aol.cyclops.types.applicative.ApplicativeFunctor;
import com.aol.cyclops.types.stream.reactive.ValueSubscriber;
import com.aol.cyclops.util.function.Curry;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * Inclusive Or (can be one of Primary, Secondary or Both Primary and Secondary)
 * 
 * An Either or Union type, but right biased. Primary and Secondary are used instead of Right & Left.
 * 'Right' (or primary type) biased disjunct union.
 *  No 'projections' are provided, swap() and secondaryXXXX alternative methods can be used instead.
 *  
 * 
 * @author johnmcclean
 *
 * @param <ST> Secondary type
 * @param <PT> Primary type
 */
public interface Ior<ST, PT> extends Supplier<PT>, MonadicValue2<ST, PT>, BiFunctor<ST, PT>, Functor<PT>, Filterable<PT>, ApplicativeFunctor<PT> {

    public static <T> Ior<Throwable, T> fromPublisher(final Publisher<T> pub) {
        final ValueSubscriber<T> sub = ValueSubscriber.subscriber();
        pub.subscribe(sub);
        return sub.toXor()
                  .toIor();
    }

    public static <ST, T> Ior<ST, T> fromIterable(final Iterable<T> iterable) {
        final Iterator<T> it = iterable.iterator();
        return Ior.primary(it.hasNext() ? it.next() : null);
    }

    public static <ST, PT> Ior<ST, PT> primary(final PT primary) {
        return new Primary<>(
                             primary);
    }

    public static <ST, PT> Ior<ST, PT> secondary(final ST secondary) {
        return new Secondary<>(
                               secondary);
    }

    public static <ST, PT> Ior<ST, PT> both(final Ior<ST, PT> secondary, final Ior<ST, PT> primary) {
        return new Both<ST, PT>(
                                secondary, primary);
    }

    public static <ST, PT> Ior<ST, PT> both(final ST secondary, final PT primary) {
        return new Both<ST, PT>(
                                Ior.secondary(secondary), Ior.primary(primary));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#anyM()
     */
    @Override
    default AnyMValue<PT> anyM() {
        return AnyM.ofValue(this);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue2#unit(java.lang.Object)
     */
    @Override
    default <T> Ior<ST, T> unit(final T unit) {
        return Ior.primary(unit);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Filterable#filter(java.util.function.Predicate)
     */
    @Override
    Ior<ST, PT> filter(Predicate<? super PT> test);

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Value#toXor()
     */
    @Override
    Xor<ST, PT> toXor(); //drop ST

    Xor<ST, PT> toXorDropPrimary(); //drop ST

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Value#toXor(java.lang.Object)
     */
    @Override
    default <ST2> Xor<ST2, PT> toXor(final ST2 secondary) {
        return visit(s -> Xor.secondary(secondary), p -> Xor.primary(p), (s, p) -> Xor.primary(p));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Value#toIor()
     */
    @Override
    default Ior<ST, PT> toIor() {
        return this;
    }

    Ior<ST, PT> secondaryToPrimayMap(Function<? super ST, ? extends PT> fn);

    <R> Ior<R, PT> secondaryMap(Function<? super ST, ? extends R> fn);

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue2#map(java.util.function.Function)
     */
    @Override
    <R> Ior<ST, R> map(Function<? super PT, ? extends R> fn);

    Ior<ST, PT> secondaryPeek(Consumer<? super ST> action);

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Functor#peek(java.util.function.Consumer)
     */
    @Override
    Ior<ST, PT> peek(Consumer<? super PT> action);

    Ior<PT, ST> swap();

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#coflatMap(java.util.function.Function)
     */
    @Override
    default <R> Ior<ST, R> coflatMap(final Function<? super MonadicValue<PT>, R> mapper) {
        return (Ior<ST, R>) MonadicValue2.super.coflatMap(mapper);
    }

    //cojoin
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#nest()
     */
    @Override
    default Ior<ST, MonadicValue<PT>> nest() {
        return this.map(t -> unit(t));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue2#combine(com.aol.cyclops.Monoid, com.aol.cyclops.types.MonadicValue2)
     */
    @Override
    default Ior<ST, PT> combineEager(final Monoid<PT> monoid, final MonadicValue2<? extends ST, ? extends PT> v2) {
        return (Ior<ST, PT>) MonadicValue2.super.combineEager(monoid, v2);
    }

    Optional<Tuple2<ST, PT>> both();

    default Value<Optional<Tuple2<ST, PT>>> bothValue() {
        return () -> both();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Functor#patternMatch(java.util.function.Function, java.util.function.Supplier)
     */
    @Override
    default <R> Xor<ST, R> patternMatch(final Function<CheckValue1<PT, R>, CheckValue1<PT, R>> case1, final Supplier<? extends R> otherwise) {

        return (Xor<ST, R>) ApplicativeFunctor.super.patternMatch(case1, otherwise);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.BiFunctor#bimap(java.util.function.Function, java.util.function.Function)
     */
    @Override
    default <R1, R2> Ior<R1, R2> bimap(final Function<? super ST, ? extends R1> fn1, final Function<? super PT, ? extends R2> fn2) {
        final Eval<Ior<R1, R2>> ptMap = (Eval) Eval.later(() -> this.map(fn2)); //force unused secondary to required
        final Eval<Ior<R1, R2>> stMap = (Eval) Eval.later(() -> this.secondaryMap(fn1)); //force unused primary to required
        if (isPrimary())
            return Ior.<R1, R2> primary(ptMap.get()
                                             .get());
        if (isSecondary())
            return Ior.<R1, R2> secondary(stMap.get()
                                               .swap()
                                               .get());

        return Ior.both(stMap.get(), ptMap.get());
    }

    /**
     * @param secondary
     * @param primary
     * @param both
     * @return
     */
    default <R> R visit(final Function<? super ST, ? extends R> secondary, final Function<? super PT, ? extends R> primary,
            final BiFunction<? super ST, ? super PT, ? extends R> both) {

        if (isSecondary())
            return swap().visit(secondary, () -> null);
        if (isPrimary())
            return visit(primary, () -> null);

        return Matchables.tuple2(both().get())
                         .visit((a, b) -> both.apply(a, b));
    }

    
   

    <R> Eval<R> matches(Function<CheckValue1<ST, R>, CheckValue1<ST, R>> secondary, Function<CheckValue1<PT, R>, CheckValue1<PT, R>> primary,
            Function<CheckValue2<ST, PT, R>, CheckValue2<ST, PT, R>> both, Supplier<? extends R> otherwise);

    /* (non-Javadoc)
     * @see java.util.function.Supplier#get()
     */
    @Override
    PT get();

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Convertable#isPresent()
     */
    @Override
    default boolean isPresent() {
        return isPrimary() || isBoth();
    }

    Value<ST> secondaryValue();

    ST secondaryGet();

    Optional<ST> secondaryToOptional();

    ReactiveSeq<ST> secondaryToStream();

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue2#flatMap(java.util.function.Function)
     */
    @Override
    <LT1, RT1> Ior<LT1, RT1> flatMap(Function<? super PT, ? extends MonadicValue2<? extends LT1, ? extends RT1>> mapper);

    <LT1, RT1> Ior<LT1, RT1> secondaryFlatMap(Function<? super ST, ? extends Ior<LT1, RT1>> mapper);

    Ior<ST, PT> secondaryToPrimayFlatMap(Function<? super ST, ? extends Ior<ST, PT>> fn);

    public boolean isPrimary();

    public boolean isSecondary();

    public boolean isBoth();

    public static <ST, PT> Ior<ListX<PT>, ListX<ST>> sequenceSecondary(final CollectionX<Ior<ST, PT>> iors) {
        return AnyM.sequence(AnyM.listFromIor(iors.map(Ior::swap)))
                   .unwrap();
    }

    public static <ST, PT, R> Ior<?, R> accumulateSecondary(final CollectionX<Ior<ST, PT>> iors, final Reducer<R> reducer) {
        return sequenceSecondary(iors).map(s -> s.mapReduce(reducer));
    }

    public static <ST, PT, R> Ior<?, R> accumulateSecondary(final CollectionX<Ior<ST, PT>> iors, final Function<? super ST, R> mapper,
            final Semigroup<R> reducer) {
        return sequenceSecondary(iors).map(s -> s.map(mapper)
                                                 .reduce(reducer.reducer())
                                                 .get());
    }

    public static <ST, PT> Ior<?, ST> accumulateSecondary(final CollectionX<Ior<ST, PT>> iors, final Semigroup<ST> reducer) {
        return sequenceSecondary(iors).map(s -> s.reduce(reducer.reducer())
                                                 .get());
    }

    public static <ST, PT> Ior<ListX<ST>, ListX<PT>> sequencePrimary(final CollectionX<Ior<ST, PT>> iors) {
        return AnyM.sequence(AnyM.<ST, PT> listFromIor(iors))
                   .unwrap();
    }

    public static <ST, PT, R> Ior<?, R> accumulatePrimary(final CollectionX<Ior<ST, PT>> iors, final Reducer<R> reducer) {
        return sequencePrimary(iors).map(s -> s.mapReduce(reducer));
    }

    public static <ST, PT, R> Ior<?, R> accumulatePrimary(final CollectionX<Ior<ST, PT>> iors, final Function<? super PT, R> mapper,
            final Semigroup<R> reducer) {
        return sequencePrimary(iors).map(s -> s.map(mapper)
                                               .reduce(reducer.reducer())
                                               .get());
    }

    public static <ST, PT> Ior<?, PT> accumulatePrimary(final CollectionX<Ior<ST, PT>> iors, final Semigroup<PT> reducer) {
        return sequencePrimary(iors).map(s -> s.reduce(reducer.reducer())
                                               .get());
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#ofType(java.lang.Class)
     */
    @Override
    default <U> Ior<ST, U> ofType(final Class<? extends U> type) {

        return (Ior<ST, U>) Filterable.super.ofType(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#filterNot(java.util.function.Predicate)
     */
    @Override
    default Ior<ST, PT> filterNot(final Predicate<? super PT> fn) {

        return (Ior<ST, PT>) Filterable.super.filterNot(fn);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#notNull()
     */
    @Override
    default Ior<ST, PT> notNull() {

        return (Ior<ST, PT>) Filterable.super.notNull();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Functor#cast(java.lang.Class)
     */
    @Override
    default <U> Ior<ST, U> cast(final Class<? extends U> type) {

        return (Ior<ST, U>) ApplicativeFunctor.super.cast(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Functor#trampoline(java.util.function.Function)
     */
    @Override
    default <R> Ior<ST, R> trampoline(final Function<? super PT, ? extends Trampoline<? extends R>> mapper) {

        return (Ior<ST, R>) ApplicativeFunctor.super.trampoline(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.BiFunctor#bipeek(java.util.function.Consumer, java.util.function.Consumer)
     */
    @Override
    default Ior<ST, PT> bipeek(final Consumer<? super ST> c1, final Consumer<? super PT> c2) {

        return (Ior<ST, PT>) BiFunctor.super.bipeek(c1, c2);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.BiFunctor#bicast(java.lang.Class, java.lang.Class)
     */
    @Override
    default <U1, U2> Ior<U1, U2> bicast(final Class<U1> type1, final Class<U2> type2) {

        return (Ior<U1, U2>) BiFunctor.super.bicast(type1, type2);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.BiFunctor#bitrampoline(java.util.function.Function, java.util.function.Function)
     */
    @Override
    default <R1, R2> Ior<R1, R2> bitrampoline(final Function<? super ST, ? extends Trampoline<? extends R1>> mapper1,
            final Function<? super PT, ? extends Trampoline<? extends R2>> mapper2) {

        return (Ior<R1, R2>) BiFunctor.super.bitrampoline(mapper1, mapper2);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#zip(java.lang.Iterable, java.util.function.BiFunction)
     */
    @Override
    default <T2, R> Ior<ST, R> zip(final Iterable<? extends T2> app, final BiFunction<? super PT, ? super T2, ? extends R> fn) {
        return map(v -> Tuple.tuple(v, Curry.curry2(fn)
                                            .apply(v))).flatMap(tuple -> Ior.fromIterable(app)
                                                                            .visit(i -> Ior.primary(tuple.v2.apply(i)), () -> Ior.secondary(null)));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#zip(java.util.function.BiFunction, org.reactivestreams.Publisher)
     */
    @Override
    default <T2, R> Ior<ST, R> zip(final BiFunction<? super PT, ? super T2, ? extends R> fn, final Publisher<? extends T2> app) {
        return map(v -> Tuple.tuple(v, Curry.curry2(fn)
                                            .apply(v))).flatMap(tuple -> Xor.fromPublisher(app)
                                                                            .visit(i -> Xor.primary(tuple.v2.apply(i)), () -> Xor.secondary(null)));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Zippable#zip(org.jooq.lambda.Seq, java.util.function.BiFunction)
     */
    @Override
    default <U, R> Ior<ST, R> zip(final Seq<? extends U> other, final BiFunction<? super PT, ? super U, ? extends R> zipper) {

        return (Ior<ST, R>) MonadicValue2.super.zip(other, zipper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Zippable#zip(java.util.stream.Stream, java.util.function.BiFunction)
     */
    @Override
    default <U, R> Ior<ST, R> zip(final Stream<? extends U> other, final BiFunction<? super PT, ? super U, ? extends R> zipper) {

        return (Ior<ST, R>) MonadicValue2.super.zip(other, zipper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Zippable#zip(java.util.stream.Stream)
     */
    @Override
    default <U> Ior<ST, Tuple2<PT, U>> zip(final Stream<? extends U> other) {

        return (Ior) MonadicValue2.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Zippable#zip(org.jooq.lambda.Seq)
     */
    @Override
    default <U> Ior<ST, Tuple2<PT, U>> zip(final Seq<? extends U> other) {

        return (Ior) MonadicValue2.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Zippable#zip(java.lang.Iterable)
     */
    @Override
    default <U> Ior<ST, Tuple2<PT, U>> zip(final Iterable<? extends U> other) {

        return (Ior) MonadicValue2.super.zip(other);
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(of = { "value" })
    public static class Primary<ST, PT> implements Ior<ST, PT> {
        private final PT value;

        @Override
        public Xor<ST, PT> toXor() {
            return Xor.primary(value);
        }

        @Override
        public Xor<ST, PT> toXorDropPrimary() {
            return Xor.primary(value);
        }

        @Override
        public Ior<ST, PT> secondaryToPrimayMap(final Function<? super ST, ? extends PT> fn) {
            return this;
        }

        @Override
        public <R> Ior<R, PT> secondaryMap(final Function<? super ST, ? extends R> fn) {
            return (Ior<R, PT>) this;
        }

        @Override
        public <R> Ior<ST, R> map(final Function<? super PT, ? extends R> fn) {
            return new Primary<ST, R>(
                                      fn.apply(value));
        }

        @Override
        public Ior<ST, PT> secondaryPeek(final Consumer<? super ST> action) {
            return this;
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary, final Function<? super PT, ? extends R> primary,
                final BiFunction<? super ST, ? super PT, ? extends R> both) {
            return primary.apply(value);
        }

        @Override
        public Ior<ST, PT> peek(final Consumer<? super PT> action) {
            action.accept(value);
            return this;
        }

        @Override
        public Ior<ST, PT> filter(final Predicate<? super PT> test) {
            if (test.test(value))
                return this;
            return Ior.secondary(null);
        }

        @Override
        public <R1, R2> Ior<R1, R2> bimap(final Function<? super ST, ? extends R1> fn1, final Function<? super PT, ? extends R2> fn2) {
            return Ior.<R1, R2> primary(fn2.apply(value));
        }

        @Override
        public Ior<PT, ST> swap() {
            return new Secondary<PT, ST>(
                                         value);
        }

        @Override
        public PT get() {
            return value;
        }

        @Override
        public ST secondaryGet() {
            return null;
        }

        @Override
        public Optional<ST> secondaryToOptional() {
            return Optional.empty();
        }

        @Override
        public ReactiveSeq<ST> secondaryToStream() {
            return ReactiveSeq.empty();
        }

        @Override
        public <LT1, RT1> Ior<LT1, RT1> flatMap(final Function<? super PT, ? extends MonadicValue2<? extends LT1, ? extends RT1>> mapper) {
            return (Ior<LT1, RT1>) mapper.apply(value)
                                         .toIor();
        }

        @Override
        public <LT1, RT1> Ior<LT1, RT1> secondaryFlatMap(final Function<? super ST, ? extends Ior<LT1, RT1>> mapper) {
            return (Ior<LT1, RT1>) this;
        }

        @Override
        public Ior<ST, PT> secondaryToPrimayFlatMap(final Function<? super ST, ? extends Ior<ST, PT>> fn) {
            return this;
        }

        @Override
        public Ior<ST, PT> bipeek(final Consumer<? super ST> stAction, final Consumer<? super PT> ptAction) {
            ptAction.accept(value);
            return this;
        }

        @Override
        public Optional<Tuple2<ST, PT>> both() {
            return Optional.empty();
        }

        @Override
        public boolean isPrimary() {
            return true;
        }

        @Override
        public boolean isSecondary() {
            return false;
        }

        @Override
        public boolean isBoth() {
            return false;
        }

        @Override
        public Value<ST> secondaryValue() {
            return Value.of(() -> null);
        }

        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Ior.primary[" + value + "]";
        }

        /* (non-Javadoc)
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Ior<ST, R> combine(final Value<? extends T2> app, final BiFunction<? super PT, ? super T2, ? extends R> fn) {

            return app.toXor()
                      .visit(s -> Ior.secondary(null), f -> Ior.primary(fn.apply(get(), app.get())));
        }

        @Override
        public <R> Eval<R> matches(
                final Function<com.aol.cyclops.control.Matchable.CheckValue1<ST, R>, com.aol.cyclops.control.Matchable.CheckValue1<ST, R>> secondary,
                final Function<com.aol.cyclops.control.Matchable.CheckValue1<PT, R>, com.aol.cyclops.control.Matchable.CheckValue1<PT, R>> primary,
                final Function<com.aol.cyclops.control.Matchable.CheckValue2<ST, PT, R>, com.aol.cyclops.control.Matchable.CheckValue2<ST, PT, R>> both,
                final Supplier<? extends R> otherwise) {

            final Matchable.MTuple1<PT> mt1 = () -> Tuple.tuple(value);
            return mt1.matches(primary, otherwise);
        }

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(of = { "value" })
    public static class Secondary<ST, PT> implements Ior<ST, PT> {
        private final ST value;

        @Override
        public boolean isSecondary() {
            return true;
        }

        @Override
        public boolean isPrimary() {
            return false;
        }

        @Override
        public Xor<ST, PT> toXor() {
            return Xor.secondary(value);
        }

        @Override
        public Xor<ST, PT> toXorDropPrimary() {
            return Xor.secondary(value);
        }

        @Override
        public Ior<ST, PT> secondaryToPrimayMap(final Function<? super ST, ? extends PT> fn) {
            return new Primary<ST, PT>(
                                       fn.apply(value));
        }

        @Override
        public <R> Ior<R, PT> secondaryMap(final Function<? super ST, ? extends R> fn) {
            return new Secondary<R, PT>(
                                        fn.apply(value));
        }

        @Override
        public <R> Ior<ST, R> map(final Function<? super PT, ? extends R> fn) {
            return (Ior<ST, R>) this;
        }

        @Override
        public Ior<ST, PT> secondaryPeek(final Consumer<? super ST> action) {
            return secondaryMap((Function) FluentFunctions.expression(action));
        }

        @Override
        public Optional<Tuple2<ST, PT>> both() {
            return Optional.empty();
        }

        @Override
        public Ior<ST, PT> peek(final Consumer<? super PT> action) {
            return this;
        }

        @Override
        public Ior<ST, PT> filter(final Predicate<? super PT> test) {
            return this;
        }

        @Override
        public <R1, R2> Ior<R1, R2> bimap(final Function<? super ST, ? extends R1> fn1, final Function<? super PT, ? extends R2> fn2) {
            return Ior.<R1, R2> secondary(fn1.apply(value));
        }

        @Override
        public Ior<PT, ST> swap() {
            return new Primary<PT, ST>(
                                       value);
        }

        @Override
        public PT get() {
            throw new NoSuchElementException();
        }

        @Override
        public ST secondaryGet() {
            return value;
        }

        @Override
        public Optional<ST> secondaryToOptional() {
            return Optional.ofNullable(value);
        }

        @Override
        public ReactiveSeq<ST> secondaryToStream() {
            return ReactiveSeq.fromStream(StreamUtils.optionalToStream(secondaryToOptional()));
        }

        @Override
        public <LT1, RT1> Ior<LT1, RT1> flatMap(final Function<? super PT, ? extends MonadicValue2<? extends LT1, ? extends RT1>> mapper) {
            return (Ior<LT1, RT1>) this;
        }

        @Override
        public <LT1, RT1> Ior<LT1, RT1> secondaryFlatMap(final Function<? super ST, ? extends Ior<LT1, RT1>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public Ior<ST, PT> secondaryToPrimayFlatMap(final Function<? super ST, ? extends Ior<ST, PT>> fn) {
            return fn.apply(value);
        }

        @Override
        public Ior<ST, PT> bipeek(final Consumer<? super ST> stAction, final Consumer<? super PT> ptAction) {
            stAction.accept(value);
            return this;
        }

        @Override
        public Value<ST> secondaryValue() {
            return Value.of(() -> value);
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary, final Function<? super PT, ? extends R> primary,
                final BiFunction<? super ST, ? super PT, ? extends R> both) {
            return swap().visit(secondary, () -> null);
        }

        @Override
        public boolean isBoth() {
            return false;
        }

        @Override
        public Maybe<PT> toMaybe() {
            return Maybe.none();
        }

        @Override
        public Optional<PT> toOptional() {
            return Optional.empty();
        }

        /* (non-Javadoc)
         * @see com.aol.cyclops.value.Value#unapply()
         */
        @Override
        public ListX<ST> unapply() {
            return ListX.of(value);
        }

        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Ior.secondary[" + value + "]";
        }

        @Override
        public <R> Eval<R> matches(
                final Function<com.aol.cyclops.control.Matchable.CheckValue1<ST, R>, com.aol.cyclops.control.Matchable.CheckValue1<ST, R>> fn1,
                final Function<com.aol.cyclops.control.Matchable.CheckValue1<PT, R>, com.aol.cyclops.control.Matchable.CheckValue1<PT, R>> fn2,
                final Function<com.aol.cyclops.control.Matchable.CheckValue2<ST, PT, R>, com.aol.cyclops.control.Matchable.CheckValue2<ST, PT, R>> fn3,
                final Supplier<? extends R> otherwise) {
            final Matchable.MTuple1<ST> mt1 = () -> Tuple.tuple(value);
            return mt1.matches(fn1, otherwise);

        }

        /* (non-Javadoc)
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Ior<ST, R> combine(final Value<? extends T2> app, final BiFunction<? super PT, ? super T2, ? extends R> fn) {
            return (Ior<ST, R>) this;
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @EqualsAndHashCode(of = { "secondary", "primary" })
    public static class Both<ST, PT> implements Ior<ST, PT> {
        private final Ior<ST, PT> secondary;
        private final Ior<ST, PT> primary;

        @Override
        public ReactiveSeq<PT> stream() {
            return primary.stream();
        }

        @Override
        public Iterator<PT> iterator() {
            return primary.iterator();
        }

        @Override
        public Xor<ST, PT> toXor() {
            return primary.toXor();
        }

        @Override
        public Xor<ST, PT> toXorDropPrimary() {
            return secondary.toXor();
        }

        @Override
        public Ior<ST, PT> secondaryToPrimayMap(final Function<? super ST, ? extends PT> fn) {
            return this;
        }

        @Override
        public <R> Ior<R, PT> secondaryMap(final Function<? super ST, ? extends R> fn) {
            return Ior.both(secondary.secondaryMap(fn), primary.secondaryMap(fn));
        }

        @Override
        public <R> Ior<ST, R> map(final Function<? super PT, ? extends R> fn) {
            return Ior.<ST, R> both(secondary.map(fn), primary.map(fn));
        }

        @Override
        public Ior<ST, PT> secondaryPeek(final Consumer<? super ST> action) {
            secondary.secondaryPeek(action);
            return this;
        }

        @Override
        public Ior<ST, PT> peek(final Consumer<? super PT> action) {
            primary.peek(action);
            return this;
        }

        @Override
        public Ior<ST, PT> filter(final Predicate<? super PT> test) {
            return primary.filter(test);
        }

        @Override
        public Ior<PT, ST> swap() {
            return Ior.both(primary.swap(), secondary.swap());

        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary, final Function<? super PT, ? extends R> primary,
                final BiFunction<? super ST, ? super PT, ? extends R> both) {
            return Matchables.tuple2(both().get())
                             .visit((a, b) -> both.apply(a, b));
        }

        @Override
        public Optional<Tuple2<ST, PT>> both() {
            return Optional.of(Tuple.tuple(secondary.secondaryGet(), primary.get()));
        }

        @Override
        public PT get() {
            return primary.get();
        }

        @Override
        public Value<ST> secondaryValue() {
            return secondary.secondaryValue();
        }

        @Override
        public ST secondaryGet() {
            return secondary.secondaryGet();
        }

        @Override
        public Optional<ST> secondaryToOptional() {
            return secondary.secondaryToOptional();
        }

        @Override
        public ReactiveSeq<ST> secondaryToStream() {
            return secondary.secondaryToStream();
        }

        @Override
        public <LT1, RT1> Ior<LT1, RT1> flatMap(final Function<? super PT, ? extends MonadicValue2<? extends LT1, ? extends RT1>> mapper) {
            return Ior.both(secondary.flatMap(mapper), primary.flatMap(mapper));
        }

        @Override
        public <LT1, RT1> Ior<LT1, RT1> secondaryFlatMap(final Function<? super ST, ? extends Ior<LT1, RT1>> mapper) {
            return Ior.both(secondary.secondaryFlatMap(mapper), primary.secondaryFlatMap(mapper));
        }

        @Override
        public Ior<ST, PT> secondaryToPrimayFlatMap(final Function<? super ST, ? extends Ior<ST, PT>> fn) {
            return Ior.both(secondary.secondaryToPrimayFlatMap(fn), primary.secondaryToPrimayFlatMap(fn));
        }

        @Override
        public Ior<ST, PT> bipeek(final Consumer<? super ST> stAction, final Consumer<? super PT> ptAction) {
            secondary.secondaryPeek(stAction);
            primary.peek(ptAction);
            return this;
        }

        @Override
        public <R1, R2> Ior<R1, R2> bimap(final Function<? super ST, ? extends R1> fn1, final Function<? super PT, ? extends R2> fn2) {
            return Ior.both((Ior) secondary.secondaryMap(fn1), (Ior) primary.map(fn2));
        }

        @Override
        public boolean isPrimary() {

            return false;
        }

        @Override
        public boolean isSecondary() {

            return false;
        }

        @Override
        public boolean isBoth() {
            return true;
        }

        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Ior.both[" + primary.toString() + ":" + secondary.toString() + "]";
        }

        @Override
        public <R> Eval<R> matches(
                final Function<com.aol.cyclops.control.Matchable.CheckValue1<ST, R>, com.aol.cyclops.control.Matchable.CheckValue1<ST, R>> fn1,
                final Function<com.aol.cyclops.control.Matchable.CheckValue1<PT, R>, com.aol.cyclops.control.Matchable.CheckValue1<PT, R>> fn2,
                final Function<com.aol.cyclops.control.Matchable.CheckValue2<ST, PT, R>, com.aol.cyclops.control.Matchable.CheckValue2<ST, PT, R>> fn3,
                final Supplier<? extends R> otherwise) {
            final Matchable.MTuple2<ST, PT> mt2 = () -> Tuple.tuple(secondary.secondaryGet(), primary.get());
            return mt2.matches(fn3, otherwise);

        }

        /* (non-Javadoc)
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Ior<ST, R> combine(final Value<? extends T2> app, final BiFunction<? super PT, ? super T2, ? extends R> fn) {
            return app.toXor()
                      .visit(s -> Ior.secondary(this.secondaryGet()), f -> Ior.both(this.secondaryGet(), fn.apply(get(), app.get())));
        }
    }

}