package com.zyx.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyx.reggie.common.R;
import com.zyx.reggie.entity.Category;
import com.zyx.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 在分类管理功能模块中，新增菜品分类、套餐分类
     * @param category
     * @return
     * 新增菜品分类和新增套餐分类请求的服务端地址和提交的json数据结构相同，所以服务端只需要提供一个方法统一处理即可
     */
    @PostMapping
    public R<String> save(@RequestBody Category category){
        categoryService.save(category);
        return R.success("新增分类成功");
    }

    /**
     * 在分类管理功能模块中，展示所有菜品信息
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize){
        //构造分页构造器
        Page<Category> pageInfo = new Page<>(page, pageSize);//这里注意要传入page, pageSize两个参数，否则前端部分分页功能会失效
        //构造条件构造器
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Category::getSort);
        //调用categoryService的方法进行查询
        categoryService.page(pageInfo, lambdaQueryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 在分类管理功能模块中，删除菜品分类信息
     * @param ids
     * @return
     */
    @DeleteMapping
    //这里为什么不用@PathVariable
    public R<String> delete(Long ids){
        categoryService.remove(ids);
        return R.success("删除成功");
    }

    /**
     * 在分类管理功能模块中，修改菜品分类信息
     * @param category
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Category category){
        categoryService.updateById(category);
        return R.success("修改分类信息成功");
    }

    /**
     * 根据菜品分类表category中type字段查询对应的菜品
     * @param category
     * @return
     */
    @GetMapping("/list")
    public R<List<Category>> list(Category category){
        //构造条件构造器
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        lambdaQueryWrapper.eq(category.getType() != null, Category::getType, category.getType());
        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);
        //查询数据
        List<Category> categoryList = categoryService.list(lambdaQueryWrapper);
        return R.success(categoryList);
    }
}
