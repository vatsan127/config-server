package dev.srivatsan.config_server.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.UUID;

@Aspect
@Slf4j
@Component
public class AspectService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Pointcut for all service layer methods
    private static final String SERVICE_LAYER_POINTCUT = 
        "execution(* dev.srivatsan.config_server.service.*.*.*(..))";

    // Pointcut for all controller layer methods
    private static final String CONTROLLER_LAYER_POINTCUT =
            "execution(* dev.srivatsan.config_server.controller.*.*(..))";

    @Before(CONTROLLER_LAYER_POINTCUT)
    public void logControllerMethodEntry(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        // Generate request ID ONLY at controller level
        String requestId = generateRequestId();
        RequestContext.setRequestId(requestId);
        
        log.info("{} - ENTRY | {} | RequestId: {} | Args: {}", className, methodName, requestId, formatArguments(joinPoint.getArgs()));
    }

    @Before(SERVICE_LAYER_POINTCUT)
    public void logServiceMethodEntry(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        // Use existing request ID from controller
        String requestId = RequestContext.getRequestId();
        
        log.info("{} - ENTRY | {} | RequestId: {} | Args: {}", className, methodName, requestId, formatArguments(joinPoint.getArgs()));
    }

    @Around(SERVICE_LAYER_POINTCUT)
    public Object measureServiceMethodPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String requestId = RequestContext.getRequestId();

        try {
            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            
            // Exit logging with execution time, result, and request ID
            log.info("{} - EXIT | {} | RequestId: {} | {}ms | Result: {}", className, methodName, requestId, executionTime, formatResult(result));

            return result;
        } catch (Throwable throwable) {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            
            // Exception logging with request ID
            log.error("{} - EXCEPTION | {} | RequestId: {} | {}ms | Exception: {}", className, methodName, requestId, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    @Around(CONTROLLER_LAYER_POINTCUT)
    public Object measureControllerMethodPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String requestId = RequestContext.getRequestId();

        try {
            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            
            // Exit logging with execution time, result, and request ID
            log.info("{} - EXIT | {} | RequestId: {} | {}ms | Result: {}", className, methodName, requestId, executionTime, formatResult(result));

            return result;
        } catch (Throwable throwable) {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            
            // Exception logging with request ID
            log.error("{} - EXCEPTION | {} | RequestId: {} | {}ms | Exception: {}", className, methodName, requestId, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    // Utility methods
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        try {
            String resultStr = objectMapper.writeValueAsString(result);
            if (resultStr.length() > 1000) {
                return resultStr.substring(0, 1000) + "... [truncated]";
            }
            return resultStr;
        } catch (Exception e) {
            return result.toString();
        }
    }

    private static class RequestContext {
        private static final ThreadLocal<String> requestIdHolder = new ThreadLocal<>();

        public static void setRequestId(String requestId) {
            requestIdHolder.set(requestId);
        }

        public static String getRequestId() {
            return requestIdHolder.get();
        }

        public static void clear() {
            requestIdHolder.remove();
        }
    }
}