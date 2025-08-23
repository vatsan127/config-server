package dev.srivatsan.config_server.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.srivatsan.config_server.service.util.UtilService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;

@Aspect
@Slf4j
@Component
public class AspectService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SERVICE_LAYER_POINTCUT = "execution(* dev.srivatsan.config_server.service.*.*.*(..))";
    private static final String CONTROLLER_LAYER_POINTCUT = "execution(* dev.srivatsan.config_server.controller.*.*(..))";

    private final UtilService utilService;

    public AspectService(UtilService utilService) {
        this.utilService = utilService;
    }

    @Before(CONTROLLER_LAYER_POINTCUT)
    public void logControllerMethodEntry(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        String requestId = utilService.generateRequestId();
        RequestContext.setRequestId(requestId);

        log.info("{} - ENTRY | {} | RequestId: {} | Args: {}", className, methodName, requestId, formatArguments(joinPoint.getArgs()));
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

            String resultStr = result == null ? "null" : objectMapper.writeValueAsString(result);
            log.info("{} - EXIT | {} | RequestId: {} | {}ms | Result: {}", className, methodName, requestId, executionTime, resultStr);

            return result;
        } catch (Throwable throwable) {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            log.error("{} - EXCEPTION | {} | RequestId: {} | {}ms | Exception: {}", className, methodName, requestId, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    @Before(SERVICE_LAYER_POINTCUT)
    public void logServiceMethodEntry(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

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

            String resultStr = result == null ? "null" : objectMapper.writeValueAsString(result);
            log.info("{} - EXIT | {} | RequestId: {} | {}ms | Result: {}", className, methodName, requestId, executionTime, resultStr);

            return result;
        } catch (Throwable throwable) {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            log.error("{} - EXCEPTION | {} | RequestId: {} | {}ms | Exception: {}", className, methodName, requestId, executionTime, throwable.getMessage());
            throw throwable;
        }
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

    private static class RequestContext {

        private static final ThreadLocal<String> requestIdHolder = new ThreadLocal<>();

        public static void setRequestId(String requestId) {
            requestIdHolder.set(requestId);
        }

        public static String getRequestId() {
            return requestIdHolder.get();
        }

    }
}