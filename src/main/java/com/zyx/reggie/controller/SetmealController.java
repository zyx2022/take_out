package com.zyx.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyx.reggie.common.R;
import com.zyx.reggie.dto.DishDto;
import com.zyx.reggie.dto.SetmealDto;
import com.zyx.reggie.entity.Category;
import com.zyx.reggie.entity.Dish;
import com.zyx.reggie.entity.Setmeal;
import com.zyx.reggie.entity.SetmealDish;
import com.zyx.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;


    /**
     * 套餐信息分页查询
     * 注意：在套餐管理界面，套餐分类字段显示的是categoryId对应的中文，但在数据库里查询到的是categoryId，
     * 因此需要利用categoryId查询到categoryName，并赋值给数据传输对象SetmealDto
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info("数据传输对象setmealDto：{}", setmealDto.toString());

        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    @GetMapping("/page")
    public R<Page> list(int page, int pageSize, String name){
        //构造分页构造器对象
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>(page, pageSize);
        //构造查询条件对象
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件、排序条件
        lambdaQueryWrapper.eq(name != null, Setmeal::getName, name);
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        //操作数据库进行查询
        setmealPage = setmealService.page(setmealPage, lambdaQueryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");

        //获取原始records数据
        List<Setmeal> setmealPageRecords = setmealPage.getRecords();
        //获取新的records数据并赋值
        List<SetmealDto> setmealDtoList = setmealPageRecords.stream().map((setmeal) ->{
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(setmeal, setmealDto);
            //获取categoryId
            Long categoryId = setmeal.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null){
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(setmealDtoList);
        return R.success(setmealDtoPage);
    }

    /**
     * 套餐批量删除
     * 在套餐管理列表页面点击删除按钮，可以删除对应的套餐信息
     * 也可以通过复选框选择多个套餐，点击批量删除按钮一次删除多个套餐
     * 注意，对于状态为售卖中的套餐不能删除，需要先停售，然后才能删除。
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        setmealService.deleteWithDish(ids);
        return R.success("删除套餐信息成功");
    }

    /**
     * 通过套餐种类Id和套餐对应的状态查询出指定套餐下的所有子套餐信息
     * 请求 URL: http://localhost:8080/dish/list?categoryId=1539486290356031490&status=1
     * 请求方法: GET
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
        log.info("套餐分类信息：{}", setmeal.toString());
        /*这段代码注释原因：此处需求显示套餐分类的下的具体子套餐分类信息，而不是展示再下一层级的子套餐分类中具体的菜品信息

        //构造条件查询对象，并添加条件，通过前端传来的categoryId查询setmeal表，获取到对应的套餐信息
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        setmealLambdaQueryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        Setmeal setmealByCategoryId = setmealService.getOne(setmealLambdaQueryWrapper);

        //构造条件查询对象，并添加条件，通过上面获取到setmeal表的套餐信息，进而去setmeal_dish表中查询到该套餐下的所有菜品信息
        if (setmealByCategoryId != null){
            LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
            setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, setmealByCategoryId.getId());
            List<SetmealDish> setmealDishList = setmealDishService.list(setmealDishLambdaQueryWrapper);
            return R.success(setmealDishList);
        }*/

        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        setmealLambdaQueryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        setmealLambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> setmealList = setmealService.list(setmealLambdaQueryWrapper);
        return R.success(setmealList);
    }

    @GetMapping("/dish/{id}")
    public R<List<DishDto>> dish(@PathVariable Long id){
        log.info(id.toString());

        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishList = setmealDishService.list(setmealDishLambdaQueryWrapper);

        List<DishDto> dishDtoList = setmealDishList.stream().map((setmealDish) -> {
            DishDto dishDto = new DishDto();
            //这里拷贝是浅拷贝，主要是为了拷贝属性copies，这里要注意一下
            BeanUtils.copyProperties(setmealDish, dishDto);
            //这里是为了把套餐中的菜品的基本信息填充到dto中，比如菜品描述，菜品图片等菜品的基本信息
            Dish dish = dishService.getById(setmealDish.getDishId());
            BeanUtils.copyProperties(dish, dishDto);
            return dishDto;
        }).collect(Collectors.toList());

        /*展示效果没有菜品具体份数，所以改为DishDto
        List<Dish> dishList = new ArrayList<>();
        for (SetmealDish setmealDish : setmealDishList){
            LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishLambdaQueryWrapper.eq(Dish::getId, setmealDish.getDishId());
            Dish dish = dishService.getOne(dishLambdaQueryWrapper);
            dishList.add(dish);
        }*/
        return R.success(dishDtoList);
    }


    /**
     * 套餐批量起售和停售
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> update(@PathVariable("status") Integer status, @RequestParam List<Long> ids){
        log.info("status: {}", status);
        log.info("ids: {}", ids);

        setmealService.updateByIds(status, ids);

        return R.success("套餐状态修改成功");
    }

    /**
     * 根据套餐id查询套餐基本信息及套餐关联菜品信息
     * @param id
     * @return
     */
    @GetMapping("{id}")
    public R<SetmealDto> getSetmealById(@PathVariable("id") Long id){
        log.info("id：{}", id);

        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }

    /**
     * 修改套餐信息
     * 包含套餐基本信息和套餐关联菜品信息
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> updateWithDish(@RequestBody SetmealDto setmealDto){
        log.info("套餐信息：{}", setmealDto.toString());
        setmealService.updateWithDish(setmealDto);
        return R.success("修改套餐信息成功");
    }
}
