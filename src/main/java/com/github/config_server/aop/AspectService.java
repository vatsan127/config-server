package com.github.config_server.aop;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.config_server.service.util.UtilService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;

@Aspect
@Component
public class AspectService {

    private final Logger log = LoggerFactory.getLogger(AspectService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UtilService utilService;

    private static final String COMBINED_POINTCUT =
            "execution(* com.github.config_server.service.*.*.*(..)) || " +
                    "execution(* com.github.config_server.controller.*.*(..))";

    public AspectService(UtilService utilService) {
        this.utilService = utilService;
    }

    @Around(COMBINED_POINTCUT)
    public Object logAndMeasureMethodPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        boolean isController = isControllerMethod(joinPoint);
        String requestId = getOrSetRequestId(isController);

        if (isController) {
            log.info("{}:{} - ENTRY | RequestId: {} | Args: {}", className, methodName, requestId, formatArguments(joinPoint.getArgs()));
        } else {
            log.debug("{}:{} - ENTRY | RequestId: {} | Args: {}", className, methodName, requestId, formatArguments(joinPoint.getArgs()));
        }

        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            String resultStr = formatResult(result);

            if (isController) {
                log.info("{}:{} - EXIT | RequestId: {} | {}ms", className, methodName, requestId, executionTime);
            } else {
                log.debug("{}:{} - EXIT | RequestId: {} | {}ms | Result: {}", className, methodName, requestId, executionTime, resultStr);
            }

            return result;
        } catch (Throwable throwable) {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();

            log.error("{} - EXCEPTION | {} | RequestId: {} | {}ms | Exception: {}",
                    className, methodName, requestId, executionTime, throwable.getMessage());
            throw throwable;
        } finally {
            if (isController) {
                RequestContext.clear();
            }
        }
    }

    private boolean isControllerMethod(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getName();
        return className.contains(".controller.");
    }

    private String getOrSetRequestId(boolean isController) {
        if (isController) {
            String requestId = utilService.generateRequestId();
            RequestContext.setRequestId(requestId);
            return requestId;
        }
        return RequestContext.getRequestId();
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return result.toString();
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

        public static void clear() {
            requestIdHolder.remove();
        }
    }
}
