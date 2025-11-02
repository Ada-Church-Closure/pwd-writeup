package com.comment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.comment.dto.LoginFormDTO;
import com.comment.dto.Result;
import com.comment.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ada
 * @since 2021-11-02
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();
}
