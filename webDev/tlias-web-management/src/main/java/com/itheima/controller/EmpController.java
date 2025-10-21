package com.itheima.controller;

import com.itheima.pojo.Emp;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/emps")
public class EmpController {

    @Autowired
    private EmpService empService;

    /**
     *
     * @param page      页码数量
     * @param pageSize  页面大小
     * @param name      名字模糊查询
     * @param gender    性别
     * @param begin     入职日期 开始时间
     * @param end       结束时间
     * @return          查询page的封装信息
     */
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       String name, Short gender,
                       // 需要正确进行日期格式的转换
                       @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate begin,
                       @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end){
        log.info("分页查询,参数{} {}", page, pageSize);
        PageBean pageBean =  empService.page(page, pageSize,  name, gender, begin,end);
        return Result.success(pageBean);
    }

    @DeleteMapping("/{ids}")
    // 路径传参
    public Result delete(@PathVariable List<Integer> ids){
        log.info("批量删除操作, ids:{}", ids);
        empService.delete(ids);
        return Result.success();
    }

    @PostMapping
    public Result save(@RequestBody Emp emp){
        log.info("新增一个员工 Emp:{}", emp);
        empService.save(emp);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getByID(@PathVariable Integer id){
        log.info("查询员工的id:{}", id);
        Emp emp = empService.getByID(id);
        return Result.success(emp);

    }

    @PutMapping
    public Result update(@RequestBody Emp emp){
        log.info("修改员工的信息:{}", emp);
        empService.updateByID(emp);
        return Result.success();
    }


}
