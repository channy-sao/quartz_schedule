package com.example.schedule.config;


import org.quartz.spi.JobFactory;

import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzSchedulerConfiguration {

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(
            JobFactory jobFactory) {

        return factory -> factory.setJobFactory(jobFactory);
    }
}