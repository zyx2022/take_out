package com.zyx.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyx.reggie.entity.AddressBook;
import com.zyx.reggie.mapper.AddressBookMapper;
import com.zyx.reggie.service.AddressBookService;
import org.springframework.stereotype.Service;

@Service
public class AddressBookServicelmpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {
}
