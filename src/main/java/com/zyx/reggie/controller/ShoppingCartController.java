package com.zyx.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyx.reggie.common.BaseContext;
import com.zyx.reggie.common.R;
import com.zyx.reggie.entity.Dish;
import com.zyx.reggie.entity.DishFlavor;
import com.zyx.reggie.entity.ShoppingCart;
import com.zyx.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * 购物车需求分析
 * 移动端用户可以将菜品或者套餐添加到购物车
 * 对于菜品来说，如果设置了口味信息，则需要选择规格后才能加入购物车
 * 对于套餐来说，可以直接点击将当前套餐加入购物车
 * 在购物车中可以修改菜品和套餐的数量，也可以清空购物车。
 * <p>
 * 具体交互过程
 * 点击加入购物车按钮或者+按钮，页面发送ajax请求，请求服务端，将菜品或者套餐添加到购物车
 * 点击购物车图标，页面发送ajax请求，请求服务端查询购物车中的菜品和套餐
 * 点击清空购物车按钮，页面发送ajax请求，请求服务端来执行清空购物车操作
 * 开发购物车功能，其实就是在服务端编写代码去处理前端页面发送的这3次请求即可
 */
@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 购物车添加菜品
     * 用户登录外卖系统，选择喜欢的菜品或套餐，若用户选择菜品并且该菜品有相应的口味则需要选择口味，点击加入购物车
     * 点击加入购物车按钮或者+按钮，页面发送ajax请求，请求服务端，将菜品或者套餐添加到购物车
     * <p>
     * 目的：将用户添加到购物车的菜品或套餐信息保存到数据库中，若添加相同菜品或相同套餐，
     * 则只需要在shopping_cart表更新该菜品或者套餐的数量（number字段）；反之，则将菜品或套餐直接保存数据库中
     *
     * @param shoppingCart 用于接收前端传过来的JSON数据
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> save(@RequestBody ShoppingCart shoppingCart) {
        log.info(shoppingCart.toString());

        //设置用户id，指定当前是哪个用户的购物车数据
        shoppingCart.setUserId(BaseContext.getCurrentId());

        //构造条件查询对象
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();

        //判断添加的是菜品还是套餐
        if (shoppingCart.getDishId() != null) {
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else {
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        //添加查询条件  菜品口味  这一句代码非常重要，直接替换了下面好长一部分的代码
        shoppingCartLambdaQueryWrapper.eq(shoppingCart.getDishFlavor() != null, ShoppingCart::getDishFlavor, shoppingCart.getDishFlavor());

        //查询当前菜品(注意区分不同口味！！！)或者套餐是否在购物车中
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
//        ShoppingCart cartServiceOne = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);

        for (ShoppingCart cartServiceOne : shoppingCartList) {
            if (cartServiceOne != null) {
                cartServiceOne.setNumber(cartServiceOne.getNumber() + 1);
                shoppingCartService.updateById(cartServiceOne);
                return R.success(cartServiceOne);
            }

//            if (cartServiceOne != null && shoppingCart.getDishFlavor() == null && cartServiceOne.getDishFlavor() == null) {
//                //如果记录存在、但口味规格都不存在，则数量+1
//                cartServiceOne.setNumber(cartServiceOne.getNumber() + 1);
//                shoppingCartService.updateById(cartServiceOne);
//                return R.success(cartServiceOne);
//            }else if (cartServiceOne != null && shoppingCart.getDishFlavor() != null && shoppingCart.getDishFlavor().equals(cartServiceOne.getDishFlavor())){
//                //如果记录存在、口味规格存在，且口味规格内容也相同，则数量+1
//                cartServiceOne.setNumber(cartServiceOne.getNumber() + 1);
//                shoppingCartService.updateById(cartServiceOne);
//                return R.success(cartServiceOne);
//            }else if (cartServiceOne != null && shoppingCart.getDishFlavor() != null && !shoppingCart.getDishFlavor().equals(cartServiceOne.getDishFlavor())){
//                //如果记录存在、口味规格存在，但口味规格不相同，则直接添加到购物车，数量默认为1
//                shoppingCartService.save(shoppingCart);
//                return R.success(shoppingCart);
//            }
        }

        //如果记录不存在，则直接添加到购物车，数量默认为1
        shoppingCartService.save(shoppingCart);
        return R.success(shoppingCart);
    }


    /**
     * 购物车删除菜品
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
        //判断删除的是菜品还是套餐，并添加条件查询
        Long dishId = shoppingCart.getDishId();
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (dishId != null){
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getDishId, dishId);
            //添加查询条件  菜品口味
            shoppingCartLambdaQueryWrapper.eq(shoppingCart.getDishFlavor() != null, ShoppingCart::getDishFlavor, shoppingCart.getDishFlavor());
        }else {
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        //查询到相应的菜品或者套餐
        ShoppingCart cartServiceOne = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);
        if (cartServiceOne != null){
            //删减菜品或者套餐的数量
            Integer number = cartServiceOne.getNumber();
            if (number > 1){
                //菜品或者套餐数量大于1时，数量-1，并更新菜品数量到数据库中
                cartServiceOne.setNumber(number - 1);
                shoppingCartService.updateById(cartServiceOne);
            }else if (number == 1){
                //菜品或者套餐数量等于1时，数量-1，并直接删除购物车中对应的菜品和套餐信息
                cartServiceOne.setNumber(number - 1);
                shoppingCartService.removeById(cartServiceOne);
            }
        }
        return R.success(cartServiceOne);
    }

    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(userId != null, ShoppingCart::getUserId, userId);
        shoppingCartLambdaQueryWrapper.orderByDesc(ShoppingCart::getCreateTime);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        return R.success(shoppingCartList);
    }

    @DeleteMapping("/clean")
    public R<String> clean() {
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(userId != null, ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        if (shoppingCartList != null) {
            shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
        }
        return R.success("清空购物车成功");
    }
}