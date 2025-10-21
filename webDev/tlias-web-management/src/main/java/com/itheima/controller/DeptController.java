package com.itheima.controller;

import com.itheima.pojo.Dept;
import com.itheima.pojo.Result;
import com.itheima.service.DeptService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理Controller
 */
// 三层架构Controller层
@Slf4j  // 直接使用log进行调用
@RequestMapping("/depts") // 直接把公共方法抽取到这个类上面
@RestController
public class DeptController {

    // 使用logger来打日志
    // private static Logger log = LoggerFactory.getLogger(DeptController.class);

    @Autowired
    private DeptService deptService;

    // @RequestMapping(value = "/depts", method = RequestMethod.GET) //指定这个接口的请求方式
    @GetMapping // --->简单地限定这个请求的方式
    public Result list() {
        log.info("查询全部员工的数据");

        List<Dept> deptLis = deptService.list();

        return Result.success(deptLis);
    }

    /**
     * 删除部门的员工
     *
     * @return void
     */
    @DeleteMapping("/{id}")
    // PathVariable:路径传参
    public Result delete(@PathVariable Integer id) {
        log.info("根据id删除一个部门 id:{}", id);
        deptService.delete(id);
        return Result.success();
    }

    /**
     * 新增一个部门
     *
     * @return void
     */
    @PostMapping
    // 把收到的消息封装在dept内部即可
    public Result add(@RequestBody Dept dept) {
        log.info("新增部门:{}", dept);
        deptService.add(dept);
        return Result.success();
    }

    /**
     * 修改部门
     * @return void
     */
    @PutMapping
    public Result update(@RequestBody Dept dept){
        log.info("新增部门{}", dept);
        deptService.update(dept);
        return Result.success();
    }

}
