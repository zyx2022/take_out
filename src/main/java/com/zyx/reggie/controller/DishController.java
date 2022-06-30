package com.zyx.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyx.reggie.common.R;
import com.zyx.reggie.dto.DishDto;
import com.zyx.reggie.entity.Category;
import com.zyx.reggie.entity.Dish;
import com.zyx.reggie.entity.DishFlavor;
import com.zyx.reggie.service.CategoryService;
import com.zyx.reggie.service.DishFlavorService;
import com.zyx.reggie.service.DishService;
import jdk.nashorn.internal.ir.CaseNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     *
     * @param dishDto
     * @return 因为Dish实体类不满足接收flavor参数，即需要导入DishDto，用于封装页面提交的数据
     * <p>
     * DTO，全称为Data Transfer Object，即数据传输对象，一般用于展示层与服务层之间的数据传输。
     * 代码逻辑：测试是否可以正常的接收前端传过来的json数据
     * 注意：因为前端传来的是json数据，所以我们需要在参数前添加@RequestBody注解
     */
    @PostMapping
    public R<String> saveWithFlavor(@RequestBody DishDto dishDto) {
        log.info("接收的dishDto数据：{}", dishDto.toString());

        //保存数据到数据库
        dishService.saveWithFlavor(dishDto);

        //清理所有菜品的缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }



    /**
     * 菜品信息分页查询
     * 注意：由于后端响应的数据和前端需要的数据存在不一致，导致菜品分类信息无法显示
     * 利用数据传输对象DishDto对分页构造器进行二次处理，主要在原始基础上，增加categoryName属性并为其赋值
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //1、构造分页构造器
        Page<Dish> dishPage = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>(page, pageSize);//为了前端显示菜品分类，进行修改
        //2、构造查询构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //3、添加过滤、排序条件
        lambdaQueryWrapper.like(name != null, Dish::getName, name);
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        //4、调用DishService层page()执行分页查询
        dishPage = dishService.page(dishPage, lambdaQueryWrapper);

        /**对象拷贝
         * BeanUtils.copyProperties(Object source, Object target);
         * => BeanUtils.copyProperties("转换前的类", "转换后的类");
         * 这里具体含义是：将pageInfo的除records数据外所有的信息全部拷贝给数据传输对象DishDto
         */
        BeanUtils.copyProperties(dishPage, dishDtoPage, "records");
        //获取原始records数据，里面存放着一条条Dish记录
        List<Dish> originalRecords = dishPage.getRecords();
        //获取新的records数据并赋值
        List<DishDto> newRecords = dishDtoPage.getRecords();
        /**
         * 下面这段代码作用：为为 List<DishDto>中的每一条记录，对records进行赋值，
         * 生成一条条新的Dish记录,主要是为了增加categoryName属性并对其进行赋值
         */
        //方法一：通过流的方式
        newRecords = originalRecords.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            /**
             * 拿原始records数据记录中的菜品分类id categoryId
             * 调用CategoryService层方法获取对应的菜品分类category
             * 进而获取到菜品分类具体名称categoryName
             * 最终将categoryName赋值给dishDto（扩展Dish实体类得到的数据传输对象DishDto）并返回
             */
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            //我们需要在分页方法中添加判空条件，若查询的数据为空，经过判断后跳过部分代码，就不会爆空指针异常
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        /**
         * Page.class中属性
         * protected List<T> records = Collections.emptyList();
         * Page<T> setRecords(List<T> records)
         */
        dishDtoPage.setRecords(newRecords);

        return R.success(dishDtoPage);
    }


    /**
     * 根据id查询菜品，实现数据回显
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> getDishById(@PathVariable Long id) {
        //查询
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {

        //保存数据到数据库
        dishService.updateWithFlavor(dishDto);

        //清理所有菜品的缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("修改菜品信息成功");
    }


    /**
     * 通过菜品分类ID获取菜品列表分类
     * 通过条件查询获取该种类下的所有菜品信息，并且查询的菜品必须为起售状态（status=1）
     *
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        List<DishDto> dishDtoList = null;

        //动态构造key
        String key = "dish_"  + dish.getCategoryId() + "_" + dish.getStatus();  //dish_1524731277968793602_1
        //先从redis，获取缓存数据
        dishDtoList = (List<DishDto>)redisTemplate.opsForValue().get(key);

        //如果存在，直接返回， 无需查询数据库
        if (dishDtoList != null){
            return R.success(dishDtoList);
        }

        //如果不存在，需要查询数据库
        //构造条件构造器
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加条件，查询状态为1的菜品（1为起售，0为停售）
        dishLambdaQueryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        dishLambdaQueryWrapper.eq(Dish::getStatus, 1);
        //查询
        List<Dish> dishList = dishService.list(dishLambdaQueryWrapper);

        dishDtoList = dishList.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            //对象拷贝（将dishList中每条菜品记录item拷贝到当前的dishDto对象）
            BeanUtils.copyProperties(item, dishDto);

            /**
             * 以下代码并不需要，原因：index.html在初始化时 initData(){}已经拿到了categoryList
             * 前端 Promise.all([categoryListApi(),cartListApi({})]) ---> /category/list  --->  categoryList  ---> 最终已经可以拿到categoryName
             */
            /*
            //获取当前菜品分类id
            Long categoryId = item.getCategoryId();
            //通过categoryId查询到category内容
            Category category = categoryService.getById(categoryId);
            //判空，设置dishDto的属性categoryName
            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }*/

            //获取当前菜品id
            Long dishId = item.getId();
            //构造条件查询对象
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            //添加查询条件
            dishFlavorLambdaQueryWrapper.eq(dishId != null, DishFlavor::getDishId, dishId);
            //select * from dish_flavors where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            //为当前dishDto的flavors属性赋值
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        //将查询到的菜品数据缓存到redis中
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }


    /**
     * 菜品批量启售和批量停售
     *
     * @param status 菜品状态
     * @param ids    多个菜品id
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable("status") Integer status, @RequestParam List<Long> ids) {
        /**
         * 这个参数这里一定记得加@RequestParam注解才能获取到参数，否则这里非常容易出问题
         * 三个注解@RequestBody、@RequestParam、@PathVariable ,这三个注解之间的区别和应用分别是什么?
         * 区别
         * @RequestParam用于接收url地址传参或表单传参
         * @RequestBody用于接收json数据
         * @PathVariable用于接收路径参数，使用{参数名称}描述路径参数
         * 应用
         * 后期开发中，发送请求参数超过1个时，以json格式为主，@RequestBody应用较广
         * 如果发送非json格式数据，选用@RequestParam接收请求参数
         * 采用RESTful进行开发，当参数数量较少时，例如1个，可以采用@PathVariable接收请求路
         * 径变量，通常用于传递id值
         */
        log.info("status：{}", status);
        log.info("ids: {}", ids.toString());

        dishService.updateByIds(status, ids);

        //清理所有菜品的缓存数据
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
//        String key = "dish_" + dishDto.getCategoryId() + "_1";
//        redisTemplate.delete(key);

        return R.success("菜品状态修改成功");
    }


    /**
     * 菜品批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids: {}", ids.toString());

        dishService.deleteByIds(ids);
        return R.success("批量删除菜品成功");
    }
}
