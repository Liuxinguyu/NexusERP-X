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

    // Currently ShopScope is typically intercepted in similar manner 
    // to DataScope (or combined). This sets a basic skeleton.

    @Before("@annotation(shopScopeAnnotation)")
    public void doBefore(JoinPoint point, ShopScope shopScopeAnnotation) {
        // Implementation similar to DataScope interceptor but for sys_role_shop
        // Future extensions will leverage DataScopeContext adding a setShopIds([])
    }

    @After("@annotation(shopScopeAnnotation)")
    public void doAfter(JoinPoint point, ShopScope shopScopeAnnotation) {
    }
}
