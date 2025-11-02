package com.comment.service;

import com.comment.dto.Result;
import com.comment.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author ada
 * @since 2025-11-1
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}
