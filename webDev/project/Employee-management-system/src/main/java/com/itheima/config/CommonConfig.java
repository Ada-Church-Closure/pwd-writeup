package com.itheima.config;

import org.jdom2.output.StAXStreamReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 配置类
public class CommonConfig {
    // 声明一个第三方的Bean对象
    @Bean   // 交给IOC容器管理
    public StAXStreamReader stAXStreamReader(){
        return new StAXStreamReader();
    }
}
