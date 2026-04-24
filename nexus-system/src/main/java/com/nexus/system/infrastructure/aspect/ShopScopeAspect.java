package com.nexus.system.infrastructure.aspect;

import com.nexus.common.annotation.ShopScope;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ShopScopeAspect {

    // TODO: implement shop-level data scope filtering via DataScopeContext

    @Before("@annotation(shopScopeAnnotation)")
    public void doBefore(JoinPoint point, ShopScope shopScopeAnnotation) {
    }

    @After("@annotation(shopScopeAnnotation)")
    public void doAfter(JoinPoint point, ShopScope shopScopeAnnotation) {
    }
}
