package com.zyx.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyx.reggie.common.BaseContext;
import com.zyx.reggie.common.CustomException;
import com.zyx.reggie.controller.ShoppingCartController;
import com.zyx.reggie.entity.*;
import com.zyx.reggie.mapper.OrdersMapper;
import com.zyx.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartController shoppingCartController;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealDishService setmealDishService;


    /**
     * 用户下单
     * 需求分析：将订单基本信息保存到orders表，订单详情信息保存到order_detail表
     *
     * @param orders
     */
    @Override
    @Transactional
    public void submit(Orders orders) {
        log.info("订单信息为：{}", orders);
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();

        //查询当前用户的购物车数据，多条数据
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(userId != null, ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new CustomException("购物车为空，不能下单");
        }

        //查询用户数据
        User user = userService.getById(userId);

        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("地址信息有误，不能下单");
        }

        //设置订单id
        long orderId = IdWorker.getId();//MyBatisPlus提供 package com.baomidou.mybatisplus.core.toolkit;

        /**
         * 1、封装一个orderDetailList，用于订单明细表插入数据
         * 2、设置订单总金额
         * AtomicInteger是一个提供原子操作的Integer类，通过线程安全的方式操作加减。
         * AtomicInteger提供原子操作来进行Integer的使用，因此十分适合多线程、高并发情况下的使用。
         */
        AtomicInteger amount = new AtomicInteger(0);
        List<OrderDetail> orderDetailList = shoppingCartList.stream().map((item) -> {
            //创建OrderDetail对象并逐一赋值
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setName(item.getName());
            orderDetail.setOrderId(orderId);
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setNumber(item.getNumber());
            orderDetail.setAmount(item.getAmount());//这里是单份的金额
            orderDetail.setImage(item.getImage());

            /**
             * java.util.concurrent.atomic.AtomicInteger.addandget()是Java中的一种内置方法，
             * 它将在函数的参数中传递的值添加到先前的值，并返回数据类型为int的新更新值。
             *
             * 累加金额
             * item：shopping_cart表中的当前菜品或套餐数据
             * item.getAmount()：当前菜品或套餐的单份价格
             * item.getNumber()：当前菜品或套餐的单份数量
             * ==>得到当前数量下菜品或套餐数据的总价格
             */
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        //封装一个orders，用于向订单表插入数据，一条数据
        orders.setId(orderId);
        orders.setNumber(String.valueOf(orderId));
        orders.setStatus(2);
        orders.setUserId(userId);
        orders.setAddressBookId(addressBookId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setAmount(new BigDecimal(amount.get()));//订单总金额
        orders.setPhone(addressBook.getPhone());
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(
                (addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName()) +
                (addressBook.getCityName() == null ? "" : addressBook.getCityName()) +
                (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName()) +
                (addressBook.getDetail() == null ? "" : addressBook.getDetail())
        );

        //向订单表插入数据，一条数据
        super.save(orders);

        //向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetailList);

        //清空购物车数据
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
    }


    //抽离的一个方法，通过订单id查询订单明细，得到一个订单明细的集合
    //这里抽离出来是为了避免在stream中遍历的时候直接使用构造条件来查询导致eq叠加，从而导致后面查询的数据都是null
    public List<OrderDetail> getOrderDetailListByOrderId(Long orderId){
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);
        return orderDetailList;
    }

}
