package com.movie.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movie.api.model.entity.LeavingMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.movie.api.model.dto.MessageCount;
import java.util.List;

@Mapper
public interface LeavingMessageMapper extends BaseMapper<LeavingMessage> {
    @Select("SELECT uid, COUNT(*) AS number FROM t_leaving_message GROUP BY uid")
    List<MessageCount> countByUser();
}
