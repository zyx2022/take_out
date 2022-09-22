# 订餐项目说明

## 项目简介
本项目分为两部分：系统管理后台和移动端应用。
其中系统管理后台供餐饮企业管理者使用，主要有员工管理、分类管理、菜品管理、套餐管理、订单细明等功能模块。
移动端应用供消费者使用，主要有浏览菜品、购物车管理、地址管理、订单管理等功能模块。

本项目是基于B站中黑马程序员视频，进行学习开发的。总体而言，难度系数并不大。 对于初次接触项目者，可极大的锻炼需求分析能力、编码能力、bug调试能力，增长开发经验。 项目中前端代码均为黑马程序员所提供，后端代码全部为本人所写并完善。

## 技术架构
用户层：H5、VUE.js、ElementUI、axios 

应用层：Spring boot、Spring MVC、Spring Session、Spring、Lombok

数据层：MySQL、Mybatis Plus、Redis 

工具：git、maven、junit

## 使用说明
运行项目，在浏览器可进行系统后台管理和移动端页面。 本项目添加URL过滤器，当输入其他URL,自动跳转到登录页面

1、后台管理：管理员：admin ，密码：123456 （默认自动填充），添加员工时，默认初始密码为123456，存入数据库时已做加密处理。 后台管理:http://localhost:8080/backend/page/login/login.html

2、移动端：用户采用手机注册，模拟发送验证码，验证码可在Idea控制台中查看。验证码缓存于Redis中。 移动端：http://localhost:8080/front/page/login.html

## 补充说明
本项目已增加黑马程序员视频中未完善的功能。本人技术可能不到位，代码不够完美，但功能均已测试通过。