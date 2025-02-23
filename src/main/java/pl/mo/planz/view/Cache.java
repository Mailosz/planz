package pl.mo.planz.view;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Cache<K,T> {
    Map<K,T> objects = new HashMap<K,T>();
    Function<K,T> loadFunction;

    public Cache(Function<K,T> loadFunction) {
        this.loadFunction = loadFunction;
    }

    public T get(K key) {
        if (!objects.containsKey(key)) {

            var object = loadFunction.apply(key);
            objects.put(key, object);
            return object;
        }
        return objects.get(key);
    }
}
