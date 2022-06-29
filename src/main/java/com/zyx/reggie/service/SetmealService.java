package com.zyx.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyx.reggie.dto.SetmealDto;
import com.zyx.reggie.entity.Setmeal;
import org.springframework.stereotype.Service;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    public void saveWithDish(SetmealDto setmealDto);
    public void deleteWithDish(List<Long> ids);
    public void updateByIds(Integer status, List<Long> ids);
    public SetmealDto getByIdWithDish(Long id);
    public void updateWithDish(SetmealDto setmealDto);
}
