package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper 接口
 * 用于用户数据的数据库访问
 * @author Administrator
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 获取所有管理员用户名
     */
    @Select("SELECT username FROM users WHERE role = 'ADMIN' AND enabled = 1 AND deleted = 0")
    List<String> selectAllAdminUsernames();

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    User findByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND deleted = 0")
    User findByEmail(@Param("email") String email);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username} AND deleted = 0")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email} AND deleted = 0")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 按用户名模糊搜索
     */
    @Select("""
            SELECT *
            FROM users
            WHERE username LIKE CONCAT('%', #{keyword}, '%')
              AND id <> #{excludeUserId}
              AND enabled = 1
              AND deleted = 0
            ORDER BY username ASC
            LIMIT #{limit}
            """)
    List<User> searchByUsernameLike(
            @Param("keyword") String keyword,
            @Param("excludeUserId") Long excludeUserId,
            @Param("limit") Integer limit
    );
}
