package com.nexus.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    int expireSeconds() default 3;

    String message() default "您的请求正在处理中，请勿频繁提交";

    boolean releaseOnError() default true;
}
