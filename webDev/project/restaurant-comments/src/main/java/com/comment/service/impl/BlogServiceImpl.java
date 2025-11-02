package com.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comment.dto.Result;
import com.comment.dto.ScrollResult;
import com.comment.dto.UserDTO;
import com.comment.entity.Blog;
import com.comment.entity.Follow;
import com.comment.entity.User;
import com.comment.mapper.BlogMapper;
import com.comment.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.comment.service.IFollowService;
import com.comment.service.IUserService;
import com.comment.utils.SystemConstants;
import com.comment.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.comment.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.comment.utils.RedisConstants.FEED_KEY;

/**
 *  服务实现类
 *
 * @author ada
 * @since 2025-10-31
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        // 查询blog和相关的用户
        Blog blog = getById(id);
        if(blog == null){
            return  Result.fail("あっかりん〜");
        }

        queryBlogUser(blog);
        // 查询当前用户是否给这个blog点赞了
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 这是前端来判断是否进行高亮显示
     * @param blog  访问的博客
     */
    private void isBlogLiked(Blog blog) {
        // 如果发现用户没有登陆,直接返回,并且要求登陆
        UserDTO user = UserHolder.getUser();
        if(user == null){
            blog.setIsLike(false);
            return;
        }
        // 1.获取登陆的用户id
        Long userId = UserHolder.getUser().getId();
        // 2.我们判断这个用户是否已经点赞了
        Double score = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + blog.getId(), userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {
        // 1.获取登陆的用户id
        Long userId = UserHolder.getUser().getId();
        // 2.我们判断这个用户是否已经点赞了
        Double score = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + id, userId.toString());
        // 也就是这个score不存在,就是没有点赞
        if(score == null){
            // 没有点赞,点赞数 + 1,并且加入这个blog key对应的set
            // 反之则反
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 改变成功,保存到redis这个blog对应的sorted set集合
            if(isSuccess){
                stringRedisTemplate.opsForZSet().add(BLOG_LIKED_KEY + id, userId.toString(), System.currentTimeMillis());
            }
        }else {
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if(isSuccess){
                stringRedisTemplate.opsForZSet().remove(BLOG_LIKED_KEY + id, userId.toString());
            }
        }

        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        // 1.查询Top5的点赞的用户
        Set<String> top5UserId = stringRedisTemplate.opsForZSet().range(BLOG_LIKED_KEY + id, 0, 4);
        if(top5UserId == null || top5UserId.isEmpty()){
            // 有可能没有人点赞,返回空set
            return Result.ok(Collections.emptyList());
        }
        // 2.返回一个集合,我们提取出来用户的id
        // assert top5UserId != null;
        List<Long> ids = top5UserId.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        // 3.用这些id查询出来对应的user
        // 这里sql查询的问题 in(5, 1) 也会从1开始查询
        // 利用 ORDER BY FILED (id, 5, 1) 来进行查询
        // 好复杂
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")")
                .list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店blog
        boolean isSuccess = save(blog);
        // 插入笔记之后,要发送给所有的粉丝.
        if(!isSuccess){
            return Result.fail("新增笔记失败!");
        }
        // 查询我们现在有的全部粉丝
        List<Follow> followers = followService.query().eq("follow_user_id", user.getId()).list();
        // 给所有粉丝进行推送
        for(Follow follow : followers){
            // 拿到粉丝的Id
            Long userId = follow.getUserId();
            // 推送blog的id进去
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
            return Result.ok(blog.getId());
        }


        // 返回id
        return Result.ok(blog.getId());
    }

    // TODO 查询没有被正确的触发,前端的问题?
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1.获取当前的用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询收件箱 ZREVRANGE key Max Min LIMIT offset count
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 5);

        // 3.查询到的blogs要非空
        if(typedTuples == null || typedTuples.isEmpty()){
            return Result.ok();
        }
        // 4.遍历数据进行解析
        long minTime = 0;
        int sameTimeOffset = 1;
        List<Long> ids = new ArrayList<>(typedTuples.size());

        for(ZSetOperations.TypedTuple<String> tuple : typedTuples){
            // 拿到blog id 并且收集
            ids.add(Long.valueOf(tuple.getValue()));
            // 拿到对应的时间戳
            long currTime = tuple.getScore().longValue();
            if(currTime == minTime){
                ++sameTimeOffset;
            }else{
                minTime = currTime;
                sameTimeOffset = 1;
            }
        }

        String idStr = StrUtil.join(",", ids);
        // 根据这些id查询blog
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        for(Blog blog : blogs){
            queryBlogUser(blog);
            isBlogLiked(blog);
        }

        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(sameTimeOffset);
        r.setMinTime(minTime);

        return Result.ok(r);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
