<template>
  <div class="home">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="32" color="#409eff"><Cpu /></el-icon>
          <span>DLL VTable Analyzer</span>
        </div>
      </template>
      <p class="desc">上传 DLL 文件，通过 PE 结构解析和 AI 分析，反向出其内部的虚表（vtable）地址信息。</p>
      <el-button type="primary" size="large" @click="$router.push('/analysis')">
        <el-icon><Search /></el-icon>
        开始分析
      </el-button>
    </el-card>

    <el-row :gutter="20" class="features">
      <el-col :span="8">
        <el-card>
          <el-icon :size="28" color="#67c23a"><Document /></el-icon>
          <h3>PE 全面解析</h3>
          <p>解析 PE 头、段表、导出/导入表、调试信息、TLS 等全部结构</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-icon :size="28" color="#e6a23c"><Search /></el-icon>
          <h3>多策略虚表检测</h3>
          <p>RTTI 引导、连续指针扫描、导出交叉引用三种策略互补</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-icon :size="28" color="#f56c6c"><MagicStick /></el-icon>
          <h3>AI 智能分析</h3>
          <p>DeepSeek 大模型辅助确认虚表、排除误报、推测类名</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.home { max-width: 900px; margin: 0 auto; }
.welcome-card { margin-bottom: 24px; }
.card-header { display: flex; align-items: center; gap: 12px; font-size: 20px; font-weight: bold; }
.desc { color: #606266; font-size: 15px; margin-bottom: 20px; line-height: 1.8; }
.features .el-card { text-align: center; padding: 20px; }
.features h3 { margin: 12px 0 8px; font-size: 16px; }
.features p { color: #909399; font-size: 13px; }
</style>
