package com.aol.cyclops.control.monads.transformers;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.reactivestreams.Publisher;

import com.aol.cyclops.Matchables;
import com.aol.cyclops.Monoid;
import com.aol.cyclops.control.AnyM;
import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.Trampoline;
import com.aol.cyclops.control.monads.transformers.seq.StreamTSeq;
import com.aol.cyclops.control.monads.transformers.values.FoldableTransformerSeq;
import com.aol.cyclops.control.monads.transformers.values.StreamTValue;
import com.aol.cyclops.data.collections.extensions.standard.ListX;
import com.aol.cyclops.types.MonadicValue;
import com.aol.cyclops.types.anyM.AnyMSeq;
import com.aol.cyclops.types.anyM.AnyMValue;

/**
 * Monad Transformer for Java Streams
 * 
 * StreamT allows the deeply wrapped Stream to be manipulating within it's nested /contained context
 * 
 * @author johnmcclean
 *
 * @param <T> Data type of the elements within the nested Stream(s)
 */
public interface StreamT<T> extends FoldableTransformerSeq<T> {

    public <R> StreamT<R> unitIterator(Iterator<R> it);

    public <R> StreamT<R> unit(R t);

    public <R> StreamT<R> empty();

    public <B> StreamT<B> flatMap(Function<? super T, ? extends Stream<? extends B>> f);

    /**
     * @return The wrapped AnyM
     */
    public AnyM<ReactiveSeq<T>> unwrap();

    /**
     * Peek at the current value of the Stream
     * <pre>
     * {@code 
     *    StreamT.of(AnyM.fromStream(Arrays.asStream(10))
     *             .peek(System.out::println);
     *             
     *     //prints 10        
     * }
     * </pre>
     * 
     * @param peek  Consumer to accept current value of Stream
     * @return StreamT with peek call
     */
    @Override
    public StreamT<T> peek(Consumer<? super T> peek);

    /**
     * Filter the wrapped Stream
     * <pre>
     * {@code 
     *    StreamT.of(AnyM.fromStream(Arrays.asStream(10,11))
     *             .filter(t->t!=10);
     *             
     *     //StreamT<AnyM<Stream<Stream[11]>>>
     * }
     * </pre>
     * @param test Predicate to filter the wrapped Stream
     * @return StreamT that applies the provided filter
     */
    @Override
    public StreamT<T> filter(Predicate<? super T> test);

    /**
     * Map the wrapped Stream
     * 
     * <pre>
     * {@code 
     *  StreamT.of(AnyM.fromStream(Arrays.asStream(10))
     *             .map(t->t=t+1);
     *  
     *  
     *  //StreamT<AnyM<Stream<Stream[11]>>>
     * }
     * </pre>
     * 
     * @param f Mapping function for the wrapped Stream
     * @return StreamT that applies the map function to the wrapped Stream
     */
    @Override
    public <B> StreamT<B> map(Function<? super T, ? extends B> f);

    /**
     * Flat Map the wrapped Stream
      * <pre>
     * {@code 
     *  StreamT.of(AnyM.fromStream(Arrays.asStream(10))
     *             .flatMap(t->Stream.empty();
     *  
     *  
     *  //StreamT<AnyM<Stream<Stream.empty>>>
     * }
     * </pre>
     * @param f FlatMap function
     * @return StreamT that applies the flatMap function to the wrapped Stream
     */
    default <B> StreamT<B> bind(final Function<? super T, StreamT<? extends B>> f) {
        return of(unwrap().map(stream -> stream.flatMap(a -> f.apply(a)
                                                              .unwrap()
                                                              .stream())
                                               .<B> flatMap(a -> a)));
    }

