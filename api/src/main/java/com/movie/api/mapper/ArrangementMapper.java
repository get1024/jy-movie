package com.movie.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movie.api.model.entity.Arrangement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArrangementMapper extends BaseMapper<Arrangement> {
    @Select("SELECT * FROM t_arrangement WHERE id = #{id} FOR UPDATE")
    Arrangement selectByIdForUpdate(@Param("id") String id);
}
