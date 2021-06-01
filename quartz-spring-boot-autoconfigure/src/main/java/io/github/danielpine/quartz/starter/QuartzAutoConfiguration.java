package io.github.danielpine.quartz.starter;


import io.github.danielpine.quartz.starter.annotation.QuartzTrigger;
import org.quartz.*;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnProperty(prefix = "quartz", value = "enabled", havingValue = "true")
public class QuartzAutoConfiguration {
    // 配置文件路径
    private static final String QUARTZ_CONFIG = "/quartz.properties";

    @Resource
    @Qualifier(value = "dataSource")
    private DataSource dataSource;

    @Resource
    private List<Job> jobs;

    /**
     * 从quartz.properties文件中读取Quartz配置属性
     *
     * @return
     * @throws IOException
     */
    @Bean
    public Properties quartzProperties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource(QUARTZ_CONFIG));
        propertiesFactoryBean.afterPropertiesSet();
        return propertiesFactoryBean.getObject();
    }

    /**
     * JobFactory与schedulerFactoryBean中的JobFactory相互依赖,注意bean的名称
     * 在这里为JobFactory注入了Spring上下文
     *
     * @param applicationContext
     * @return
     */
    @Bean
    public JobFactory buttonJobFactory(ApplicationContext applicationContext) {
        AutoWiredSpringBeanToJobFactory jobFactory = new AutoWiredSpringBeanToJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    /**
     * @param buttonJobFactory 为SchedulerFactory配置JobFactory
     * @return
     * @throws IOException
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(JobFactory buttonJobFactory) throws IOException, SchedulerException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(true); // 设置自行启动
        factory.setQuartzProperties(quartzProperties());
        factory.setDataSource(dataSource);// 使用应用的dataSource替换quartz的dataSource
        factory.setJobFactory(buttonJobFactory);
        return factory;
    }

    @Bean(name = "scheduler")
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        scheduler.clear();
        jobs.forEach(job -> {
            try {
                Class<? extends Job> jobClass = job.getClass();
                QuartzTrigger trigger = jobClass.getAnnotation(QuartzTrigger.class);
                if (Objects.nonNull(trigger)) {
                    JobDetail jobDetail = JobBuilder
                            .newJob(jobClass)
                            .withIdentity(trigger.name(), trigger.group())
                            .storeDurably()
                            .build();
                    jobDetail.getKey().getName();
                    CronTrigger cronTrigger = TriggerBuilder
                            .newTrigger()
                            .withIdentity(trigger.name(), trigger.group())
                            .withSchedule(CronScheduleBuilder.cronSchedule(trigger.cron()))
                            .build();
                    scheduler.scheduleJob(jobDetail, cronTrigger);
                    System.out.println("Scheduled Job " + jobClass.getName() + " with " + trigger.cron());
                }
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        });
        return scheduler;
    }

}
