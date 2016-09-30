package com.aol.cyclops.util.function;

import java.util.function.Function;

import org.jooq.lambda.function.Function3;

@FunctionalInterface
public interface TriFunction<S1, S2, S3, R> {

    public R apply(S1 a, S2 b, S3 c);

    default Function3<S1,S2,S3,R> function3(){
        return (a,b,c)->apply(a,b,c);
    }
    default Function<S2, Function<S3, R>> apply(S1 s) {
        return Curry.curry3(this)
                    .apply(s);
    }

    default Function<S3, R> apply(S1 s, S2 s2) {
        return Curry.curry3(this)
                    .apply(s)
                    .apply(s2);
    }
}
