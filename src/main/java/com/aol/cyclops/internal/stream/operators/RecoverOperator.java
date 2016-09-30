package com.aol.cyclops.internal.stream.operators;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

import com.aol.cyclops.control.StreamUtils;
import com.aol.cyclops.util.ExceptionSoftener;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RecoverOperator<T> {

    private final Stream<T> stream;
    private final Class<Throwable> type;

    private static final Object UNSET = new Object();

    public Stream<T> recover(Function<Throwable, ? extends T> fn) {
        Iterator<T> it = stream.iterator();

        return StreamUtils.stream(new Iterator<T>() {
            T result = (T) UNSET;

            @Override
            public boolean hasNext() {
                try {
                    return it.hasNext();
                } catch (Throwable t) {
                    if (type.isAssignableFrom(t.getClass())) {
                        result = fn.apply(t);
                        return true;
                    }
                    ExceptionSoftener.throwSoftenedException(t);
                    return false;
                }

            }

            @Override
            public T next() {
                if (result != UNSET) {
                    T toReturn = result;
                    result = (T) UNSET;
                    return toReturn;
                }
                return it.next();

            }

        });
    }
}
