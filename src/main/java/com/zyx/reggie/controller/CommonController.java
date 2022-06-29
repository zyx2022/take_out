package com.zyx.reggie.controller;

import com.zyx.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {
    @Value("${reggie.path}")
    private String basePath;

    //MultipartFile是spring类型，代表HTML中form data方式上传的文件，包含二进制数据+文件名称。
    //MultipartFile后面的参数名必须为file，因为需要和前端页面的name保持一致，否则不会生效
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        //file是一个临时文件，需要转存到指定位置，否则本次请求完成后临时文件删除
        log.info(file.toString());

        /**
         * 处理文件
         */
        //获取原始文件的文件名
        String originalFilename = file.getOriginalFilename();
        //获取上传的文件后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //使用UUID重新生成文件名，防止文件名重复造成文件覆盖
        String fileName = UUID.randomUUID().toString() + suffix;

        /**
         * 处理文件的存储位置 文件夹
         */
        //首先创建一个目录对象
        File dir = new File(basePath);
        //判断目录是否存在，不存在，则需要创建
        if(!dir.exists()){
            //目录不存在，需要创建
            dir.mkdir();
        }

        //将临时文件存储到指定位置
        try {
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //服务端需返回文件名给前端，便于后续开发使用
        return R.success(fileName);
    }

    @GetMapping("/download")
    public void download(String name, HttpServletResponse response){
        FileInputStream fileInputStream = null;
        ServletOutputStream outputStream = null;
        try {
            //造文件对象
            File file = new File(basePath + name);
            //造输入流，将文件对象扔进去，通过输入流兑取文件内容
            fileInputStream = new FileInputStream(file);
            //造输出流，通过输出流将文件写回浏览器，在浏览器展示图片
            outputStream = response.getOutputStream();
            //代表图片文件
            response.setContentType("image/jpeg");

            int len = 0;
            byte[] bytes = new byte[1024];
            while((len = fileInputStream.read(bytes)) != -1){
                //向response缓冲区写入字节，再由Tomcat服务器将字节内容组成http响应返回给浏览器
                outputStream.write(bytes, 0, len);
                //所存储的数据全部清空
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
