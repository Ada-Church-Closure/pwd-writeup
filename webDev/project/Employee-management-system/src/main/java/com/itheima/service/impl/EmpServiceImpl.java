package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.Emp;
import com.itheima.pojo.PageBean;
import com.itheima.service.EmpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmpServiceImpl implements EmpService {
    @Autowired
    private EmpMapper empMapper;

//    @Override
//    public PageBean page(Integer page, Integer pageSize) {
//        Long count = empMapper.count();
//
//        Integer start = (page - 1) * pageSize;
//        List<Emp> rows = empMapper.page(start, pageSize);
//
//        return new PageBean(count, rows);
//    }

    // 直接使用pagehelper来进行分页查询
    @Override
    public PageBean page(Integer page, Integer pageSize, String name, Short gender, LocalDate begin,LocalDate end) {
        // 1.设置分页的参数
        PageHelper.startPage(page, pageSize);

        // 2.查询--->强制转换为page类型
        List<Emp> empList =  empMapper.list(name, gender, begin,end);
        Page<Emp> p = (Page<Emp>)  empList;

        return new PageBean(p.getTotal(), p.getResult());
    }

    @Override
    public void delete(List<Integer> ids) {
        empMapper.delete(ids);
    }

    /**
     * 新增员工
     * @param emp 封装json参数到一个实体类
     */
    @Override
    public void save(Emp emp) {
        emp.setCreateTime(LocalDateTime.now());
        emp.setUpdateTime(LocalDateTime.now());
        empMapper.insert(emp);
    }

    @Override
    public Emp getByID(Integer id) {
        return empMapper.getByID(id);
    }

    @Override
    public void updateByID(Emp emp) {
        emp.setUpdateTime(LocalDateTime.now());
        empMapper.updateByID(emp);
    }

    @Override
    public Emp login(Emp emp) {
        return empMapper.getByUsernameAndPassword(emp);
    }

}
