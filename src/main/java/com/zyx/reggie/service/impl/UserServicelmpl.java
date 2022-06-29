package com.zyx.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyx.reggie.entity.User;
import com.zyx.reggie.mapper.UserMapper;
import com.zyx.reggie.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServicelmpl extends ServiceImpl<UserMapper, User> implements UserService {
}
