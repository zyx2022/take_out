package com.zyx.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyx.reggie.common.CustomException;
import com.zyx.reggie.entity.Category;
import com.zyx.reggie.entity.Dish;
import com.zyx.reggie.entity.Setmeal;
import com.zyx.reggie.mapper.CategoryMapper;
import com.zyx.reggie.service.CategoryService;
import com.zyx.reggie.service.DishService;
import com.zyx.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 逻辑：查看当前要删除的分类id是否与菜品或套餐相关联，若与其中一个关联，则抛出异常
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类，删除之前需要进行判断：一是是否关联菜品，二是是否关联套餐
     * @param id
     */
    @Override
    public void remove(Long id) {
        //是否关联菜品???
        //构造条件构造器
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加Dish查询条件，根据id进行查询
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, id);
        //查询当前分类是否关联菜品，若关联，则抛出一个业务异常
        int countDish = dishService.count(dishLambdaQueryWrapper);
        if (countDish > 0){
            throw new CustomException("当前分类已关联菜品，无法删除");
        }

        //是否关联套餐???
        //构造条件构造器
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加Dish查询条件，根据id进行查询
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, id);
        //查询当前分类是否关联菜品，若关联，则抛出一个业务异常
        int countSetmeal = setmealService.count(setmealLambdaQueryWrapper);
        if (countDish > 0){
            throw new CustomException("当前分类已关联套餐，无法删除");
        }

        super.removeById(id);
    }
}
