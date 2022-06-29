package com.zyx.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyx.reggie.dto.DishDto;
import com.zyx.reggie.entity.Dish;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface DishService extends IService<Dish> {
    public void saveWithFlavor(DishDto dishDto);
    public DishDto getByIdWithFlavor(Long id);
    public void updateWithFlavor(DishDto dishDto);
    public void updateByIds(Integer status, List<Long> ids);
    public void deleteByIds(List<Long> ids);
}
