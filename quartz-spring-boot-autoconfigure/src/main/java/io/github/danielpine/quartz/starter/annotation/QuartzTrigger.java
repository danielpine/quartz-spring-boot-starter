package io.github.danielpine.quartz.starter.annotation;


import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public @interface QuartzTrigger {

    String cron() default "";

    String name() default "";

    String group() default "";

}



