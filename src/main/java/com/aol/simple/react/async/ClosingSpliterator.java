package com.aol.simple.react.async;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.aol.simple.react.async.Queue.ClosedQueueException;


public class ClosingSpliterator<T> implements Spliterator<T> {
        private long estimate;
        final Supplier<T> s;

        protected ClosingSpliterator(long estimate,Supplier<T> s) {
            this.estimate = estimate;
            this.s = s;
        }

        @Override
        public long estimateSize() {
            return estimate;
        }

        @Override
        public int characteristics() {
            return IMMUTABLE;
        }
        
    

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			 Objects.requireNonNull(action);
            try{ 
            	System.out.println(action.getClass());
            	System.out.println(action);
            	action.accept(s.get());
             return true;
            }catch(ClosedQueueException e){
            	return false;
            }catch(Exception e){
            	e.printStackTrace();
            	return false;
            }
            
		}

		@Override
		public Spliterator<T> trySplit() {
			
			return new ClosingSpliterator(estimate >>>= 1, s);
		}

       
    }
  
    
