package com.fungame.songquiz.support.logging;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class LogContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        Map<String, String> submitted = MDC.getCopyOfContextMap();

        return () -> {
            Map<String, String> runnerOwn = MDC.getCopyOfContextMap();
            replaceWith(submitted);
            try {
                task.run();
            } finally {
                replaceWith(runnerOwn);
            }
        };
    }

    private static void replaceWith(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
            return;
        }

        MDC.setContextMap(context);
    }
}
