package com.zyx.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.zyx.reggie.common.BaseContext;
import com.zyx.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//过滤器实际上就是对web资源进行拦截，做一些处理后再交给下一个过滤器或servlet处理
//通常都是用来拦截request进行处理的，也可以对返回的response进行拦截处理
@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    //路径匹配
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        log.info("拦截到请求：{}", request.getRequestURI());

        //获取本次请求的URI
        String requestURI = request.getRequestURI();

        //定义不需要被处理的请求地址
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                //我们在进行手机验证码登录时，发送的请求需要在此过滤器处理时直接放行。
                "/user/sendMsg",//移动端发送短信
                "/user/login" //移动端登录
        };

//        判断本次请求是否需要处理
        boolean check = check(urls, requestURI);
        if(check){
            filterChain.doFilter(request,response);
            return;
        }

//        电脑端-->判断登录状态，如果已登录，则直接放行
        if (request.getSession().getAttribute("empId") != null){
            Long epmId = (Long) request.getSession().getAttribute("empId");
//            log.info("**************" + epmId + "****************");
            BaseContext.setCurrentId(epmId);

            filterChain.doFilter(request, response);
            return;
        }

//        移动端-->判断登录状态，如果已登录，则直接放行
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("user"));

            Long userId = (Long)request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request,response);
            return;
        }

  //5.如果未登录则返回未登录结果,通过输出流方式向客户端页面响应数据
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    public boolean check(String[] urls, String requestURI){
        for (String url : urls) {
            //如果不需要处理，则直接放行
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }
}
