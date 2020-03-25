package dev.w1zzrd.util.support;

import jline.internal.Nullable;

/**
 * Used to return a value from a safe invocation of a method while also conveying whether or not the method threw and exception.
 */
public final class Result<T> {

    /**
     * The value returned from a method invocation. Can be null even after successful method execution if method returns null or is void.
     * The value should also be null if method throws and exception.
     */
    public final @Nullable T value;

    /**
     * Let's the user determine whether or not a safe invocation of a method was interrupted by an error.
     * If so, {@link Result#value} <em>should</em> be null and {@link Result#error} <em>should</em> be non-null.
     */
    public final boolean success;

    /**
     * Should only contain a value of invoking the method caused an exception to be thrown (success==false).
     */
    public final @Nullable Throwable error;
    public Result(@Nullable T t, boolean success, @Nullable Throwable error){ value = t; this.success = success; this.error = error; }
}
