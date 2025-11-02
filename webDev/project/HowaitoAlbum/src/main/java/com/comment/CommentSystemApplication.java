package com.comment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.comment.mapper")
@SpringBootApplication
public class CommentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentSystemApplication.class, args);
    }

}
