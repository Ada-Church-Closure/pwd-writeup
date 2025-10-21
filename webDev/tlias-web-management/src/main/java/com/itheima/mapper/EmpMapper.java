package com.itheima.mapper;

import com.itheima.anno.Log;
import com.itheima.pojo.Emp;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工管理
 */
@Mapper
public interface EmpMapper {
//    /**
//     * 查询总记录数字
//     * @return 总记录数
//     */
//    @Select("SELECT COUNT(*) FROM emp")
//    public Long count();
//
//    /**
//     * 分页查询,来获取列表的数据
//     * @param start 起始索引
//     * @param pageSize  page大小
//     * @return  查询到的员工的列表
//     */
//    @Select("SELECT * FROM emp LIMIT #{start}, #{pageSize}")
//    public List<Emp> page(Integer start, Integer pageSize);


    /**
     * 员工信息查询
     * @return  返回一个员工的列表
     */
    // @Select("SELECT  * FROM emp")
    public List<Emp> list(String name, Short gender, LocalDate begin,LocalDate end);

    /**
     * 员工信息删除
     * @param ids:要删除的员工的id
     */
    @Log
    void delete(List<Integer> ids);

    /**
     * 新增员工
     */
    @Log
    @Insert("INSERT INTO emp(username, name, gender, image, job, entrydate, dept_id, create_time,update_time)" + "VALUES (#{username}, #{name}, #{gender}, #{image},#{job}, #{entrydate},#{deptId}, #{createTime},#{updateTime})")
    void insert(Emp emp);

    @Select("SELECT * FROM emp where id = #{id}")
    Emp getByID(Integer id);


    void updateByID(Emp emp);

    @Select("SELECT * FROM emp where username = #{username} and password = #{password}")
    Emp getByUsernameAndPassword(Emp emp);

    /**
     * 根据部门的ID删除这个部门的所有员工
     * @param deptId    要删除部门的ID
     */
    @Log
    @Delete("DELETE FROM emp where dept_id = #{deptId}")
    void deleteByDeptId(Integer deptId);
}
