package com.example.rocketmq.service.imp;

import com.example.rocketmq.autoconfig.RocketMQAutoConfiguration;
import com.example.rocketmq.service.RocketMQService;
import com.zeyiyouhuo.framework.rocketmq.core.RocketMQTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RocketMQServiceImpl implements RocketMQService {
    @Autowired(required = true)
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private RocketMQAutoConfiguration rocketMQAutoConfiguration;


    @Override
    public void test() {
        log.info(rocketMQAutoConfiguration.toString());
        log.info(rocketMQTemplate.toString());

    }
}
