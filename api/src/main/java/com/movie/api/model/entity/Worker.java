package com.movie.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 影院客服
 */
@Data
@TableName("t_worker")
@NoArgsConstructor
@AllArgsConstructor
public class Worker implements Serializable {

    private String id;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String password;

    private String nickname;

    private String gender;

    private String phone;

    private String avatar;

    //入职
    private boolean entry;

    //部门
    private String department;

    private String createAt;

    private String updateAt;

}
