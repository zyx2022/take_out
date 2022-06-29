package com.zyx.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyx.reggie.entity.OrderDetail;
import com.zyx.reggie.mapper.OrderDetailMapper;
import com.zyx.reggie.service.OrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailServicelmpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
}
