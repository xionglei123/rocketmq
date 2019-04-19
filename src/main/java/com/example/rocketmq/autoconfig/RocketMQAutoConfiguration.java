package com.example.rocketmq.autoconfig;

import com.zeyiyouhuo.framework.rocketmq.annotation.RocketMQMessageListener;
import com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainer;
import com.zeyiyouhuo.framework.rocketmq.core.RocketMQListener;
import com.zeyiyouhuo.framework.rocketmq.core.RocketMQSerializer;
import com.zeyiyouhuo.framework.rocketmq.core.RocketMQTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.METHOD_DESTROY;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_CONSUMER_GROUP;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_CONSUME_MESSAGE_BATCH_MAX_SIZE;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_CONSUME_MODE;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_CONSUME_THREAD_MAX;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_CONSUME_THREAD_MIN;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_HEART_BEAT_BROKER_INTERVAL;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_MESSAGE_MODEL;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_NAMESERVER;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_POLL_NAME_SERVER_INTERVAL;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_PULL_BATCH_SIZE;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_ROCKETMQ_LISTENER;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_SELECTOR_EXPRESS;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_SELECTOR_TYPE;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_SERIALIZER;
import static com.zeyiyouhuo.framework.rocketmq.core.DefaultRocketMQListenerContainerConstants.PROP_TOPIC;


/**
 * RocketMQ 自动装载
 */
@Configuration
@EnableConfigurationProperties(RocketMQProperties.class)
@ConditionalOnClass(MQClientAPIImpl.class)
@Slf4j
@Order
public class RocketMQAutoConfiguration {

    @Autowired(required = false)
    private RocketMQSerializer serializer;

    @Bean
    @ConditionalOnClass(DefaultMQProducer.class)
    @ConditionalOnMissingBean(DefaultMQProducer.class)
    @ConditionalOnProperty(prefix = "spring.rocketmq", value = {"nameServer", "producer.group"})
    public DefaultMQProducer mqProducer(RocketMQProperties rocketMQProperties) {
        RocketMQProperties.Producer producerConfig = rocketMQProperties.getProducer();
        log.info("rocketMQ开始装载了。。。");
        String groupName = producerConfig.getGroup();
        Assert.hasText(groupName, "[spring.rocketmq.producer.group] must not be null");
        DefaultMQProducer producer = new DefaultMQProducer(producerConfig.getGroup());
        producer.setNamesrvAddr(rocketMQProperties.getNameServer());
        producer.setSendMsgTimeout(producerConfig.getSendMsgTimeout());
        producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(producerConfig.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(producerConfig.getMaxMessageSize());
        producer.setVipChannelEnabled(false);
        producer.setCompressMsgBodyOverHowmuch(producerConfig.getCompressMsgBodyOverHowmuch());
        producer.setRetryAnotherBrokerWhenNotStoreOK(producerConfig.isRetryAnotherBrokerWhenNotStoreOk());
        return producer;
    }


    @Bean
    @ConditionalOnBean(DefaultMQProducer.class)
    @ConditionalOnMissingBean
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer mqProducer) {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setProducer(mqProducer);
        Optional.ofNullable(serializer).ifPresent(s -> rocketMQTemplate.setSerializer(s));
        return rocketMQTemplate;
    }

    @Configuration
    @ConditionalOnClass(DefaultMQPushConsumer.class)
    @EnableConfigurationProperties(RocketMQProperties.class)
    @ConditionalOnProperty(prefix = "spring.rocketmq", value = "nameServer")
    @Order
    public static class ListenerContainerConfiguration implements ApplicationContextAware, InitializingBean, EnvironmentAware {

        private ConfigurableApplicationContext applicationContext;

        private AtomicLong counter = new AtomicLong(0);

        private StandardEnvironment environment;

        @Autowired
        private RocketMQProperties rocketMQProperties;

        @Autowired(required = false)
        private RocketMQSerializer serializer;

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        }


