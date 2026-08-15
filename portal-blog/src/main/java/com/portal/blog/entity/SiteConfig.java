package com.portal.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客前台站点配置（单行制：id 固定为 1）
 */
@Data
@TableName("blog_site_config")
public class SiteConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 站点名称（左上角展示） */
    private String siteName;

    /** 副标题 / 标语（名称下方小字，可空） */
    private String slogan;

    /** 顶栏背景 CSS：纯色(如 #ffffff) 或 渐变(如 linear-gradient(...)) */
    private String headerBg;

    /** 顶栏透明度 0~1（1=不透明，<1 半透明毛玻璃效果） */
    private Double headerOpacity;

    /** 页面背景类型：color=纯色 / gradient=渐变 / image=图片 */
    private String pageBgType;

    /** 页面背景值：color/gradient 时为 CSS；image 时为图片地址 */
    private String pageBg;

    /** 页面背景透明度 0~1（1=不透明；对图片背景会叠加白色/黑色蒙层） */
    private Double pageOpacity;

    /** 页脚文字（版权/标语，可空） */
    private String footerText;

    private LocalDateTime updateTime;
}
