package com.example.rocketmq;

import com.example.rocketmq.autoconfig.RocketMQAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.LinkOption;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RocketmqApplication.class)
@Slf4j
public class RocketmqApplicationTests {

    @Autowired
    RocketMQAutoConfiguration rocketMQAutoConfiguration;
    @Test
    public void contextLoads() {
        log.info(rocketMQAutoConfiguration.toString());
    }

}
