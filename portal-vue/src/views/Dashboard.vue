<template>
  <el-row :gutter="16">
    <el-col :span="8">
      <el-card><div class="stat"><div class="num">用户管理</div><div class="label">系统用户</div></div></el-card>
    </el-col>
    <el-col :span="8">
      <el-card><div class="stat"><div class="num">菜单管理</div><div class="label">权限菜单</div></div></el-card>
    </el-col>
    <el-col :span="8">
      <el-card><div class="stat"><div class="num">微服务</div><div class="label">网关 + 认证 + 用户</div></div></el-card>
    </el-col>
  </el-row>
  <el-card class="mt">欢迎使用门户网站管理系统</el-card>
</template>

<script setup></script>
<style scoped>
.stat .num { font-size: 20px; font-weight: bold; }
.stat .label { color: #999; margin-top: 6px; }
.mt { margin-top: 16px; }
</style>
