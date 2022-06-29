package com.zyx.reggie.controller;

import com.aliyuncs.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyx.reggie.common.BaseContext;
import com.zyx.reggie.common.R;
import com.zyx.reggie.dto.OrdersDto;
import com.zyx.reggie.entity.*;
import com.zyx.reggie.service.OrderDetailService;
import com.zyx.reggie.service.OrdersService;
import com.zyx.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrdersController {
    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 用户下单
     * 需求分析：将订单基本信息保存到orders表，订单详情信息保存到order_detail表
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单信息为：{}", orders);

        //保存订单基本信息到orders表
        ordersService.submit(orders);

        return R.success("下单成功");
    }

    /**
     * 移动端展示自己的订单分页查询
     * @param page
     * @param pageSize
     * @return
     * 遇到的坑：原来分页对象中的records集合存储的对象是分页泛型中的对象，里面有分页泛型对象的数据
     *      * 开始的时候我以为前端只传过来了分页数据，其他所有的数据都要从本地线程存储的用户id开始查询，
     *      * 结果就出现了一个用户id查询到 n个订单对象，然后又使用 n个订单对象又去查询 m 个订单明细对象，
     *      * 结果就出现了评论区老哥出现的bug(嵌套显示数据....)
     *      * 正确方法:直接从分页对象中获取订单id就行，问题大大简化了.....
     */
    @GetMapping("/userPage")
    public R<Page<OrdersDto>> userPage(int page, int pageSize){
        log.info("分页信息为：page = {}， pageSize = {}", page, pageSize);

        //构造分页查询对象
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page,pageSize);
        //构造条件查询对象
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        ordersLambdaQueryWrapper.eq(Orders::getUserId,BaseContext.getCurrentId());
        //这里是直接把当前用户分页的全部结果查询出来，要添加用户id作为查询条件，否则会出现用户可以查询到其他用户的订单情况
        //添加排序条件，根据更新时间降序排列
        ordersLambdaQueryWrapper.orderByDesc(Orders::getOrderTime);
        ordersPage = ordersService.page(ordersPage, ordersLambdaQueryWrapper);

        //通过orderId查询对应的orderDetail
        LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();

        //获取原始ordersPage中的records
        List<Orders> ordersPageRecords = ordersPage.getRecords();
        //ordersPage的所有属性赋值给ordersDtoPage
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");

        //对orderDto进行必要的属性orderDetails赋值
        List<OrdersDto> orderDtoList = ordersPageRecords.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            //先复制原有属性
            BeanUtils.copyProperties(item, ordersDto);
            //此时的orderDto对象里面orderDetails属性还是空 下面准备为它赋值
            Long orderId = item.getId();
            //拿着订单表orders中id字段值，在订单明细表order_detail中获取orderDetailList
            List<OrderDetail> orderDetailList = ordersService.getOrderDetailListByOrderId(orderId);
            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;
        }).collect(Collectors.toList());

        //逻辑关系：ordersDtoPage.setRecords(orderDtoList); --> ordersDto.setOrderDetails(orderDetailList); --> orderDetailList -->ordersService.getOrderDetailListByOrderId(orderId);
        ordersDtoPage.setRecords(orderDtoList);

        return R.success(ordersDtoPage);
    }


    /**
     * 再来一单
     * 前端点击再来一单是直接跳转到购物车的，所以为了避免数据有问题，在跳转之前我们需要把购物车的数据给清除
     * @param orderDetail
     * @return
     */
    @PostMapping("/again")
    public R<String> again(@RequestBody OrderDetail orderDetail){
        log.info(orderDetail.toString());

        //1、要清空当前用户的购物车
        LambdaQueryWrapper<ShoppingCart> oldShoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        oldShoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartService.remove(oldShoppingCartLambdaQueryWrapper);

        //2、根据ordersId，在订单明细表order_detail中获取当前订单明细
        LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderDetailLambdaQueryWrapper.eq(orderDetail.getOrderId() != null, OrderDetail::getOrderId, orderDetail.getOrderId());
        List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailLambdaQueryWrapper);

        //3、获取具体的菜品或套餐数据,并封装成购物车对象，获取shoppintCartList集合
        List<ShoppingCart> shoppintCartList = orderDetailList.stream().map((item) -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setName(item.getName());
            shoppingCart.setImage(item.getImage());
            shoppingCart.setUserId(BaseContext.getCurrentId());
            if (item.getDishId() != null){
                shoppingCart.setDishId(item.getDishId());
                shoppingCart.setDishFlavor(item.getDishFlavor());
            }else {
                shoppingCart.setSetmealId(item.getSetmealId());
            }
            shoppingCart.setNumber(item.getNumber());
            shoppingCart.setAmount(item.getAmount());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        //把携带数据的购物车批量插入购物车表，这个批量保存的方法要使用熟练！！！
        shoppingCartService.saveBatch(shoppintCartList);

        return R.success("再次下单成功");
    }


    /**
     * PC端 展示订单明细
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number, String beginTime, String endTime){
        //构造分页对象
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        //构造查询对象
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件  动态sql  字符串使用StringUtils.isNotEmpty这个方法来判断
        //这里使用了范围查询的动态SQL，这里是重点！！
        ordersLambdaQueryWrapper.like(number != null, Orders::getNumber, number)
                .gt(!StringUtils.isEmpty(beginTime), Orders::getOrderTime, beginTime)
                .lt(!StringUtils.isEmpty(endTime), Orders::getOrderTime, endTime);

        ordersService.page(ordersPage, ordersLambdaQueryWrapper);
        return R.success(ordersPage);
    }

    /**
     * 修改订单状态
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> updateStatus(@RequestBody Orders orders){
        log.info(orders.toString());

        if (orders.getId() == null || orders.getStatus() == null){
            return R.error("传入信息不合法");
        }

        if (ordersService.getById(orders.getId()) != null) {
            ordersService.updateById(orders);
            return R.success("订单状态修改成功");
        }
        return R.success("订单状态修改失败");
    }
}
