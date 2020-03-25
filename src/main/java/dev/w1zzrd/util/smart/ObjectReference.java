package dev.w1zzrd.util.smart;

/**
 * Points to/stores the value of an object. This defines a meta-structure for passing arguments to a class.
 * @param <T> Type of the object returned by {@link ObjectReference#get()}
 */
public interface ObjectReference<T> {
    /**
     * Access the object that this reference points to.
     * @return Stored object.
     */
    T get();
}