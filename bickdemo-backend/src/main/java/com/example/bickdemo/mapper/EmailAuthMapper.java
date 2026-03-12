package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.EmailAuth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmailAuthMapper extends BaseMapper<EmailAuth> {

    @Select("SELECT * FROM email_auth WHERE email = #{email} AND deleted = 0 LIMIT 1")
    EmailAuth findByEmail(@Param("email") String email);

    @Select("SELECT COUNT(*) > 0 FROM email_auth WHERE email = #{email} AND deleted = 0")
    boolean existsByEmail(@Param("email") String email);
}
