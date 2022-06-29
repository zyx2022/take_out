package com.zyx.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zyx.reggie.common.BaseContext;
import com.zyx.reggie.common.R;
import com.zyx.reggie.entity.AddressBook;
import com.zyx.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址簿管理
 */
@Slf4j
@RestController
@RequestMapping("/addressBook")
public class AddressBookController {
    @Autowired
    private AddressBookService addressBookService;

    /**
     * 查询指定用户的全部地址
     * @return
     */
    @GetMapping("/list")
    public R<List<AddressBook>> list(AddressBook addressBook){
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("addressBook={}", addressBook);

        //条件构造器
        LambdaQueryWrapper<AddressBook> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(addressBook.getUserId() != null, AddressBook::getUserId,addressBook.getUserId());
        lambdaQueryWrapper.orderByDesc(AddressBook::getUpdateTime);

        List<AddressBook> list = addressBookService.list(lambdaQueryWrapper);
        return R.success(list);
    }

    /**
     *将新增地址保存到数据库,将前端以json格式传输到后端的数据，保存到数据库中
     * @param addressBook
     * @return
     */
    @PostMapping
    public R<AddressBook> save(@RequestBody AddressBook addressBook){
        //必须设置用户id，否则会报错 java.sql.SQLException: Field 'user_id' doesn't have a default value
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("addressBook={}", addressBook);

        addressBookService.save(addressBook);
        return R.success(addressBook);
    }

    /**
     * 设置默认地址
     * 注意：默认地址只能有一个，将前端传递的地址id的is_default字段更新为1，与用户id所关联的其它地址的is_default字段更新为0
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    public R<AddressBook> getDefault(@RequestBody AddressBook addressBook){
        log.info("addressBook={}", addressBook);
        addressBook.setUserId(BaseContext.getCurrentId());

        /**
         * 与用户id所关联的其它地址的is_default字段更新为0
         */
        //构造更新构造器
        LambdaUpdateWrapper<AddressBook> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        //添加条件1：当前user_id的所有地址
        lambdaUpdateWrapper.eq(addressBook.getUserId() != null, AddressBook::getUserId, addressBook.getUserId());
        //添加条件2：is_default字段更新为0
        lambdaUpdateWrapper.set(AddressBook::getIsDefault, 0);
        addressBookService.update(lambdaUpdateWrapper);

        /**
         * 将前端传递的地址id的is_default字段更新为1
         */
        addressBook.setIsDefault(1);
        addressBookService.updateById(addressBook);

        return R.success(addressBook);
    }

    @GetMapping("/default")
    public R<AddressBook> getDefault(){
        Long userId = BaseContext.getCurrentId();

        //构造更新构造器
        LambdaUpdateWrapper<AddressBook> addressBookLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        //添加条件1：当前user_id的默认地址
        addressBookLambdaUpdateWrapper.eq(AddressBook::getUserId, userId);
        addressBookLambdaUpdateWrapper.eq(AddressBook::getIsDefault, 1);
        //查询
        AddressBook addressBook = addressBookService.getOne(addressBookLambdaUpdateWrapper);

        return R.success(addressBook);
    }

    /**
     * 编辑地址的回显
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<AddressBook> getAddressBook(@PathVariable Long id){
        log.info("需修改地址id为：{}", id);
        LambdaQueryWrapper<AddressBook> addressBookLambdaQueryWrapper = new LambdaQueryWrapper<>();
        addressBookLambdaQueryWrapper.eq(id != null, AddressBook::getId, id);
        AddressBook addressBook = addressBookService.getOne(addressBookLambdaQueryWrapper);
        return R.success(addressBook);
    }

    /**
     * 修改地址
     * @param addressBook
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody AddressBook addressBook){
        log.info(addressBook.toString());
        LambdaQueryWrapper<AddressBook> addressBookLambdaQueryWrapper = new LambdaQueryWrapper<>();
        addressBookLambdaQueryWrapper.eq(addressBook.getId() != null, AddressBook::getId, addressBook.getId());
        if (addressBookService.getOne(addressBookLambdaQueryWrapper) != null){
            addressBookService.updateById(addressBook);
            return R.success("修改地址成功");
        }
        return R.error("修改地址失败");
    }

    @DeleteMapping
    public R<String> delete(@RequestParam Long ids){
        log.info(ids.toString());
        LambdaQueryWrapper<AddressBook> addressBookLambdaQueryWrapper = new LambdaQueryWrapper<>();
        addressBookLambdaQueryWrapper.eq(ids != null, AddressBook::getId, ids);
        if (addressBookService.getOne(addressBookLambdaQueryWrapper) != null){
            addressBookService.removeById(ids);
            return R.success("删除地址成功");
        }
        return R.error("修改地址失败");
    }

}
