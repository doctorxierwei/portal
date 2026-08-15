package com.portal.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.blog.entity.SiteConfig;
import com.portal.blog.mapper.SiteConfigMapper;
import com.portal.blog.service.SiteConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SiteConfigServiceImpl extends ServiceImpl<SiteConfigMapper, SiteConfig> implements SiteConfigService {

    /** 固定单行配置 id */
    private static final Long CONFIG_ID = 1L;

    @Override
    public SiteConfig getConfig() {
        SiteConfig cfg = getById(CONFIG_ID);
        if (cfg == null) {
            cfg = new SiteConfig();
            cfg.setId(CONFIG_ID);
            cfg.setSiteName("我的博客");
            cfg.setSlogan("");
            cfg.setHeaderBg("#ffffff");
            cfg.setHeaderOpacity(1.0);
            cfg.setPageBgType("color");
            cfg.setPageBg("#f5f6f7");
            cfg.setFooterText("© 我的博客");
            save(cfg);
        }
        return cfg;
    }

    @Override
    public SiteConfig saveConfig(SiteConfig config) {
        config.setId(CONFIG_ID);
        config.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(config);
        return getById(CONFIG_ID);
    }
}
