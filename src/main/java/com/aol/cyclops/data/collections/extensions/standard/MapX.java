package com.aol.cyclops.data.collections.extensions.standard;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.lambda.Collectable;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.StreamUtils;
import com.aol.cyclops.control.Trampoline;
import com.aol.cyclops.data.collections.extensions.FluentMapX;
import com.aol.cyclops.types.BiFunctor;
import com.aol.cyclops.types.Foldable;
import com.aol.cyclops.types.Functor;
import com.aol.cyclops.types.IterableFilterable;
import com.aol.cyclops.types.OnEmpty;
import com.aol.cyclops.types.OnEmptySwitch;
import com.aol.cyclops.types.stream.CyclopsCollectable;

/**
 * An eXtended Map type, that offers additional eagerly executed functional style operators such as bimap, filter and more
 * 
 * @author johnmcclean
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public interface MapX<K, V> extends Map<K, V>, FluentMapX<K, V>, BiFunctor<K, V>, Functor<V>, IterableFilterable<Tuple2<K, V>>, OnEmpty<Tuple2<K, V>>,
        OnEmptySwitch<Tuple2<K, V>, Map<K, V>>, Publisher<Tuple2<K, V>>, Foldable<Tuple2<K, V>>, CyclopsCollectable<Tuple2<K, V>> {

    
    
    /**
     * 
     * @return A Collector that generates a mutable Map from a Collection of Tuple2
     */
    static <K, V> Collector<Tuple2<? extends K, ? extends V>, ?, Map<K, V>> defaultCollector() {
        return Collectors.toMap(t -> t.v1, t -> t.v2);
    }

    /**
     * @return A Collector that generates an Immutable Map from a Collection of Tuple2
     */
    static <K, V> Collector<Tuple2<? extends K, ? extends V>, ?, Map<K, V>> immutableCollector() {
        return Collectors.collectingAndThen(defaultCollector(), Collections::unmodifiableMap);

    }

    /**
     * @return A Collector that generates a mutable MapX from a Collection of Tuple2
     */
    static <K, V> Collector<Tuple2<? extends K, ? extends V>, ?, Map<K, V>> toMapX() {
        return Collectors.collectingAndThen(defaultCollector(), (Map<K, V> d) -> new MapXImpl<K, V>(
                                                                                                    d, defaultCollector()));

    }

    /**
     * @return The currently configured Map Collector for this MapX
     */
    public <K, V> Collector<Tuple2<? extends K, ? extends V>, ?, Map<K, V>> getCollector();

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Foldable#stream()
     */
    default ReactiveSeq<Tuple2<K, V>> stream() {
        return ReactiveSeq.fromIterable(this.entrySet())
                          .map(e -> Tuple.tuple(e.getKey(), e.getValue()));
    }

    /**
     * @return An empty MapX
     */
    static <K, V> MapX<K, V> empty() {
        return fromMap(new HashMap<K, V>());
    }

    /**
     * Wrap a Map in a MapX
     * 
     * @param map to wrap
     * @return MapX wrapping the supplied Map
     */
    public static <K, V> MapX<K, V> fromMap(Map<? extends K, ? extends V> map) {
        return fromMap(defaultCollector(), map);
    }

    /**
     * Wrap a map in a MapX, also supplying a Collector for use in operations
     * 
     * @param collector To generate new MapX's from
     * @param map to wrap
     * @return MapX wrapping the supplied Map
     */
    public static <K, V> MapX<K, V> fromMap(Collector<Tuple2<? extends K, ? extends V>, ?, Map<K, V>> collector, Map<? extends K, ? extends V> map) {
        if (map instanceof MapX)
            return (MapX) map;
        if (map instanceof Map)
            return new MapXImpl<K, V>(
                                      (Map) map, collector);
        return new MapXImpl<K, V>(
                                  StreamUtils.stream(map)
                                             .map(e -> Tuple.tuple(e.getKey(), e.getValue()))
                                             .collect(collector),
                                  collector);
    }

    /**
     * Construct a new MapX with the same collector from the supplied Stream
     * 
     * @param stream ot Tuples to convert into a MapX
     * @return MapX
     */
    default MapX<K, V> fromStream(ReactiveSeq<Tuple2<K, V>> stream) {
        return new MapXImpl<>(
                              stream.toMap(t -> t.v1, t -> t.v2), getCollector());
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    default Iterator<Tuple2<K, V>> iterator() {
        return stream().iterator();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.sequence.SequenceMCollectable#collectable()
     */
    @Override
    default Collectable<Tuple2<K, V>> collectable() {
        return stream();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Functor#map(java.util.function.Function)
     */
    default <KR, VR> MapX<KR, VR> flatMap(BiFunction<? super K, ? super V, ? extends MapX<KR, VR>> fn) {

        ReactiveSeq<Tuple2<KR, VR>> s = stream().flatMap(t -> fn.apply(t.v1, t.v2)
                                                                .stream());
        return new MapXImpl<>(
                              s.<KR, VR> toMap(t -> t.v1, t -> t.v2), getCollector());
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Functor#map(java.util.function.Function)
     */
    @Override
    default <R> MapX<K, R> map(Function<? super V, ? extends R> fn) {

        ReactiveSeq<Tuple2<K, R>> s = stream().map(t -> t.map2(v -> fn.apply(v)));
        return new MapXImpl<>(
                              s.<K, R> toMap(t -> t.v1, t -> t.v2), getCollector());
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.MapX#bimap(java.util.function.Function, java.util.function.Function)
     */
    @Override
    default <R1, R2> MapX<R1, R2> bimap(Function<? super K, ? extends R1> fn1, Function<? super V, ? extends R2> fn2) {
        ReactiveSeq<Tuple2<R1, V>> s1 = stream().map(t -> t.map1(v -> fn1.apply(v)));
        ReactiveSeq<Tuple2<R1, R2>> s2 = s1.map(t -> t.map2(v -> fn2.apply(v)));
        return new MapXImpl<>(
                              s2.<R1, R2> toMap(t -> t.v1, t -> t.v2), getCollector());
    }

    @Override
    int size();

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.CyclopsCollectable#allMatch(java.util.function.Predicate)
     */
    @Override
    default boolean allMatch(Predicate<? super Tuple2<K, V>> c) {
        return CyclopsCollectable.super.allMatch(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.CyclopsCollectable#anyMatch(java.util.function.Predicate)
     */
    @Override
    default boolean anyMatch(Predicate<? super Tuple2<K, V>> c) {
        return CyclopsCollectable.super.anyMatch(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.CyclopsCollectable#noneMatch(java.util.function.Predicate)
     */
    @Override
    default boolean noneMatch(Predicate<? super Tuple2<K, V>> c) {
        return CyclopsCollectable.super.noneMatch(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.CyclopsCollectable#max(java.util.Comparator)
     */
    @Override
    default Optional<Tuple2<K, V>> max(Comparator<? super Tuple2<K, V>> comparator) {
        return CyclopsCollectable.super.max(comparator);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.stream.CyclopsCollectable#min(java.util.Comparator)
     */
    @Override
    default Optional<Tuple2<K, V>> min(Comparator<? super Tuple2<K, V>> comparator) {
        return CyclopsCollectable.super.min(comparator);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.FluentMapX#plus(java.lang.Object, java.lang.Object)
     */
    @Override
    default MapX<K, V> plus(K key, V value) {
        return (MapX<K, V>) FluentMapX.super.plus(key, value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.FluentMapX#plusAll(java.util.Map)
     */
    @Override
    default MapX<K, V> plusAll(Map<? extends K, ? extends V> map) {
        return (MapX<K, V>) FluentMapX.super.plusAll(map);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.FluentMapX#minus(java.lang.Object)
     */
    default MapX<K, V> minus(Object key) {
        return (MapX<K, V>) FluentMapX.super.minus(key);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.FluentMapX#minusAll(java.util.Collection)
     */
    default MapX<K, V> minusAll(Collection<?> keys) {
        return (MapX<K, V>) FluentMapX.super.minusAll(keys);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Functor#cast(java.lang.Class)
     */
    @Override
    default <U> MapX<K, U> cast(Class<? extends U> type) {

        return (MapX<K, U>) Functor.super.cast(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Functor#peek(java.util.function.Consumer)
     */
    @Override
    default MapX<K, V> peek(Consumer<? super V> c) {

        return (MapX<K, V>) Functor.super.peek(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Functor#trampoline(java.util.function.Function)
     */
    @Override
    default <R> MapX<K, R> trampoline(Function<? super V, ? extends Trampoline<? extends R>> mapper) {

        return (MapX<K, R>) Functor.super.trampoline(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#filter(java.util.function.Predicate)
     */
    @Override
    default MapX<K, V> filter(Predicate<? super Tuple2<K, V>> fn) {
        return stream().filter(fn)
                       .toMapX(t -> t.v1, t -> t.v2);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#filterNot(java.util.function.Predicate)
     */
    @Override
    default MapX<K, V> filterNot(Predicate<? super Tuple2<K, V>> fn) {

        return (MapX<K, V>) IterableFilterable.super.filterNot(fn);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#notNull()
     */
    @Override
    default MapX<K, V> notNull() {

        return (MapX<K, V>) IterableFilterable.super.notNull();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#removeAll(java.util.stream.Stream)
     */
    @Override
    default MapX<K, V> removeAll(Stream<? extends Tuple2<K, V>> stream) {

        return (MapX<K, V>) IterableFilterable.super.removeAll(stream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#removeAll(java.lang.Iterable)
     */
    @Override
    default MapX<K, V> removeAll(Iterable<? extends Tuple2<K, V>> it) {

        return (MapX<K, V>) IterableFilterable.super.removeAll(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#removeAll(java.lang.Object[])
     */
    @Override
    default MapX<K, V> removeAll(Tuple2<K, V>... values) {

        return (MapX<K, V>) IterableFilterable.super.removeAll(values);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#retainAll(java.lang.Iterable)
     */
    @Override
    default MapX<K, V> retainAll(Iterable<? extends Tuple2<K, V>> it) {

        return (MapX<K, V>) IterableFilterable.super.retainAll(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#retainAll(java.util.stream.Stream)
     */
    @Override
    default MapX<K, V> retainAll(Stream<? extends Tuple2<K, V>> stream) {

        return (MapX<K, V>) IterableFilterable.super.retainAll(stream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Filterable#retainAll(java.lang.Object[])
     */
    @Override
    default MapX<K, V> retainAll(Tuple2<K, V>... values) {

        return (MapX<K, V>) IterableFilterable.super.retainAll(values);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Functor#patternMatch(java.util.function.Function, java.util.function.Supplier)
     */
    @Override
    default <R> MapX<K, R> patternMatch(Function<CheckValue1<V, R>, CheckValue1<V, R>> case1, Supplier<? extends R> otherwise) {

        return (MapX<K, R>) Functor.super.patternMatch(case1, otherwise);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.BiFunctor#bipeek(java.util.function.Consumer, java.util.function.Consumer)
     */
    @Override
    default MapX<K, V> bipeek(Consumer<? super K> c1, Consumer<? super V> c2) {

        return (MapX<K, V>) BiFunctor.super.bipeek(c1, c2);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.BiFunctor#bicast(java.lang.Class, java.lang.Class)
     */
    @Override
    default <U1, U2> MapX<U1, U2> bicast(Class<U1> type1, Class<U2> type2) {

        return (MapX<U1, U2>) BiFunctor.super.bicast(type1, type2);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.BiFunctor#bitrampoline(java.util.function.Function, java.util.function.Function)
     */
    @Override
    default <R1, R2> MapX<R1, R2> bitrampoline(Function<? super K, ? extends Trampoline<? extends R1>> mapper1,
            Function<? super V, ? extends Trampoline<? extends R2>> mapper2) {

        return (MapX<R1, R2>) BiFunctor.super.bitrampoline(mapper1, mapper2);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Traversable#subscribe(org.reactivestreams.Subscriber)
     */
    @Override
    default void subscribe(Subscriber<? super Tuple2<K, V>> s) {

        stream().subscribe(s);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.OnEmpty#onEmpty(java.lang.Object)
     */
    @Override
    default MapX<K, V> onEmpty(Tuple2<K, V> value) {

        return fromStream(stream().onEmpty(value));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.OnEmpty#onEmptyGet(java.util.function.Supplier)
     */
    @Override
    default MapX<K, V> onEmptyGet(Supplier<? extends Tuple2<K, V>> supplier) {

        return fromStream(stream().onEmptyGet(supplier));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.OnEmpty#onEmptyThrow(java.util.function.Supplier)
     */
    @Override
    default <X extends Throwable> MapX<K, V> onEmptyThrow(Supplier<? extends X> supplier) {

        return fromStream(stream().onEmptyThrow(supplier));
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.OnEmptySwitch#onEmptySwitch(java.util.function.Supplier)
     */
    @Override
    default MapX<K, V> onEmptySwitch(Supplier<? extends Map<K, V>> supplier) {
        if (this.isEmpty())
            return MapX.fromMap(supplier.get());
        return this;
    }
    
    /**
     * Convert this MapX to a ListX via the provided transformation function
     * 
     * @param fn Mapping function to transform each Map entry into a single value
     * @return ListX of transformed values
     */
    default <T> ListX<T> toListX(Function<? super Tuple2<? super K,? super V>,? extends T> fn){
        return ListX.narrow(stream().map(fn).toListX());
    }
    /**
     * Convert this MapX to a SetX via the provided transformation function
     * 
     * @param fn Mapping function to transform each Map entry into a single value
     * @return SetX of transformed values
     */
    default <T> SetX<T> toSetX(Function<? super Tuple2<? super K,? super V>,? extends T> fn){
        return SetX.narrow(stream().map(fn).toSetX());
    }
    /**
     * Convert this MapX to a SortedSetX via the provided transformation function
     * 
     * @param fn Mapping function to transform each Map entry into a single value
     * @return SortedSetX of transformed values
     */
    default <T> SortedSetX<T> toSortedSetX(Function<? super Tuple2<? super K,? super V>,? extends T> fn){
        return SortedSetX.narrow(stream().map(fn).toSortedSetX());
    }
    
    /**
     * Convert this MapX to a QueueX via the provided transformation function
     * 
     * @param fn Mapping function to transform each Map entry into a single value
     * @return QueueX of transformed values
     */
    default <T> QueueX<T> toQueueX(Function<? super Tuple2<? super K,? super V>,? extends T> fn){
        return QueueX.narrow(stream().map(fn).toQueueX());
    }
    
    /**
     * Convert this MapX to a DequeX via the provided transformation function
     * 
     * @param fn Mapping function to transform each Map entry into a single value
     * @return DequeX of transformed values
     */
    default <T> DequeX<T> toDequeX(Function<? super Tuple2<? super K,? super V>,? extends T> fn){
        return DequeX.narrow(stream().map(fn).toDequeX());
    }

}
