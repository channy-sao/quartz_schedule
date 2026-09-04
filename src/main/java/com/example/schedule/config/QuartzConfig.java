package com.example.schedule.config;

import org.quartz.spi.JobFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

  @Bean
  public JobFactory jobFactory(AutowiringJobFactory autowiringJobFactory) {
    return autowiringJobFactory;
  }
}
