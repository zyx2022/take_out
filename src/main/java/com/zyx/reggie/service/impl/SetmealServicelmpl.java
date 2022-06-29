package com.zyx.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyx.reggie.common.CustomException;
import com.zyx.reggie.dto.DishDto;
import com.zyx.reggie.dto.SetmealDto;
import com.zyx.reggie.entity.Setmeal;
import com.zyx.reggie.entity.SetmealDish;
import com.zyx.reggie.mapper.SetmealMapper;
import com.zyx.reggie.service.SetmealDishService;
import com.zyx.reggie.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServicelmpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 保存套餐的基本信息
     * 保存套餐和菜品的关联信息
     * @param setmealDto
     */
    @Override
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，操作setmeal，执行insert操作
        super.save(setmealDto);

        Long setmealId = setmealDto.getId();
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        setmealDishes = setmealDishes.stream().map((item) ->{
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        //保存套餐和菜品的关联信息
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 套餐批量删除
     * 注意：首先需要查询套餐状态，确定是否可以删除；其次再去删除套餐基本信息和关联菜品信息，即套餐表setmeal和套餐关系表setmeal_dish两张表中对应的数据记录
     * select count(*) from setmeal where ids in(1,2,3) and status = 1
     * @param ids
     */
    @Override
    public void deleteWithDish(List<Long> ids) {
        if (ids == null || ids.size() == 0) {
            throw new CustomException("传入的数据不合法，请重试");
        }

        //对于状态为售卖中的套餐不能删除，需要先停售，然后才能删除
        LambdaQueryWrapper<Setmeal> setmeallambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmeallambdaQueryWrapper.in(ids != null, Setmeal::getId,ids);
        setmeallambdaQueryWrapper.eq(Setmeal::getStatus, 1);
        int count = super.count(setmeallambdaQueryWrapper);
        if (count > 0){
            //如果不能删除，抛出一个业务异常
            throw new CustomException("有套餐正处于售卖状态，请停售后，再次进行删除");
        }

        //如果可以删除，先删除套餐基本信息
        super.removeByIds(ids);
        //再删除套餐关联菜品信息
        LambdaQueryWrapper<SetmealDish> setmealDishlambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishlambdaQueryWrapper.in(ids != null, SetmealDish::getSetmealId, ids);
        setmealDishService.remove(setmealDishlambdaQueryWrapper);
    }

    /**
     * 套餐批量起售与停售
     * @param status
     * @param ids
     */
    @Override
    public void updateByIds(Integer status, List<Long> ids) {
        if (ids == null || ids.size() == 0){
            throw new CustomException("传入的数据不合法，请重试");
        }

        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.in(Setmeal::getId, ids);
        List<Setmeal> setmealList = super.list(setmealLambdaQueryWrapper);

        for (Setmeal setmeal : setmealList){
            if (setmeal != null && setmeal.getStatus() != status){
                setmeal.setStatus(status);
            }
        }
        if (setmealList != null){
            super.updateBatchById(setmealList);
        }
    }

    /**
     * 根据id查询套餐信息
     * @param id
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {
        if (id == null){
            throw new CustomException("传入的数据不合法，请重试");
        }

        //查询原有套餐setmeal基本信息，并拷贝到setmealDto
        Setmeal setmeal = super.getById(id);
        if (setmeal != null){
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(setmeal, setmealDto);

            //查询套餐关联菜品信息，并赋值到setmealDto
            LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
            setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
            List<SetmealDish> setmealDishList = setmealDishService.list(setmealDishLambdaQueryWrapper);
            setmealDto.setSetmealDishes(setmealDishList);
            return setmealDto;
        }
        return null;
    }

    /**
     * 修改套餐信息
     * 包含套餐基本信息和套餐关联菜品信息
     * @param setmealDto
     */
    @Override
    public void updateWithDish(SetmealDto setmealDto) {
        if (setmealDto == null){
            throw new CustomException("传入的数据不合法，请重试");
        }
        List<SetmealDish> setmealDishList = setmealDto.getSetmealDishes();
        if (setmealDishList == null){
            throw new CustomException("套餐中没有菜品，请添加菜品到套餐中");
        }

        //点击修改后的保存，后端会接收到下面的数据：发现setmealId == null，所以这里需要自己单独填充
        Long setmealId = setmealDto.getId();
        for (SetmealDish setmealDish : setmealDishList){
            setmealDish.setSetmealId(setmealId);
        }

        //为了不把问题复杂化，先把相关的setmealDish内容移除然后再重新添加，这样就可以不用考虑dish重复的问题和哪些修改哪些没修改
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, setmealId);
        setmealDishService.remove(setmealDishLambdaQueryWrapper);

        //更新套餐的基本信息到套餐表
        super.updateById(setmealDto);
        //更新套餐关联的菜品基本信息到套餐菜品表
        setmealDishService.saveBatch(setmealDishList);
    }

}
