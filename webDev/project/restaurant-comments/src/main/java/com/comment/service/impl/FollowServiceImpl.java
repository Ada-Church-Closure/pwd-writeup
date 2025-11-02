package com.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.comment.dto.Result;
import com.comment.dto.UserDTO;
import com.comment.entity.Follow;
import com.comment.mapper.FollowMapper;
import com.comment.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.comment.service.IUserService;
import com.comment.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.comment.utils.RedisConstants.FOLLOWS_KEY;

/**
 * @author ada
 * @since 2025-11-1
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;


    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();

        // 是关注还是取关
        if(isFollow){
            // 数据库中添加这样一条记录
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            // 把当前用户关注的所有博主放到一个redis set内部
            if(isSuccess){
                stringRedisTemplate.opsForSet().add(FOLLOWS_KEY + userId, followUserId.toString());
            }
        }else{
            // 取关,删除mysql中这样的一条记录
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            // 取关同样在redis内部删除
            if(isSuccess) {
                stringRedisTemplate.opsForSet().remove(FOLLOWS_KEY + userId, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        // 获取是否有记录即可.
        Integer count = query()
                .eq("user_id", userId)
                .eq("follow_user_id", followUserId)
                .count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        Long userId = UserHolder.getUser().getId();
        String myKey = FOLLOWS_KEY + userId;
        String targetKey = FOLLOWS_KEY + id;
        // 求交集
        Set<String> intersectFollows = stringRedisTemplate.opsForSet().intersect(myKey, targetKey);

        if(intersectFollows == null || intersectFollows.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = intersectFollows.stream().map(Long::valueOf).collect(Collectors.toList());
        // 查询交集的关注
        List<UserDTO> userDTOList = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOList);
    }
}
