package com.portal.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.blog.entity.BlogArticle;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

    @Select("SELECT tag_id FROM blog_article_tag WHERE article_id = #{articleId}")
    List<Long> selectTagIds(@Param("articleId") Long articleId);

    @Insert("<script>" +
            "INSERT INTO blog_article_tag (article_id, tag_id) VALUES " +
            "<foreach collection='tagIds' item='tid' separator=','>(#{articleId}, #{tid})</foreach>" +
            "</script>")
    void insertArticleTags(@Param("articleId") Long articleId, @Param("tagIds") List<Long> tagIds);

    @Select("DELETE FROM blog_article_tag WHERE article_id = #{articleId}")
    void deleteArticleTags(@Param("articleId") Long articleId);

    /** 查询某作者名下的全部文章 ID, 用于评论数据权限过滤 */
    @Select("SELECT id FROM blog_article WHERE author_id = #{authorId}")
    List<Long> selectIdsByAuthor(@Param("authorId") Long authorId);
}
