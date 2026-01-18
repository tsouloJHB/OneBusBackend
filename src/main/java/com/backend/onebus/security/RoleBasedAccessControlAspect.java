package com.backend.onebus.security;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspect to intercept methods annotated with @RoleBasedAccessControl
 * and enforce role-based authorization
 */
@Aspect
@Component
public class RoleBasedAccessControlAspect {

    @Around("@annotation(com.backend.onebus.security.RoleBasedAccessControl) || @within(com.backend.onebus.security.RoleBasedAccessControl)")
    public Object checkRoleAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get the HTTP request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        
        HttpServletRequest request = attributes.getRequest();
        String userRole = (String) request.getAttribute("userRole");
        
        if (userRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        
        // Get the annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        RoleBasedAccessControl annotation = method.getAnnotation(RoleBasedAccessControl.class);
        if (annotation == null) {
            // Check class-level annotation
            annotation = method.getDeclaringClass().getAnnotation(RoleBasedAccessControl.class);
        }
        
        if (annotation != null) {
            String[] allowedRoles = annotation.allowedRoles();
            
            // Check if user's role is in the allowed roles
            boolean hasAccess = Arrays.asList(allowedRoles).contains(userRole);
            
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access denied. Required roles: " + Arrays.toString(allowedRoles));
            }
        }
        
        // User has access, proceed with the method
        return joinPoint.proceed();
    }
}
