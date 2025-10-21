package com.itheima.controller;


import com.itheima.pojo.Result;
import com.itheima.utils.AliOSSUtils;
import com.sun.source.tree.ReturnTree;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
public class UploadController {
    @Autowired
    private AliOSSUtils aliOSSUtils;
//    /**
//     * 文件上传的函数
//     * @param username 用户名
//     * @param age      年龄
//     * @param image    图像
//     * @return         void
//     */
//    @PostMapping("/upload")
//    // server接收文件要使用Multipartfle来接收.
//    public Result upload(String username, Integer age, MultipartFile image) throws IOException {
//        log.info("实现文件的上传:{}, {}, {}", username, age, image);
//        // 怎么进行本地的存储
//        String originFileName = image.getOriginalFilename();
//
//        // 一旦上传的名称相同,就会覆盖,怎么生成唯一的名称
//        // UUID通用唯一识别码
//        assert originFileName != null;
//        int index = originFileName.lastIndexOf(".");
//        String extName = originFileName.substring(index);
//        // 我们下载的时候好像就会有很多UUID
//        String newFileName = UUID.randomUUID() + extName;
//        log.info("生成新的文件名称:{}", newFileName);
//
//
//        image.transferTo(new File("/home/ada/Pictures/ServerImageStorage/" + newFileName));
//
//        return Result.success();
//    }

    @PostMapping("/upload")
    public Result upload(MultipartFile image) throws IOException {
        log.info("文件上传,name:{}", image.getOriginalFilename());
        String url =  aliOSSUtils.upload(image);
        log.info("文件url:{}", url);

        return Result.success();
    }


}
