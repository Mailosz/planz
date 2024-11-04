package pl.mo.planz.utils;

import java.util.function.Supplier;

public class Lazy<T> {


    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }
    
    private Supplier<T> supplier;
    private T object;

    public T get() {

        if (object == null) {
            synchronized (this) {
                if (object == null) {
                    object = this.supplier.get();
                }
            }
        }
        return object;
    }
}
