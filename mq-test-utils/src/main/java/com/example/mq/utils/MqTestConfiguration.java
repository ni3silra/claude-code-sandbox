package com.example.mq.utils;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MqProperties.class)
@ComponentScan(basePackages = "com.example.mq.utils")
public class MqTestConfiguration {
}