        private void registerContainer(String beanName, Object bean) {
            Class<?> clazz = AopUtils.getTargetClass(bean);
            if (!RocketMQListener.class.isAssignableFrom(bean.getClass())) {
                throw new IllegalStateException(clazz + " is not instance of " + RocketMQListener.class.getName());
            }
            RocketMQListener rocketMQListener = (RocketMQListener) bean;
            RocketMQMessageListener annotation = clazz.getAnnotation(RocketMQMessageListener.class);
            BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.rootBeanDefinition(DefaultRocketMQListenerContainer.class);
            String nameServer = StringUtils.isNotEmpty(annotation.nameServer()) ? environment.resolvePlaceholders(annotation.nameServer()) : rocketMQProperties.getNameServer();
            beanBuilder.addPropertyValue(PROP_NAMESERVER, nameServer);
            beanBuilder.addPropertyValue(PROP_TOPIC, environment.resolvePlaceholders(annotation.topic()));
            beanBuilder.addPropertyValue(PROP_CONSUMER_GROUP, environment.resolvePlaceholders(annotation.consumerGroup()));
            beanBuilder.addPropertyValue(PROP_CONSUME_MODE, annotation.consumeMode());
            beanBuilder.addPropertyValue(PROP_CONSUME_THREAD_MAX, Integer.valueOf(environment.resolvePlaceholders(annotation.consumeThreadMax())));
            beanBuilder.addPropertyValue(PROP_CONSUME_THREAD_MIN, Integer.valueOf(environment.resolvePlaceholders(annotation.consumeThreadMin())));
            beanBuilder.addPropertyValue(PROP_MESSAGE_MODEL, annotation.messageModel());
            beanBuilder.addPropertyValue(PROP_SELECTOR_EXPRESS, environment.resolvePlaceholders(annotation.selectorExpress()));
            beanBuilder.addPropertyValue(PROP_SELECTOR_TYPE, annotation.selectorType());
            beanBuilder.addPropertyValue(PROP_ROCKETMQ_LISTENER, rocketMQListener);
            beanBuilder.addPropertyValue(PROP_CONSUME_MESSAGE_BATCH_MAX_SIZE, annotation.consumeMessageBatchMaxSize());
            beanBuilder.addPropertyValue(PROP_PULL_BATCH_SIZE, annotation.pullBatchSize());
            beanBuilder.addPropertyValue(PROP_POLL_NAME_SERVER_INTERVAL, annotation.pollNameServerInterval());
            beanBuilder.addPropertyValue(PROP_HEART_BEAT_BROKER_INTERVAL, annotation.heartbeatBrokerInterval());
            Optional.ofNullable(serializer).ifPresent(s -> beanBuilder.addPropertyValue(PROP_SERIALIZER, s));
            beanBuilder.setDestroyMethodName(METHOD_DESTROY);
            String containerBeanName = String.format("%s_%s", DefaultRocketMQListenerContainer.class.getName(), counter.incrementAndGet());
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            beanFactory.registerBeanDefinition(containerBeanName, beanBuilder.getBeanDefinition());
            DefaultRocketMQListenerContainer container = beanFactory.getBean(containerBeanName, DefaultRocketMQListenerContainer.class);
            if (!container.isStarted()) {
                try {
                    container.start();
                } catch (Exception e) {
                    log.error("started container failed. {}", container, e);
                    throw new RuntimeException(e);
                }
            }
            log.info("register rocketMQ listener to container, listenerBeanName:{}, containerBeanName:{}", beanName, containerBeanName);
        }

        @Override
        public void setEnvironment(Environment environment) {
            if (environment instanceof StandardEnvironment) {
                this.environment = (StandardEnvironment) environment;
            }
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(RocketMQMessageListener.class);
            Optional.ofNullable(beans).ifPresent(b -> b.forEach(this::registerContainer));
        }
    }
}

