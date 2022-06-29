package com.zyx.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyx.reggie.entity.OrderDetail;
import com.zyx.reggie.entity.Orders;

import java.util.List;

public interface OrdersService extends IService<Orders> {
    /**
     * 用户下单
     * @param orders
     */
    void submit(Orders orders);

    /**
     * 通过本地线程中获取用户id，继而获取一个orderDetailList
     * @param orderId
     * @return
     */
    public List<OrderDetail> getOrderDetailListByOrderId(Long orderId);

}
