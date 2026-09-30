package com.movie.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.movie.api.constant.Roles;
import com.movie.api.mapper.AdminMapper;
import com.movie.api.model.dto.LoginDto;
import com.movie.api.model.entity.Admin;
import com.movie.api.service.AdminService;
import com.movie.api.utils.JwtTokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public String login(LoginDto loginDto) throws Exception {
        QueryWrapper<Admin> wrapper = new QueryWrapper<>();
        wrapper.in("username", loginDto.getUsername());
        Admin admin = adminMapper.selectOne(wrapper);
        if (admin == null || !StringUtils.hasText(loginDto.getPassword())
                || !StringUtils.hasText(admin.getPassword())) throw new Exception("用户名密码错误");
        String stored = admin.getPassword();
        boolean encoded = stored.matches("\\A\\$2[aby]\\$\\d{2}\\$[./0-9A-Za-z]{53}\\z");
        boolean matches = encoded
                ? bCryptPasswordEncoder.matches(loginDto.getPassword(), stored)
                : MessageDigest.isEqual(loginDto.getPassword().getBytes(StandardCharsets.UTF_8),
                        stored.getBytes(StandardCharsets.UTF_8));
        if (!matches) throw new Exception("用户名密码错误");
        // The supplied SQL fixtures contain legacy plaintext admin passwords.
        // Upgrade only after a successful login; existing installations need no forced reset.
        if (!encoded) {
            int updated = adminMapper.update(null, new UpdateWrapper<Admin>()
                    .eq("id", admin.getId())
                    .eq("password", stored)
                    .set("password", bCryptPasswordEncoder.encode(loginDto.getPassword())));
            if (updated != 1) throw new Exception("账号已更新，请重新登录");
        }
        //是否选择记住我
        long exp = loginDto.isRemember() ? JwtTokenUtil.REMEMBER_EXPIRATION_TIME : JwtTokenUtil.EXPIRATION_TIME;
        List<String> roles = new ArrayList<>();
        roles.add(Roles.ROLE_ADMIN);
        return JwtTokenUtil.createToken(loginDto.getUsername(), roles, exp);
    }

}
