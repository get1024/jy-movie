package com.movie.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movie.api.model.entity.Film;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FilmMapper extends BaseMapper<Film> {
    @Update("UPDATE t_film SET hot = COALESCE(hot, 0) + #{amount} WHERE id = #{id}")
    int incrementHot(@Param("id") String id, @Param("amount") int amount);
}
