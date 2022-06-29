package com.zyx.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyx.reggie.entity.Employee;
import com.zyx.reggie.mapper.EmployeeMapper;
import com.zyx.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {
}
