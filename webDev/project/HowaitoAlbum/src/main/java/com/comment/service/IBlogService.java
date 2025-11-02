package com.comment.service;

import com.comment.dto.Result;
import com.comment.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ada
 * @since 2025-10-31
 */
public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    /**
     * 对于一个已经登陆的用户,对于某篇blog做点赞的处理
     * @param id    用户正在浏览的blog
     * @return  是否点赞成功
     */
    Result likeBlog(Long id);

    /**
     * 返回对于一个blog点赞的用户列表
     * @param id   用户正在浏览的blog
     * @return     是否点赞成功
     */
    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
