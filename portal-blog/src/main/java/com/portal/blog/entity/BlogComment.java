package com.portal.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("blog_comment")
public class BlogComment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long userId;
    private String nickname;
    private String content;
    private Long parentId;     // 回复哪条评论
    private Integer status;    // 0 待审 1 通过
    private LocalDateTime createTime;
}
