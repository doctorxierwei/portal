package com.portal.blog.service;

import com.portal.blog.entity.SiteConfig;

public interface SiteConfigService {
    /** 获取站点配置（不存在则初始化默认行） */
    SiteConfig getConfig();

    /** 保存/更新站点配置（固定 id=1） */
    SiteConfig saveConfig(SiteConfig config);
}
