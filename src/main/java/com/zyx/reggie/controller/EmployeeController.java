package com.zyx.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyx.reggie.common.R;
import com.zyx.reggie.entity.Employee;
import com.zyx.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.swing.plaf.PanelUI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@Slf4j
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工对登录
     * @param httpServletRequest
     * @param employee
     * @return
     * @RequestBody：用于接收前端前端传递给后端的JSON字符串（请求体中的数据）
     * HttpServletRequest request作用：如果登录成功，将员工对应的id存到session一份，这样想获取一份登录用户的信息就可以随时获取出来
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest httpServletRequest, @RequestBody Employee employee){
//        将页面提交的密码进行MD5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
//        根据页面提交的用户名username查询数据库
        //构造查询条件构造器
        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        lambdaQueryWrapper.eq(Employee::getUsername, employee.getUsername());
        //调用employeeService进行查询
        Employee emp = employeeService.getOne(lambdaQueryWrapper);
//        如果没有查询到则返回登录失败结果
        if(emp == null){
            return R.error("用户不存在，登录失败");
        }
//        密码比对，如果不一致则返回登录结果
        if(!emp.getPassword().equals(password)){
            return R.error("密码错误，登录失败");
        }
//        查看员工状态，如果已为禁用状态，则返回员工已禁用结果
        if (emp.getStatus() == 0){
            return R.error("用户已经被禁用，登录失败");
        }

        /**
         * 请求转发资源间共享数据:使用Request对象
         * 存储数据到request域[范围,数据是存储在request对象]中 void setAttribute(String name,Object o);
         * 根据key获取值 Object getAttribute(String name);
         * 根据key删除该键值对 void removeAttribute(String name);
         */
        //登录成功，将员工id存入session以便将来使用，并返回登录成功结果
        httpServletRequest.getSession().setAttribute("empId", emp.getId());
        return R.success(emp);
    }

    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //清理session中中保存的当前登录员工的id
        request.getSession().removeAttribute("empId");
        return R.success("退出成功");
    }

    /**
     * 新增员工
     * @param request
     * @param employee
     * @return
     */
    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee){
        log.info("新增员工的信息：{}", employee.toString());
        //设置初始密码，需要进行md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));

        //设置创建时间、更新时间
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());

        //处理新增员工ID，强转为Long类型
//        Long empId = (Long) request.getSession().getAttribute("empId");
        //设置创建人ID，更新人ID
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);

        employeeService.save(employee);
        return R.success("新增员工成功");
    }

    /**
     * 分页显示员工信息
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page = {}, pageSize = {}, name = {}",page,pageSize,name);
        //构造分页构造器
        Page<Employee> pageInfo = new Page<>(page, pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        lambdaQueryWrapper.eq(name != null, Employee::getUsername, name);
        //添加排序添加
        lambdaQueryWrapper.orderByDesc(Employee::getUpdateTime);
        //调用service层方法进行查询
        employeeService.page(pageInfo,lambdaQueryWrapper);
        /**
         * 后端Page类中：
         * protected List<T> records;
         * protected long total;
         *
         * 前端list.html中
         * this.tableData = res.data.records || []
         * this.counts = res.data.total
         *
         * 综上所述，返回值应携带records和total
         */
        return R.success(pageInfo);
    }

    /**
     * 编辑、修改员工信息
     * @param request
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee){
        log.info(employee.toString());

//        Long empId = (Long) request.getSession().getAttribute("empId"); //登录阶段已存入 httpServletRequest.getSession().setAttribute("empId", emp.getId());

//        employee.setUpdateUser(empId);
//        employee.setUpdateTime(LocalDateTime.now());
        employeeService.updateById(employee);
        /**
         * SQL执行的结果是更新的数据行数为0，仔细观察id的值1539109576580919300，和数据库中对应记录的id值1539109576580919297并不相同。
         * 即js对long型数据进行处理时丢失精度，导致提交的id和数据库中的id不一致。
         * 如何解决这个问题?
         * 我们可以在服务端给页面响应json数据时进行处理，将long型数据统一转为String字符串
         */
        return R.success("员工修改信息成功");
    }

    /**
     * 根据id查询员工信息并进行回显
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息...");
        Employee employee = employeeService.getById(id);
        if(employee != null){
            return R.success(employee);
        }
        return R.error("没有查询到该员工信息");
    }
}
