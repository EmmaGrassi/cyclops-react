package com.aol.cyclops.data.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.react.async.subscription.Continueable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Synchronized;

/**
 * A class that can accept input streams and generate output streams where data sent in the Topic is guaranteed to be
 * provided to all Topic subsribers
 * 
 * @author johnmcclean
 *
 * @param <T> Data type for the Topic
 */
public class Topic<T> implements Adapter<T> {

    @Getter(AccessLevel.PACKAGE)
    private final DistributingCollection<T> distributor = new DistributingCollection<T>();
    @Getter(AccessLevel.PACKAGE)
    private volatile PMap<Seq, Queue<T>> streamToQueue = HashTreePMap.empty();
    private final Object lock = new Object();
    private volatile int index = 0;

    /**
     * Construct a new Topic
     */
    public Topic() {
        final Queue<T> q = new Queue<T>();

        distributor.addQueue(q);
    }

    /**
     * Construct a Topic using the Queue provided
     * @param q Queue to back this Topic with
     */
    public Topic(final Queue<T> q) {

        distributor.addQueue(q);
    }

    /**
     * Topic will maintain a queue for each Subscribing Stream
     * If a Stream is finished with a Topic it is good practice to disconnect from the Topic 
     * so messages will no longer be stored for that Stream
     * 
     * @param stream
     */
    @Synchronized("lock")
    public void disconnect(final Stream<T> stream) {

        distributor.removeQueue(streamToQueue.get(stream));

        this.streamToQueue = streamToQueue.minus(stream);
        this.index--;
    }

    @Synchronized("lock")
    private <R> ReactiveSeq<R> connect(final Function<Queue<T>, ReactiveSeq<R>> streamCreator) {
        final Queue<T> queue = this.getNextQueue();
        final ReactiveSeq<R> stream = streamCreator.apply(queue);

        this.streamToQueue = streamToQueue.plus(stream, queue);
        return stream;
    }

    /**
     * @param stream Input data from provided Stream
     */
    @Override
    public boolean fromStream(final Stream<T> stream) {
        stream.collect(Collectors.toCollection(() -> distributor));
        return true;

    }

    /**
     * Generating a streamCompletableFutures will register the Stream as a subscriber to this topic.
     * It will be provided with an internal Queue as a mailbox. @see Topic.disconnect to disconnect from the topic
     * 
     * @return Stream of CompletableFutures that can be used as input into a SimpleReact concurrent dataflow
     */
    @Override
    public ReactiveSeq<CompletableFuture<T>> streamCompletableFutures() {
        return connect(q -> q.streamCompletableFutures());
    }

    /**
     * Generating a stream will register the Stream as a subscriber to this topic.
     * It will be provided with an internal Queue as a mailbox. @see Topic.disconnect to disconnect from the topic
     * @return Stream of data
     */
    @Override
    public ReactiveSeq<T> stream() {

        return connect(q -> q.stream());

    }

    @Override
    public ReactiveSeq<T> stream(final Continueable s) {

        return connect(q -> q.stream(s));

    }

    private Queue<T> getNextQueue() {

        if (index >= this.distributor.getSubscribers()
                                     .size()) {

            this.distributor.addQueue(new Queue());

        }
        return this.distributor.getSubscribers()
                               .get(index++);
    }

    /**
     * Close this Topic
     * 
     * @return true if closed
     */
    @Override
    public boolean close() {
        this.distributor.getSubscribers()
                        .forEach(it -> it.close());
        return true;

    }

    /**
     * @return Track changes in size in the Topic's data
     */
    public Signal<Integer> getSizeSignal(final int index) {
        return this.distributor.getSubscribers()
                               .get(index)
                               .getSizeSignal();
    }

    public void setSizeSignal(final int index, final Signal<Integer> s) {
        this.distributor.getSubscribers()
                        .get(index)
                        .setSizeSignal(s);
    }

    /**
     * Add a single datapoint to this Queue
     * 
     * @param data data to add
     * @return self
     */
    @Override
    public boolean offer(final T data) {
        fromStream(Stream.of(data));
        return true;

    }

    static class DistributingCollection<T> extends ArrayList<T> {

        private static final long serialVersionUID = 1L;
        @Getter
        private volatile PVector<Queue<T>> subscribers = TreePVector.empty();

        private final Object lock = new Object();

        @Synchronized("lock")
        public void addQueue(final Queue<T> q) {
            subscribers = subscribers.plus(q);
        }

        @Synchronized("lock")
        public void removeQueue(final Queue<T> q) {
            subscribers = subscribers.minus(q);

        }

        @Override
        public boolean add(final T e) {
            subscribers.forEach(it -> it.offer(e));
            return true;
        }

        @Override
        public boolean addAll(final Collection<? extends T> c) {
            subscribers.forEach(it -> c.forEach(next -> it.offer(next)));
            return true;
        }

    }

    @Override
    public <R> R visit(final Function<? super Queue<T>, ? extends R> caseQueue, final Function<? super Topic<T>, ? extends R> caseTopic) {
        return caseTopic.apply(this);
    }

}
