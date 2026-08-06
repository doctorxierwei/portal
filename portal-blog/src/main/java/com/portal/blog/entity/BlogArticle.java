package com.portal.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("blog_article")
public class BlogArticle implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String summary;       // 摘要
    private String cover;         // 封面图 URL
    private String content;       // 正文 HTML (LONGTEXT)
    private Long categoryId;
    private Integer status;       // 0 草稿 1 已发布
    private Long authorId;
    private String authorName;
    private Integer views;        // 浏览量
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private List<Long> tagIds;
}
