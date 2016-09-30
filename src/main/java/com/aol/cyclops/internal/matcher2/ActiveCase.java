package com.aol.cyclops.internal.matcher2;

import java.util.function.Function;
import java.util.function.Predicate;

import org.jooq.lambda.tuple.Tuple2;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A functionally compositional class that represents a pattern matching Case
 * 
 * Consists of a Predicate and a Function
 * When match is called, if the predicate holds the function is executed
 * 
 * 
 * @author johnmcclean
 *
 * @param <T> Input type for predicate and function (action)
 * @param <R> Return type for function (action) which is executed if the predicate tests positive
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class ActiveCase<T, R> implements Case<T, R> {

    private final Tuple2<Predicate<? super T>, Function<? super T, ? extends R>> pattern;
    @Getter
    private final boolean empty = false;

    public Tuple2<Predicate<? super T>, Function<? super T, ? extends R>> get() {
        return pattern;
    }

}
