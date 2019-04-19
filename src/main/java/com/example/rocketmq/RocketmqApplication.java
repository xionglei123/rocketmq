package com.example.rocketmq;

import com.example.rocketmq.autoconfig.RocketMQAutoConfiguration;
import com.example.rocketmq.service.RocketMQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class RocketmqApplication {

    @Autowired
    private static RocketMQService rocketMQAutoConfiguration;

    public static void main(String[] args) {

        log.info("开始。。。。。");
        SpringApplication.run(RocketmqApplication.class, args);
        log.info(rocketMQAutoConfiguration.toString());
    }

}
