package com.example.rocketmq.controller;

import com.example.rocketmq.service.RocketMQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class RocketMqController {
    @Autowired
    private RocketMQService rocketMQService;

    @GetMapping("/test")
    public void test(){
        rocketMQService.test();
    }
}
