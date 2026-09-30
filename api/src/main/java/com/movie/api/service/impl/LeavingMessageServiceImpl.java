package com.movie.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.movie.api.mapper.LeavingMessageMapper;
import com.movie.api.mapper.UserMapper;
import com.movie.api.model.entity.LeavingMessage;
import com.movie.api.model.dto.MessageCount;
import com.movie.api.model.entity.User;
import com.movie.api.model.vo.ActiveUserVO;
import com.movie.api.model.vo.LeavingMessageVO;
import com.movie.api.service.LeavingMessageService;
import com.movie.api.utils.DataTimeUtil;
import com.movie.api.utils.BatchLookup;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "leavingMessage")
public class LeavingMessageServiceImpl implements LeavingMessageService {

    @Resource
    private LeavingMessageMapper leavingMessageMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public void save(LeavingMessage leavingMessage) {
        leavingMessage.setCreateAt(DataTimeUtil.getNowTimeString());
        leavingMessageMapper.insert(leavingMessage);
    }

    @Override
    public void reply(LeavingMessage leavingMessage) {
        leavingMessageMapper.updateById(leavingMessage);
    }

    @Override
    public List<LeavingMessageVO> findAll() {
        List<LeavingMessage> list = leavingMessageMapper.selectList(null);
        List<LeavingMessageVO> result = new ArrayList<>();
        Map<String, User> users = BatchLookup.byId(list.stream().map(LeavingMessage::getUid).collect(Collectors.toList()),
                userMapper, User::getId);
        for (LeavingMessage m : list) {
            User user = users.get(m.getUid());
            result.add(new LeavingMessageVO(m.getId(), m, user));
        }
        return result;
    }

    @Override
    public List<ActiveUserVO> findActiveUsers() {
        List<ActiveUserVO> result = new ArrayList<>();
        List<User> users = userMapper.selectList(null);
        Map<String, Integer> counts = leavingMessageMapper.countByUser().stream()
                .collect(Collectors.toMap(MessageCount::getUid, MessageCount::getNumber));
        for (User user : users) {
            ActiveUserVO activeUserVO = new ActiveUserVO();
            activeUserVO.setUser(user);
            activeUserVO.setNumber(counts.getOrDefault(user.getId(), 0));
            result.add(activeUserVO);
        }
        //按留言数量排序
        result.sort((v1, v2) -> v2.getNumber().compareTo(v1.getNumber()));
        return result;
    }

}
