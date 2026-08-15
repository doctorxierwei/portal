package com.portal.blog.controller;

import com.portal.blog.entity.SiteConfig;
import com.portal.blog.service.SiteConfigService;
import com.portal.common.result.R;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/blog/site-config")
public class SiteConfigController {

    @Resource
    private SiteConfigService siteConfigService;

    /**
     * 公开接口：前台（未登录）拉取站点配置。
     * 需配合网关白名单 /blogs/blog/site-config/public/** 免鉴权。
     */
    @GetMapping("/public")
    public R<SiteConfig> publicConfig() {
        return R.ok(siteConfigService.getConfig());
    }

    /**
     * 保存配置（后台管理员，走网关鉴权）。
     */
    @PostMapping
    public R<SiteConfig> save(@RequestBody SiteConfig config) {
        return R.ok(siteConfigService.saveConfig(config));
    }
}
