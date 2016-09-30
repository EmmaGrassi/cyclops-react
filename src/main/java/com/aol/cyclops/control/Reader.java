package com.aol.cyclops.control;

import java.util.function.Function;

import com.aol.cyclops.types.Functor;

/**
 * An interface that represents the Reader monad
 * 
 * @author johnmcclean
 *
 * @param <T> Current input type of Function
 * @param <R> Current return type of Function
 */
public interface Reader<T, R> extends Function<T, R>, Functor<R> {

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Functor#map(java.util.function.Function)
     */
    @Override
    default <R1> Reader<T, R1> map(final Function<? super R, ? extends R1> f2) {
        return FluentFunctions.of(this.andThen(f2));
    }

    default <R1> Reader<T, R1> flatMap(final Function<? super R, ? extends Reader<T, R1>> f) {
        return FluentFunctions.of(a -> f.apply(apply(a))
                                        .apply(a));
    }

    default AnyM<R> anyM() {
        return AnyM.ofValue(this);
    }

}
