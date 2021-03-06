package com.aol.cyclops.types.stream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import com.aol.cyclops.Matchables;
import com.aol.cyclops.control.FluentFunctions;
import com.aol.cyclops.internal.stream.BaseHotStreamImpl;

public class NonPausableHotStream<T> extends BaseHotStreamImpl<T> {
    public NonPausableHotStream(final Stream<T> stream) {
        super(stream);
    }

    @Override
    public HotStream<T> init(final Executor exec) {
        CompletableFuture.runAsync(() -> {
            pause.get()
                 .join();
            stream.forEach(a -> {

                final int local = connected;

                for (int i = 0; i < local; i++) {

                    Matchables.blocking(connections.get(i))
                              .visit(FluentFunctions.ofChecked(in -> {
                        in.put(a);
                        return true;
                    }), q -> q.offer(a));
                }

            });

            open.set(false);

        } , exec);
        return this;
    }
}
