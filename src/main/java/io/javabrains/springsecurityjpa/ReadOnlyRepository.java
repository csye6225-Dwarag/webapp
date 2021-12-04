package io.javabrains.springsecurityjpa;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface ReadOnlyRepository {
}
