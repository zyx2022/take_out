package com.zyx.reggie.dto;

import com.zyx.reggie.entity.Dish;
import com.zyx.reggie.entity.DishFlavor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO，全称为Data Transfer Object，即数据传输对象，一般用于展示层与服务层之间的数据传输。
 * 因为Dish实体类不满足接收flavor参数，即需要导入DishDto，用于封装页面提交的数据
 */
@Data
public class DishDto extends Dish {
    /**
     * 后端用List集合flavors，接收前端传过来的JSON数据
     * flavors: [{name: "甜味", value: "["无糖","少糖","半糖","多糖","全糖"]", showOption: false},…]
     */
    private List<DishFlavor> flavors = new ArrayList<>();

    /**
     * 前端页面发现菜品分类对应的prop属性名为categoryName
     * 但我们在响应的数据当中并没有发现categoryName字段  前端只有："categoryId":"1397844263642378242"
     * 页面需要什么数据，服务端就应该返还什么样的数据，所以Dish对象不满足该页面要求
     * DishDto类，发现类中的属性名正好和前端的属性名对应
     */
    private String categoryName;
    private Integer copies;
}