    /**
     * Lift a function into one that accepts and returns an StreamT
     * This allows multiple monad types to add functionality to existing functions and methods
     * 
     * e.g. to add iteration handling (via Stream) and nullhandling (via Optional) to an existing function
     * <pre>
     * {@code 
    	Function<Integer,Integer> add2 = i -> i+2;
    	Function<StreamT<Integer>, StreamT<Integer>> optTAdd2 = StreamT.lift(add2);
    	
    	Stream<Integer> nums = Stream.of(1,2);
    	AnyM<Stream<Integer>> stream = AnyM.fromOptional(Optional.of(nums));
    	
    	List<Integer> results = optTAdd2.apply(StreamT.of(stream))
    									.unwrap()
    									.<Optional<Stream<Integer>>>unwrap()
    									.get()
    									.collect(Collectors.toList());
    	//Stream.of(3,4);
     * 
     * 
     * }</pre>
     * 
     * 
     * @param fn Function to enhance with functionality from Stream and another monad type
     * @return Function that accepts and returns an StreamT
     */
    public static <U, R> Function<StreamT<U>, StreamT<R>> lift(final Function<? super U, ? extends R> fn) {
        return optTu -> optTu.map(input -> fn.apply(input));
    }

    /**
     * Construct an StreamT from an AnyM that contains a monad type that contains type other than Stream
     * The values in the underlying monad will be mapped to Stream<A>
     * 
     * @param anyM AnyM that doesn't contain a monad wrapping an Stream
     * @return StreamT
     */
    public static <A> StreamT<A> fromAnyM(final AnyM<A> anyM) {
        return of(anyM.map(Stream::of));
    }

    /**
     * Create a StreamT from an AnyM that wraps a monad containing a Stream
     * 
     * @param monads
     * @return
     */
    public static <A> StreamT<A> of(final AnyM<? extends Stream<A>> monads) {
        return Matchables.anyM(monads)
                         .visit(v -> StreamTValue.of(v), s -> StreamTSeq.of(s));
    }

    public static <A> StreamTValue<A> fromAnyMValue(final AnyMValue<A> anyM) {
        return StreamTValue.fromAnyM(anyM);
    }

    public static <A> StreamTSeq<A> fromAnyMSeq(final AnyMSeq<A> anyM) {
        return StreamTSeq.fromAnyM(anyM);
    }

    public static <A> StreamTSeq<A> fromIterable(final Iterable<Stream<A>> iterableOfStreams) {
        return StreamTSeq.of(AnyM.fromIterable(iterableOfStreams));
    }

    public static <A> StreamTSeq<A> fromStream(final Stream<Stream<A>> streamOfStreams) {
        return StreamTSeq.of(AnyM.fromStream(streamOfStreams));
    }

    public static <A> StreamTSeq<A> fromPublisher(final Publisher<Stream<A>> publisherOfStreams) {
        return StreamTSeq.of(AnyM.fromPublisher(publisherOfStreams));
    }

    public static <A, V extends MonadicValue<? extends Stream<A>>> StreamTValue<A> fromValue(final V monadicValue) {
        return StreamTValue.fromValue(monadicValue);
    }

    public static <A> StreamTValue<A> fromOptional(final Optional<Stream<A>> optional) {
        return StreamTValue.of(AnyM.fromOptional(optional));
    }

    public static <A> StreamTValue<A> fromFuture(final CompletableFuture<Stream<A>> future) {
        return StreamTValue.of(AnyM.fromCompletableFuture(future));
    }

    public static <A> StreamTValue<A> fromIterableValue(final Iterable<Stream<A>> iterableOfStreams) {
        return StreamTValue.of(AnyM.fromIterableValue(iterableOfStreams));
    }

    public static <T> StreamTSeq<T> emptyStream() {
        return StreamT.fromIterable(ReactiveSeq.empty());
    }

