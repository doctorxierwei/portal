package com.portal.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.blog.entity.BlogArticle;
import com.portal.blog.entity.BlogCategory;
import com.portal.blog.mapper.BlogArticleMapper;
import com.portal.blog.mapper.BlogCategoryMapper;
import com.portal.blog.service.BlogArticleService;
import com.portal.common.entity.SysUser;
import com.portal.common.mapper.SysUserMapper;
import com.portal.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleMapper, BlogArticle> implements BlogArticleService {

    @Resource
    private BlogCategoryMapper categoryMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public Page<BlogArticle> pageArticle(int current, int size, String keyword, Integer status, Long categoryId) {
        return pageArticle(current, size, keyword, status, categoryId, null);
    }

    @Override
    public Page<BlogArticle> pageArticle(int current, int size, String keyword, Integer status,
                                         Long categoryId, Long authorId) {
        QueryWrapper<BlogArticle> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like("title", keyword);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        if (categoryId != null) {
            qw.eq("category_id", categoryId);
        }
        // 数据权限: 非管理员只能看自己的文章
        if (authorId != null) {
            qw.eq("author_id", authorId);
        }
        qw.orderByDesc("create_time");
        Page<BlogArticle> page = page(new Page<>(current, size), qw);
        page.getRecords().forEach(a -> { fillCategory(a); fillAuthorName(a); });
        return page;
    }

    @Override
    public BlogArticle detail(Long id) {
        BlogArticle a = getById(id);
        if (a != null) {
            a.setTagIds(baseMapper.selectTagIds(id));
            fillCategory(a);
            fillAuthorName(a);
        }
        return a;
    }

    @Override
    @Transactional
    public Long saveArticle(BlogArticle article, List<Long> tagIds) {
        if (article.getId() == null) {
            article.setViews(0);
            article.setCreateTime(LocalDateTime.now());
        }
        article.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(article);
        baseMapper.deleteArticleTags(article.getId());
        if (tagIds != null && !tagIds.isEmpty()) {
            baseMapper.insertArticleTags(article.getId(), tagIds);
        }
        return article.getId();
    }

    @Override
    @Transactional
    public void removeArticle(Long id) {
        baseMapper.deleteArticleTags(id);
        removeById(id);
    }

    @Override
    public void incrViews(Long id) {
        BlogArticle a = getById(id);
        if (a != null) {
            a.setViews((a.getViews() == null ? 0 : a.getViews()) + 1);
            updateById(a);
        }
    }

    @Override
    public boolean canOperate(Long articleId) {
        if (UserContext.isAdmin()) {
            return true;
        }
        Long userId = UserContext.currentUserId();
        if (userId == null || articleId == null) {
            return false;
        }
        BlogArticle a = getById(articleId);
        // 文章不存在时交由上层处理; 存在则必须是本人的文章
        return a != null && userId.equals(a.getAuthorId());
    }

    private void fillCategory(BlogArticle a) {
        if (a.getCategoryId() != null) {
            BlogCategory c = categoryMapper.selectById(a.getCategoryId());
            if (c != null) a.setCategoryName(c.getName());
        }
    }

    /**
     * 若作者名缺失, 按 author_id 从 sys_user 取 nickname 或 username 回填。
     * 兼容老数据: 早期文章可能只存了 "佚名"。
     */
    private void fillAuthorName(BlogArticle a) {
        if (StringUtils.hasText(a.getAuthorName()) && !"佚名".equals(a.getAuthorName())) {
            return;
        }
        if (a.getAuthorId() == null) {
            return;
        }
        SysUser user = sysUserMapper.selectById(a.getAuthorId());
        if (user != null) {
            a.setAuthorName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        }
    }
}
