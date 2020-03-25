package dev.w1zzrd.util.automation;

@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Auto {
    Class<?> value() default Infer.class;
    String name() default "";
}