    /* (non-Javadoc)
    * @see com.aol.cyclops.types.Functor#cast(java.lang.Class)
    */
    @Override
    default <U> StreamT<U> cast(final Class<? extends U> type) {
        return (StreamT<U>) FoldableTransformerSeq.super.cast(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Functor#trampoline(java.util.function.Function)
     */
    @Override
    default <R> StreamT<R> trampoline(final Function<? super T, ? extends Trampoline<? extends R>> mapper) {
        return (StreamT<R>) FoldableTransformerSeq.super.trampoline(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Functor#patternMatch(java.util.function.Function, java.util.function.Supplier)
     */
    @Override
    default <R> StreamT<R> patternMatch(final Function<CheckValue1<T, R>, CheckValue1<T, R>> case1, final Supplier<? extends R> otherwise) {
        return (StreamT<R>) FoldableTransformerSeq.super.patternMatch(case1, otherwise);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Filterable#ofType(java.lang.Class)
     */
    @Override
    default <U> StreamT<U> ofType(final Class<? extends U> type) {

        return (StreamT<U>) FoldableTransformerSeq.super.ofType(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Filterable#filterNot(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> filterNot(final Predicate<? super T> fn) {

        return (StreamT<T>) FoldableTransformerSeq.super.filterNot(fn);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Filterable#notNull()
     */
    @Override
    default StreamT<T> notNull() {

        return (StreamT<T>) FoldableTransformerSeq.super.notNull();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#combine(java.util.function.BiPredicate, java.util.function.BinaryOperator)
     */
    @Override
    default StreamT<T> combine(final BiPredicate<? super T, ? super T> predicate, final BinaryOperator<T> op) {

        return (StreamT<T>) FoldableTransformerSeq.super.combine(predicate, op);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#cycle(int)
     */
    @Override
    default StreamT<T> cycle(final int times) {

        return (StreamT<T>) FoldableTransformerSeq.super.cycle(times);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#cycle(com.aol.cyclops.Monoid, int)
     */
    @Override
    default StreamT<T> cycle(final Monoid<T> m, final int times) {

        return (StreamT<T>) FoldableTransformerSeq.super.cycle(m, times);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#cycleWhile(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> cycleWhile(final Predicate<? super T> predicate) {

        return (StreamT<T>) FoldableTransformerSeq.super.cycleWhile(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#cycleUntil(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> cycleUntil(final Predicate<? super T> predicate) {

        return (StreamT<T>) FoldableTransformerSeq.super.cycleUntil(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#zip(java.lang.Iterable, java.util.function.BiFunction)
     */
    @Override
    default <U, R> StreamT<R> zip(final Iterable<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (StreamT<R>) FoldableTransformerSeq.super.zip(other, zipper);
    }

    @Override
    default <U, R> StreamT<R> zip(final Seq<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (StreamT<R>) FoldableTransformerSeq.super.zip(other, zipper);
    }

    @Override
    default <U, R> StreamT<R> zip(final Stream<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (StreamT<R>) FoldableTransformerSeq.super.zip(other, zipper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#zip(java.util.stream.Stream)
     */
    @Override
    default <U> StreamT<Tuple2<T, U>> zip(final Stream<? extends U> other) {

        return (StreamT) FoldableTransformerSeq.super.zip(other);
    }

    @Override
    default <U> StreamT<Tuple2<T, U>> zip(final Seq<? extends U> other) {

        return (StreamT) FoldableTransformerSeq.super.zip(other);
    }

    @Override
    default <U> StreamT<Tuple2<T, U>> zip(final Iterable<? extends U> other) {

        return (StreamT) FoldableTransformerSeq.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#zip3(java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    default <S, U> StreamT<Tuple3<T, S, U>> zip3(final Stream<? extends S> second, final Stream<? extends U> third) {

        return (StreamT) FoldableTransformerSeq.super.zip3(second, third);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#zip4(java.util.stream.Stream, java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    default <T2, T3, T4> StreamT<Tuple4<T, T2, T3, T4>> zip4(final Stream<? extends T2> second, final Stream<? extends T3> third,
            final Stream<? extends T4> fourth) {

        return (StreamT) FoldableTransformerSeq.super.zip4(second, third, fourth);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#zipWithIndex()
     */
    @Override
    default StreamT<Tuple2<T, Long>> zipWithIndex() {

        return (StreamT<Tuple2<T, Long>>) FoldableTransformerSeq.super.zipWithIndex();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#sliding(int)
     */
    @Override
    default StreamT<ListX<T>> sliding(final int windowSize) {

        return (StreamT<ListX<T>>) FoldableTransformerSeq.super.sliding(windowSize);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#sliding(int, int)
     */
    @Override
    default StreamT<ListX<T>> sliding(final int windowSize, final int increment) {

        return (StreamT<ListX<T>>) FoldableTransformerSeq.super.sliding(windowSize, increment);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#grouped(int, java.util.function.Supplier)
     */
    @Override
    default <C extends Collection<? super T>> StreamT<C> grouped(final int size, final Supplier<C> supplier) {

        return (StreamT<C>) FoldableTransformerSeq.super.grouped(size, supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#groupedUntil(java.util.function.Predicate)
     */
    @Override
    default StreamT<ListX<T>> groupedUntil(final Predicate<? super T> predicate) {

        return (StreamT<ListX<T>>) FoldableTransformerSeq.super.groupedUntil(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#groupedStatefullyUntil(java.util.function.BiPredicate)
     */
    @Override
    default StreamT<ListX<T>> groupedStatefullyUntil(final BiPredicate<ListX<? super T>, ? super T> predicate) {

        return (StreamT<ListX<T>>) FoldableTransformerSeq.super.groupedStatefullyUntil(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#groupedWhile(java.util.function.Predicate)
     */
    @Override
    default StreamT<ListX<T>> groupedWhile(final Predicate<? super T> predicate) {

        return (StreamT<ListX<T>>) FoldableTransformerSeq.super.groupedWhile(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#groupedWhile(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    default <C extends Collection<? super T>> StreamT<C> groupedWhile(final Predicate<? super T> predicate, final Supplier<C> factory) {

        return (StreamT<C>) FoldableTransformerSeq.super.groupedWhile(predicate, factory);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#groupedUntil(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    default <C extends Collection<? super T>> StreamT<C> groupedUntil(final Predicate<? super T> predicate, final Supplier<C> factory) {

        return (StreamT<C>) FoldableTransformerSeq.super.groupedUntil(predicate, factory);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#grouped(int)
     */
    @Override
    default StreamT<ListX<T>> grouped(final int groupSize) {

        return (StreamT<ListX<T>>) FoldableTransformerSeq.super.grouped(groupSize);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#grouped(java.util.function.Function, java.util.stream.Collector)
     */
    @Override
    default <K, A, D> StreamT<Tuple2<K, D>> grouped(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream) {

        return (StreamT) FoldableTransformerSeq.super.grouped(classifier, downstream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#grouped(java.util.function.Function)
     */
    @Override
    default <K> StreamT<Tuple2<K, Seq<T>>> grouped(final Function<? super T, ? extends K> classifier) {

        return (StreamT) FoldableTransformerSeq.super.grouped(classifier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#distinct()
     */
    @Override
    default StreamT<T> distinct() {

        return (StreamT<T>) FoldableTransformerSeq.super.distinct();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#scanLeft(com.aol.cyclops.Monoid)
     */
    @Override
    default StreamT<T> scanLeft(final Monoid<T> monoid) {

        return (StreamT<T>) FoldableTransformerSeq.super.scanLeft(monoid);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#scanLeft(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    default <U> StreamT<U> scanLeft(final U seed, final BiFunction<? super U, ? super T, ? extends U> function) {

        return (StreamT<U>) FoldableTransformerSeq.super.scanLeft(seed, function);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#scanRight(com.aol.cyclops.Monoid)
     */
    @Override
    default StreamT<T> scanRight(final Monoid<T> monoid) {

        return (StreamT<T>) FoldableTransformerSeq.super.scanRight(monoid);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#scanRight(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    default <U> StreamT<U> scanRight(final U identity, final BiFunction<? super T, ? super U, ? extends U> combiner) {

        return (StreamT<U>) FoldableTransformerSeq.super.scanRight(identity, combiner);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#sorted()
     */
    @Override
    default StreamT<T> sorted() {

        return (StreamT<T>) FoldableTransformerSeq.super.sorted();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#sorted(java.util.Comparator)
     */
    @Override
    default StreamT<T> sorted(final Comparator<? super T> c) {

        return (StreamT<T>) FoldableTransformerSeq.super.sorted(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#takeWhile(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> takeWhile(final Predicate<? super T> p) {

        return (StreamT<T>) FoldableTransformerSeq.super.takeWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#dropWhile(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> dropWhile(final Predicate<? super T> p) {

        return (StreamT<T>) FoldableTransformerSeq.super.dropWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#takeUntil(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> takeUntil(final Predicate<? super T> p) {

        return (StreamT<T>) FoldableTransformerSeq.super.takeUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#dropUntil(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> dropUntil(final Predicate<? super T> p) {

        return (StreamT<T>) FoldableTransformerSeq.super.dropUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#dropRight(int)
     */
    @Override
    default StreamT<T> dropRight(final int num) {

        return (StreamT<T>) FoldableTransformerSeq.super.dropRight(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#takeRight(int)
     */
    @Override
    default StreamT<T> takeRight(final int num) {

        return (StreamT<T>) FoldableTransformerSeq.super.takeRight(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#skip(long)
     */
    @Override
    default StreamT<T> skip(final long num) {

        return (StreamT<T>) FoldableTransformerSeq.super.skip(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#skipWhile(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> skipWhile(final Predicate<? super T> p) {

        return (StreamT<T>) FoldableTransformerSeq.super.skipWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#skipUntil(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> skipUntil(final Predicate<? super T> p) {

        return (StreamT<T>) FoldableTransformerSeq.super.skipUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#limit(long)
     */
    @Override
    default StreamT<T> limit(final long num) {

        return (StreamT<T>) FoldableTransformerSeq.super.limit(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#limitWhile(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> limitWhile(final Predicate<? super T> p) {

        return (StreamT<T>) FoldableTransformerSeq.super.limitWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#limitUntil(java.util.function.Predicate)
     */
    @Override
    default StreamT<T> limitUntil(final Predicate<? super T> p) {

        return (StreamT<T>) FoldableTransformerSeq.super.limitUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#intersperse(java.lang.Object)
     */
    @Override
    default StreamT<T> intersperse(final T value) {

        return (StreamT<T>) FoldableTransformerSeq.super.intersperse(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#reverse()
     */
    @Override
    default StreamT<T> reverse() {

        return (StreamT<T>) FoldableTransformerSeq.super.reverse();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#shuffle()
     */
    @Override
    default StreamT<T> shuffle() {

        return (StreamT<T>) FoldableTransformerSeq.super.shuffle();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#skipLast(int)
     */
    @Override
    default StreamT<T> skipLast(final int num) {

        return (StreamT<T>) FoldableTransformerSeq.super.skipLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#limitLast(int)
     */
    @Override
    default StreamT<T> limitLast(final int num) {

        return (StreamT<T>) FoldableTransformerSeq.super.limitLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#onEmpty(java.lang.Object)
     */
    @Override
    default StreamT<T> onEmpty(final T value) {

        return (StreamT<T>) FoldableTransformerSeq.super.onEmpty(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#onEmptyGet(java.util.function.Supplier)
     */
    @Override
    default StreamT<T> onEmptyGet(final Supplier<? extends T> supplier) {

        return (StreamT<T>) FoldableTransformerSeq.super.onEmptyGet(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#onEmptyThrow(java.util.function.Supplier)
     */
    @Override
    default <X extends Throwable> StreamT<T> onEmptyThrow(final Supplier<? extends X> supplier) {

        return (StreamT<T>) FoldableTransformerSeq.super.onEmptyThrow(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#shuffle(java.util.Random)
     */
    @Override
    default StreamT<T> shuffle(final Random random) {

        return (StreamT<T>) FoldableTransformerSeq.super.shuffle(random);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#slice(long, long)
     */
    @Override
    default StreamT<T> slice(final long from, final long to) {

        return (StreamT<T>) FoldableTransformerSeq.super.slice(from, to);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.control.monads.transformers.values.TransformerSeq#sorted(java.util.function.Function)
     */
    @Override
    default <U extends Comparable<? super U>> StreamT<T> sorted(final Function<? super T, ? extends U> function) {
        return (StreamT) FoldableTransformerSeq.super.sorted(function);
    }
}