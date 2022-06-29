package com.zyx.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyx.reggie.dto.DishDto;
import com.zyx.reggie.entity.DishFlavor;
import com.zyx.reggie.mapper.DishFlavorMapper;
import com.zyx.reggie.service.DishFlavorService;
import com.zyx.reggie.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PutMapping;

@Service
public class DishFlavorServicelmpl extends ServiceImpl<DishFlavorMapper, DishFlavor> implements DishFlavorService {

}
