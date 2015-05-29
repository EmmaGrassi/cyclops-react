package com.aol.cyclops.functions;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import lombok.Value;
import lombok.val;

import com.aol.cyclops.lambda.utils.ExceptionSoftener;
import com.aol.cyclops.lambda.utils.LazyImmutable;

public class Memoise {

	/**
	 * Convert a Supplier into one that caches it's result
	 * 
	 * @param s Supplier to memoise
	 * @return Memoised Supplier
	 */
	public static <T> Supplier<T> memoiseSupplier(Supplier<T> s){
		LazyImmutable<T> lazy = LazyImmutable.def();
		return () -> lazy.computeIfAbsent(s);
	}
	/**
	 * Convert a Callable into one that caches it's result
	 * 
	 * @param s Callable to memoise
	 * @return Memoised Callable
	 */
	public static <T> Callable<T> memoiseCallable(Callable<T> s){
		LazyImmutable<T> lazy = LazyImmutable.def();
		return () -> lazy.computeIfAbsent(() -> { 
			try { 
				return s.call();
			}catch(Exception e){
				ExceptionSoftener.singleton.factory.getInstance().throwSoftenedException(e);
				return null;
			}
			
		});
	}
	
	/**
	 * Convert a Function into one that caches it's result
	 * 
	 * @param fn Function to memoise
	 * @return Memoised Function
	 */
	public static <T,R> Function<T,R> memoiseFunction(Function<T,R> fn){
		Map<T,R> lazy = new ConcurrentHashMap<>();
		return t -> lazy.computeIfAbsent(t,fn);
	}
	
	/**
	 * Convert a BiFunction into one that caches it's result
	 * 
	 * @param fn BiFunction to memoise
	 * @return Memoised BiFunction
	 */
	public static <T1,T2 , R> BiFunction<T1, T2, R> memoiseBiFunction(BiFunction<T1, T2, R> fn) {
		val memoise2 = memoiseFunction((Pair<T1,T2> pair) -> fn.apply(pair._1,pair._2));
		return (t1,t2) -> memoise2.apply(new Pair<>(t1,t2));
	}
	/**
	 * Convert a Predicate into one that caches it's result
	 * 
	 * @param p Predicate to memoise
	 * @return Memoised Predicate
	 */
	public static <T> Predicate<T> memoisePredicate(Predicate<T> p) {
		Function<T, Boolean> memoised = memoiseFunction((Function<T,Boolean>)t-> p.test(t));
		return (t) -> memoised.apply(t);
	}
	@Value
	private static class Pair<T1,T2>{
		T1 _1;
		T2 _2;
	}
}
