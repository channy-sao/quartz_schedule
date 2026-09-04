package com.example.schedule.config;

import com.example.schedule.listener.QuartzMisfireListener;
import org.quartz.spi.JobFactory;

import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzSchedulerConfiguration {

  @Bean
  public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(
      JobFactory jobFactory, QuartzMisfireListener misfireListener) {
    return factory -> {
      factory.setGlobalTriggerListeners(misfireListener);
      factory.setJobFactory(jobFactory);
    };
  }
}
