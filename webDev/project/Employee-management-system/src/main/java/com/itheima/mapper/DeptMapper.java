package com.itheima.mapper;

import com.itheima.anno.Log;
import com.itheima.pojo.Dept;
import org.apache.ibatis.annotations.*;

import java.net.Inet4Address;
import java.util.List;

/**
 * 部门管理
 */
@Mapper
public interface DeptMapper {

    @Select("SELECT * FROM dept")
    List<Dept> list();

    @Log
    @Delete("DELETE FROM dept where id = #{id}")
    void deleteByID(Integer id);

    @Log
    @Insert("INSERT INTO dept(name, create_time, update_time) VALUES(#{name},#{createTime}, #{updateTime})")
    void insert(Dept dept);

    @Log
    @Update("UPDATE dept SET name = #{name}, update_time = #{updateTime} where id = #{id}")
    void updateByID(Dept dept);
}
