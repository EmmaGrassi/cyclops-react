package com.aol.cyclops.util.function;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jooq.lambda.function.Consumer3;

/**
 * A FunctionalInterface for side-effecting statements that accept 3 inputs (with no result).
 * The three-arity specialization of {@link Consumer}.
 * 
 * @author johnmcclean
 *
 * @param <S1> Type of first input parameter
 * @param <S2> Type of second input parameter
 * @param <S3> Type of third input parameter
 */
@FunctionalInterface
public interface TriConsumer<S1, S2, S3> {

    /**
     * Create a cyclops-react TriConsumer from a jOOλ Consumer3
     * @param c3 jOOλ Consumer3
     * @return cyclops-react TriConsumer
     */
    static <S1, S2, S3> TriConsumer<S1, S2, S3> fromConsumer3(Consumer3<S1,S2,S3> c3){
        return (a,b,c) ->c3.accept(a,b,c);
    }
    /**
     * Performs operation with input parameters
     *
     * @param a the first input parameter
     * @param b the second input parameter
     * @param c the third input parameter
     */
    void accept(S1 a, S2 b, S3 c);

    /**
     * @return A jOOλ Consumer3
     */
    default Consumer3<S1,S2,S3> consumer3(){
       return (a,b,c)->accept(a,b,c);
    }
    /**
     * Partially apply the first input parameter to this TriConsumer
     * 
     * @param s the first input parameter
     * @return A curried function that returns a Consumer
     */
    default Function<S2, Consumer<S3>> apply(S1 s) {
        return CurryConsumer.curryC3(this)
                            .apply(s);
    }

    /**
     * Partially apply the first and second input parameter to this TriConsumer
     * 
     * @param s the first input parameter
     * @param s2 the second input parameter
     * @return A Consumer that accepts the third parameter
     */
    default Consumer<S3> apply(S1 s, S2 s2) {
        return CurryConsumer.curryC3(this)
                            .apply(s)
                            .apply(s2);
    }
}
