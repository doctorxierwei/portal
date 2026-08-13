package com.portal.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("blog_image")
public class BlogImage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据库只存相对路径(对象名), 如 abc.jpg; 环境切换(换 MinIO IP)只需改 url-prefix 配置 */
    private String path;

    /** 完整可访问地址: 由 path 实时拼 url-prefix 得到, 不落库 (前端继续用 img.url, 零改动) */
    @TableField(exist = false)
    private String url;

    private String name;
    private Long size;
    private String contentType;
    private String md5;
    private Long uploaderId;
    private LocalDateTime createTime;
}
