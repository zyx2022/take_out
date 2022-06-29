package com.zyx.reggie.controller;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyx.reggie.common.BaseContext;
import com.zyx.reggie.common.R;
import com.zyx.reggie.entity.User;
import com.zyx.reggie.service.UserService;
import com.zyx.reggie.utils.SMSUtils;
import com.zyx.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;


    /**
     * 发送手机短信验证码
     * @param user
     * @param session
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();

        if(StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode4String(4);
            log.info("code={}", code);

            //调用阿里云提供的短信服务API完成短信发送
            SMSUtils.sendMessage("瑞吉外卖", "SMS_243960071",phone,code);

            //需要将生成的验证码保存到session
            session.setAttribute(phone,code);
            return R.success("短信发送成功");
        }

        return R.error("短信发送失败");
    }

    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     * 注意：
     * 前端数据 {phone: "19850706069", code: "24ef"，后端这里不能用User接收，因为User无code属性
     * 解决方案：
     * 1、Dto数据传输对象：封装成UserDto，增加Code属性
     * 2、用Map接收，键值对
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        log.info(map.toString());

        //1、获取手机号、验证码
        String phone = map.get("phone").toString();
        String code = map.get("code").toString();

        //2、从Session中获取保存的验证码
        Object codeInSession = session.getAttribute(phone);

        //3、进行验证码的比对（页面提交的验证码和Session中保存的饿验证码比对）
        if (codeInSession != null && codeInSession.equals(code)){
            //4、如果比对成功，说明登录成功
            LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(lambdaQueryWrapper);
            if (user == null){
                //5、判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user", user.getId());
            return R.success(user);
        }

        return R.error("短信发送失败");
    }

    @PostMapping("/loginout")
    public R<String> loginout(HttpSession session){
        session.removeAttribute("user");
        return R.success("退出成功");
    }
}
