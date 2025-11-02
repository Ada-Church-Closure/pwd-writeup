package com.comment.controller;


import com.comment.dto.Result;
import com.comment.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author ada
 * @since 2025-11-1
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 关注博主的请求
     * @param followUserId  要关注的博主
     * @param isFollow      现在是否已经关注
     * @return              是否成功
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow){
        return followService.follow(followUserId, isFollow);
    }

    /**
     * 是否关注了该博主
     * @param followUserId  检查的博主
     * @return              检查是否成功
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);
    }

    /**
     * 检查和感兴趣用户的共同关注列表
     * @param id    现在正在查看的用户
     * @return      检查是否成功
     */
    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id){
        return followService.followCommons(id);
    }

}
