package com.comment.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private LocalDateTime expireTime;
    // 不用对源代码做修改
    private Object data;
}
