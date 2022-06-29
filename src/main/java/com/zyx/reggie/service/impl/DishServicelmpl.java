package com.zyx.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyx.reggie.common.CustomException;
import com.zyx.reggie.common.R;
import com.zyx.reggie.dto.DishDto;
import com.zyx.reggie.entity.Dish;
import com.zyx.reggie.entity.DishFlavor;
import com.zyx.reggie.mapper.DishMapper;
import com.zyx.reggie.service.DishFlavorService;
import com.zyx.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DishServicelmpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    DishFlavorService dishFlavorService;

    /**
     * 做两件事：一、保存菜品的基本信息到菜品表；二、保存菜品口味到菜品数据表
     * @param dishDto
     * 在保存数据到菜品表和菜品口味表的过程中，我们需要对保存到菜品口味表的数据做相应的处理
     * 取出dishDto的dishId，通过stream流对每一组flavor的dishId赋值
     */
    @Override
    public void saveWithFlavor(DishDto dishDto) {
        //一、保存菜品的基本信息到菜品表
        super.save(dishDto);

        //二、保存菜品口味到菜品数据表
        //获取菜品id
        Long dishId = dishDto.getId();
        //获取菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();

        /**
         * 将每条flavors的dishId赋值，将菜品id和菜品口味两者关联（绑定）在一起
         * 这是一种批量修改List集合里面某个属性的方法
         */
        //方法一：通过流的方式
//        flavors = flavors.stream().map((item) -> {
//            item.setDishId(dishId);
//            return item;
//        }).collect(Collectors.toList());

//        方法二：通过forEach
        flavors.forEach(item -> item.setDishId(dishId));

        //保存菜品口味数据到菜品口味表
        dishFlavorService.saveBatch(flavors);//前端：flavors: [{name: "甜味", value: "["无糖","少糖","半糖","多糖","全糖"]", showOption: false},…]
    }

    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //根据服务端接收的id，查询菜品的基本信息dish
        Dish dish = super.getById(id);

        //创建dishDto对象，并将查询到的dish对象属性赋值给dishDto
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        //创建dishDto对象，可以取出对应的菜品id，再通过等值条件查询，查询到DishFlavor数据信息
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dish.getId() != null, DishFlavor::getDishId, dish.getId());
        //DishFlavor dishFlavor = dishFlavorService.getOne(lambdaQueryWrapper);
        //上述写法是错误的，因为dishFlavor不是一个值，可能有多个值，是一个List集合，正确写法如下：
        List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);





        //将查询到的flavor数据信息使用set方法赋值给dishDto对象
        dishDto.setFlavors(dishFlavorList);

        return dishDto;
    }

    /**
     * 修改菜品
     * 注意：不仅需要修改菜品基本信息，还需修改菜品口味
     * @param dishDto
     */
    @Override
    public void updateWithFlavor(DishDto dishDto) {
        //一、根据id更新菜品的基本信息到菜品表
        super.updateById(dishDto);

        //二、更新菜品口味到菜品数据表dish_flavor
        //通过dish_id,删除菜品的旧flavor数据
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dishDto.getId() != null, DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(lambdaQueryWrapper);
        /**
         * 注意：这里是通过dishDto.getId()找到dish_flavor表中dish_id字段对应的那一条记录，
         * 在java中，也就是用lambdaQueryWrapper条件查询将其封装成一个对象
         * 利用dishFlavorService.remove()方法进行对应旧flavor数据的删除
         *
         * 而像下面这种做法，拿到dishId，
         * dishFlavorService.removeById(Integer id)形参id应该是dish_flavor表中id字段，而不是dish_id
         * 所以对旧flavor数据的删除肯定失败
         */
//        Long dishId = dishDto.getId();
//        dishFlavorService.removeById(dishId);

        //获取前端提交的新flavor数据
        List<DishFlavor> flavors = dishDto.getFlavors();

        /**
         * 将每条flavors的dishId赋值，将菜品id和菜品口味两者关联（绑定）在一起
         * 这是一种批量修改List集合里面某个属性的方法
         */
        //方法一：通过流的方式
//        flavors = flavors.stream().map((item) -> {
//            item.setDishId(dishDto.getId());
//            return item;
//        }).collect(Collectors.toList());

//        方法二：通过forEach
        flavors.forEach(item -> item.setDishId(dishDto.getId()));

        //保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);
    }


    /**
     * 菜品批量启售和批量停售
     * @param status
     * @param ids
     * @return
     */
    public void updateByIds(Integer status, List<Long> ids) {
        if (status == null || ids == null || ids.size() == 0) {
            throw new CustomException("传入的数据不合法，请重试");
        }

        //根据id查询需要批量启售和批量停售的菜品列表
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.in(Dish::getId, ids);
        List<Dish> dishList = super.list(dishLambdaQueryWrapper);

        //批量为菜品设置状态并更新到数据库中
        for (Dish dish : dishList){
            if (dish != null && dish.getStatus() != status){
                dish.setStatus(status);
            }
        }
        if (dishList != null){
            super.updateBatchById(dishList);
        }
    }

    /**
     * 菜品批量删除
     * 注意：首先需要查询菜品状态，确定是否可以删除；其次再去删除菜品基本信息和菜品口味信息，即菜品表dish和套餐关系表dish_flavor两张表中对应的数据记录
     * @param ids
     * @return
     */
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.size() == 0) {
            throw new CustomException("传入的数据不合法，请重试");
        }

        //根据id查询，对于状态为售卖中的菜品不能删除，需要先停售，然后才能删除
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.in(Dish::getId, ids);
        dishLambdaQueryWrapper.eq(Dish::getStatus, 1);
        int count = super.count(dishLambdaQueryWrapper);
        if (count > 0){
            throw new CustomException("有菜品正处于售卖状态，请停售后，再次进行删除");
        }

        //如果可以删除，先批量删除菜品基本信息
        super.removeByIds(ids);
        //批量删除菜品关联口味信息
        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(dishFlavorLambdaQueryWrapper);
    }
}
