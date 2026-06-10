<template>
  <div class="novel-workspace" v-loading="store.loading">
    <aside class="library-pane">
      <div class="pane-head">
        <div>
          <p class="eyebrow">创作工作台</p>
          <h3>小说助手</h3>
        </div>
        <el-button circle size="small" @click="createDialogVisible = true">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <div class="book-list">
        <button
          v-for="book in store.books"
          :key="book.id"
          class="book-row"
          :class="{ active: book.id === store.activeBookId }"
          @click="store.switchBook(book.id)"
        >
          <span class="book-title">{{ book.title || "未命名作品" }}</span>
          <span class="book-meta">{{ book.genre || "未设定题材" }} · {{ book.chapterCount }} 章</span>
        </button>
        <el-empty v-if="!store.books.length" description="暂无书籍" :image-size="70" />
      </div>

      <div class="chapter-head">
        <span>章节</span>
        <el-button text size="small" :disabled="!store.project" @click="handleAddChapter">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
      <div class="chapter-list">
        <button
          v-for="chapter in store.sortedChapters"
          :key="chapter.id"
          class="chapter-row"
          :class="{ active: chapter.id === selectedChapterId }"
          @click="selectedChapterId = chapter.id"
        >
          <span>{{ chapter.order }}. {{ chapter.title || "未命名章节" }}</span>
          <small>{{ countWords(chapter.draft) }} 字</small>
        </button>
      </div>
    </aside>

    <main v-if="store.project" class="workspace-main">
      <section class="workspace-toolbar">
        <div>
          <p class="eyebrow">{{ currentBook?.genre || store.project.meta.genre || "未设定题材" }}</p>
          <h2>{{ store.project.meta.title || "未命名作品" }}</h2>
        </div>
        <div class="toolbar-actions">
          <el-tag v-if="store.dirty" type="warning">未保存</el-tag>
          <el-tag v-else type="success">已同步</el-tag>
          <el-button :loading="store.saving" @click="store.saveCurrent">
            <el-icon><FolderChecked /></el-icon>
            保存
          </el-button>
        </div>
      </section>

      <el-tabs v-model="activeTab" class="workspace-tabs">
        <el-tab-pane label="总览" name="overview">
          <section class="panel-section overview-grid">
            <div class="form-panel">
              <div class="section-title">
                <el-icon><Notebook /></el-icon>
                <span>作品信息</span>
              </div>
              <el-form label-position="top">
                <el-form-item label="书名">
                  <el-input v-model="store.project.meta.title" @input="store.markDirty" />
                </el-form-item>
                <el-form-item label="一句话简介">
                  <el-input v-model="store.project.meta.synopsis" type="textarea" :rows="3" resize="none" @input="store.markDirty" />
                </el-form-item>
                <el-form-item label="核心设定">
                  <el-input v-model="store.project.meta.premise" type="textarea" :rows="4" resize="none" @input="store.markDirty" />
                </el-form-item>
                <div class="two-cols">
                  <el-form-item label="频道">
                    <el-segmented v-model="store.project.meta.audienceChannel" :options="audienceOptions" @change="handleAudienceChange" />
                  </el-form-item>
                  <el-form-item label="题材">
                    <el-select v-model="store.project.meta.novelType" filterable @change="handleNovelTypeChange">
                      <el-option v-for="type in currentNovelTypes" :key="type.id" :label="type.label" :value="type.id" />
                    </el-select>
                  </el-form-item>
                </div>
                <div class="two-cols">
                  <el-form-item label="调性">
                    <el-input v-model="store.project.meta.tone" @input="store.markDirty" />
                  </el-form-item>
                  <el-form-item label="目标字数">
                    <el-input v-model="store.project.meta.targetLength" @input="store.markDirty" />
                  </el-form-item>
                </div>
                <el-form-item label="严格模式">
                  <el-switch v-model="store.project.meta.strictMode" active-text="开启" inactive-text="关闭" @change="store.markDirty" />
                </el-form-item>
              </el-form>
            </div>

            <div class="stats-panel">
              <div class="stat-item">
                <strong>{{ store.project.chapters.length }}</strong>
                <span>章节</span>
              </div>
              <div class="stat-item">
                <strong>{{ totalWords }}</strong>
                <span>正文字数</span>
              </div>
              <div class="stat-item">
                <strong>{{ store.project.outlineNodes.length }}</strong>
                <span>大纲节点</span>
              </div>
              <div class="stat-item">
                <strong>{{ store.project.characters.length }}</strong>
                <span>角色</span>
              </div>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="开书" name="bootstrap">
          <section class="panel-section split-main">
            <div class="form-panel">
              <div class="section-title">
                <el-icon><MagicStick /></el-icon>
                <span>8 问灵感开书</span>
              </div>
              <el-alert v-if="!store.aiReady" type="warning" :closable="false" show-icon title="请先在设置页保存 AI Key、接口链接和模型，开书生成功能才会启用。" />
              <div class="question-list">
                <label v-for="question in inspirationQuestions" :key="question.id" class="question-item">
                  <span>{{ question.title }}</span>
                  <el-input v-model="inspirationAnswers[question.id]" type="textarea" :rows="2" resize="none" :placeholder="question.placeholder" />
                </label>
              </div>
              <div class="inline-actions">
                <el-button type="primary" :disabled="!store.aiReady" :loading="bootstrapLoading" @click="handleGenerateProposals">
                  生成 3 套方案
                </el-button>
              </div>
            </div>

            <div class="proposal-list">
              <div v-for="proposal in outlineProposals" :key="proposal.id || proposal.name" class="proposal-panel">
                <h4>{{ proposal.name }}</h4>
                <p>{{ proposal.pitch || proposal.premise }}</p>
                <div class="proposal-meta">
                  <el-tag size="small">{{ proposal.outlineNodes?.length || 0 }} 个大纲节点</el-tag>
                  <el-tag size="small">{{ proposal.characters?.length || 0 }} 个角色</el-tag>
                </div>
                <el-button size="small" type="primary" @click="handleApplyProposal(proposal)">应用方案</el-button>
              </div>
              <el-empty v-if="!outlineProposals.length" description="方案会显示在这里" :image-size="80" />
            </div>
          </section>

          <section class="panel-section questionnaire-section">
            <div class="section-title">
              <el-icon><Tickets /></el-icon>
              <span>15 问开书问卷</span>
            </div>
            <div class="inline-actions">
              <el-button :disabled="!store.aiReady" :loading="questionnaireLoading" @click="handleGenerateQuestionnaire">生成问卷</el-button>
              <el-button type="primary" :disabled="!onboardingAnswers.length" @click="handleSubmitQuestionnaire">提交问卷</el-button>
            </div>
            <div class="question-grid">
              <label v-for="answer in onboardingAnswers" :key="answer.questionId" class="question-item">
                <span>{{ answer.question }}</span>
                <el-input v-model="answer.answer" type="textarea" :rows="2" resize="none" />
              </label>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="设定" name="settings">
          <section class="panel-section">
            <el-tabs v-model="settingsTab">
              <el-tab-pane label="大纲" name="outline">
                <div class="list-toolbar">
                  <span>大纲节点</span>
                  <el-button size="small" @click="addOutlineNode"><el-icon><Plus /></el-icon>新增</el-button>
                </div>
                <div class="editable-list">
                  <div v-for="node in store.sortedOutline" :key="node.id" class="edit-row">
                    <div class="row-title">
                      <strong>{{ node.order }}. {{ node.title || "新大纲节点" }}</strong>
                      <el-button text type="danger" @click="removeOutlineNode(node.id)"><el-icon><Delete /></el-icon></el-button>
                    </div>
                    <el-input v-model="node.title" placeholder="节点标题" @input="store.markDirty" />
                    <el-input v-model="node.summary" type="textarea" :rows="2" resize="none" placeholder="摘要" @input="store.markDirty" />
                    <el-input v-model="node.objective" placeholder="目标" @input="store.markDirty" />
                    <el-input :model-value="linesToText(node.mustKeep)" type="textarea" :rows="2" resize="none" placeholder="必保留项，每行一条" @update:model-value="node.mustKeep = textToLines($event); store.markDirty()" />
                    <el-input :model-value="linesToText(node.forbidden)" type="textarea" :rows="2" resize="none" placeholder="禁写项，每行一条" @update:model-value="node.forbidden = textToLines($event); store.markDirty()" />
                  </div>
                </div>
              </el-tab-pane>
              <el-tab-pane label="角色" name="characters">
                <div class="list-toolbar">
                  <span>角色库</span>
                  <el-button size="small" @click="addCharacter"><el-icon><Plus /></el-icon>新增</el-button>
                </div>
                <div class="editable-list">
                  <div v-for="character in store.project.characters" :key="character.id" class="edit-row">
                    <div class="row-title">
                      <strong>{{ character.name || "新角色" }}</strong>
                      <el-button text type="danger" @click="removeCharacter(character.id)"><el-icon><Delete /></el-icon></el-button>
                    </div>
                    <div class="two-cols">
                      <el-input v-model="character.name" placeholder="姓名" @input="store.markDirty" />
                      <el-input v-model="character.role" placeholder="定位" @input="store.markDirty" />
                    </div>
                    <el-input v-model="character.profile" type="textarea" :rows="2" resize="none" placeholder="人物画像" @input="store.markDirty" />
                    <el-input v-model="character.motivation" placeholder="动机" @input="store.markDirty" />
                    <el-input v-model="character.relationships" placeholder="关系" @input="store.markDirty" />
                  </div>
                </div>
              </el-tab-pane>
              <el-tab-pane label="伏笔" name="foreshadowing">
                <div class="list-toolbar">
                  <span>伏笔债务</span>
                  <el-button size="small" @click="addForeshadowing"><el-icon><Plus /></el-icon>新增</el-button>
                </div>
                <div class="editable-list">
                  <div v-for="item in store.project.foreshadowing" :key="item.id" class="edit-row">
                    <div class="row-title">
                      <strong>{{ item.title || "新伏笔" }}</strong>
                      <el-button text type="danger" @click="removeForeshadowing(item.id)"><el-icon><Delete /></el-icon></el-button>
                    </div>
                    <el-input v-model="item.title" placeholder="标题" @input="store.markDirty" />
                    <el-input v-model="item.setup" type="textarea" :rows="2" resize="none" placeholder="埋设方式" @input="store.markDirty" />
                    <el-input v-model="item.payoff" type="textarea" :rows="2" resize="none" placeholder="回收方式" @input="store.markDirty" />
                    <div class="two-cols">
                      <el-input v-model="item.plannedReveal" placeholder="计划揭示" @input="store.markDirty" />
                      <el-select v-model="item.status" @change="store.markDirty">
                        <el-option label="计划中" value="planned" />
                        <el-option label="已揭示" value="revealed" />
                        <el-option label="已回收" value="paid_off" />
                      </el-select>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </section>
        </el-tab-pane>

        <el-tab-pane label="写作" name="writing">
          <section v-if="currentChapter" class="panel-section writing-layout">
            <div class="editor-panel">
              <div class="section-title">
                <el-icon><EditPen /></el-icon>
                <span>{{ currentChapter.title || "章节写作" }}</span>
              </div>
              <div class="two-cols">
                <el-input v-model="currentChapter.title" placeholder="章节标题" @input="store.markDirty" />
                <el-input v-model="currentChapter.purpose" placeholder="章节目的" @input="store.markDirty" />
              </div>
              <el-input v-model="currentChapter.summary" type="textarea" :rows="2" resize="none" placeholder="章节摘要，可留空由后端自动补" @input="store.markDirty" />
              <div class="chapter-bindings">
                <el-select v-model="currentChapter.outlineNodeIds" multiple collapse-tags placeholder="绑定大纲" @change="store.markDirty">
                  <el-option v-for="node in store.sortedOutline" :key="node.id" :label="node.title || `节点 ${node.order}`" :value="node.id" />
                </el-select>
                <el-select v-model="currentChapter.characterIds" multiple collapse-tags placeholder="出场角色" @change="store.markDirty">
                  <el-option v-for="character in store.project.characters" :key="character.id" :label="character.name || character.role" :value="character.id" />
                </el-select>
              </div>
              <el-input
                v-model="currentChapter.draft"
                type="textarea"
                class="draft-input"
                :rows="22"
                resize="none"
                placeholder="在这里写正文，或使用 AI 生成/续写。"
                @input="store.markDirty"
              />
              <div class="writer-actions">
                <el-button type="primary" :disabled="!store.aiReady || generating" :loading="generating" @click="handleGenerate(false)">
                  <el-icon><MagicStick /></el-icon>
                  重新生成
                </el-button>
                <el-button :disabled="!store.aiReady || generating" :loading="generating" @click="handleGenerate(true)">
                  <el-icon><Position /></el-icon>
                  续写
                </el-button>
                <el-button v-if="generating" type="danger" @click="cancelGeneration">取消</el-button>
                <el-button :loading="reviewing" @click="handleReviewAll">全文审查</el-button>
                <el-button @click="handleAnalyzeReadingPower">追读力</el-button>
                <el-button type="success" @click="handleCommitChapter">提交章节</el-button>
                <span class="word-count">{{ countWords(currentChapter.draft) }} 字</span>
              </div>
            </div>
          </section>
          <el-empty v-else description="请先新增章节" />
        </el-tab-pane>

        <el-tab-pane label="质量" name="quality">
          <section class="panel-section quality-grid">
            <div class="form-panel">
              <div class="section-title">
                <el-icon><CircleCheck /></el-icon>
                <span>生成与审查结果</span>
              </div>
              <div v-if="generationResult" class="result-block">
                <el-alert :type="generationResult.accepted ? 'success' : 'error'" :closable="false" :title="generationResult.accepted ? '生成已通过' : generationResult.rejectionReason || '生成未通过'" />
                <ul v-if="generationResult.warnings?.length" class="compact-list">
                  <li v-for="warning in generationResult.warnings" :key="warning">{{ warning }}</li>
                </ul>
              </div>
              <div v-for="entry in reviewEntries" :key="entry.chapterId" class="review-entry">
                <h4>{{ entry.title }}</h4>
                <div v-for="issue in entry.issues" :key="issue.original" class="issue-row">
                  <p>{{ issue.description }}</p>
                  <small>{{ issue.original }}</small>
                  <el-button size="small" @click="applyIssueFix(entry.chapterId, issue)">一键修复</el-button>
                </div>
              </div>
              <el-empty v-if="!generationResult && !reviewEntries.length" description="暂无质量结果" :image-size="80" />
            </div>

            <div class="form-panel">
              <div class="section-title">
                <el-icon><TrendCharts /></el-icon>
                <span>追读力与提交</span>
              </div>
              <pre class="json-preview">{{ qualitySnapshot }}</pre>
              <div class="inline-actions">
                <el-button @click="loadQualityPanels">刷新质量数据</el-button>
              </div>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="知识库" name="knowledge">
          <section class="panel-section knowledge-grid">
            <div class="form-panel import-panel">
              <div class="section-title">
                <el-icon><Upload /></el-icon>
                <span>导入文本</span>
              </div>
              <div class="import-grid">
                <div>
                  <el-input
                    v-model="importOutlineText"
                    type="textarea"
                    :rows="6"
                    resize="none"
                    placeholder="粘贴大纲、设定、角色表或伏笔清单"
                  />
                  <el-button class="import-button" :loading="importingOutline" @click="handleImportOutline">导入大纲</el-button>
                </div>
                <div>
                  <el-input
                    v-model="importChaptersText"
                    type="textarea"
                    :rows="6"
                    resize="none"
                    placeholder="粘贴已有章节正文，可包含多个章节标题"
                  />
                  <el-button class="import-button" :loading="importingChapters" @click="handleImportChapters">导入章节</el-button>
                </div>
              </div>
              <p v-if="importSummary" class="import-summary">{{ importSummary }}</p>
            </div>
            <div class="form-panel">
              <div class="section-title">
                <el-icon><Files /></el-icon>
                <span>上下文与记忆</span>
              </div>
              <div class="inline-actions">
                <el-button :disabled="!currentChapter" @click="loadContextPreview">组装上下文</el-button>
                <el-button :disabled="!currentChapter" @click="loadMemoryPack">记忆包</el-button>
              </div>
              <pre class="json-preview">{{ contextPreviewText }}</pre>
            </div>
            <div class="form-panel">
              <div class="section-title">
                <el-icon><Search /></el-icon>
                <span>RAG 搜索</span>
              </div>
              <el-input v-model="ragQuery" placeholder="搜索设定、章节、记忆" clearable>
                <template #append>
                  <el-button @click="handleRagSearch">搜索</el-button>
                </template>
              </el-input>
              <div class="rag-results">
                <div v-for="result in ragResults" :key="result.chunkId || result.text" class="rag-item">
                  <strong>{{ Math.round((result.score || 0) * 100) / 100 }}</strong>
                  <p>{{ result.text }}</p>
                </div>
              </div>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </main>

    <main v-else class="empty-main">
      <el-empty description="创建一本书后开始使用小说助手">
        <el-button type="primary" @click="createDialogVisible = true">创建书籍</el-button>
      </el-empty>
    </main>

    <aside class="inspector-pane">
      <div class="inspector-block">
        <div class="section-title">
          <el-icon><Connection /></el-icon>
          <span>AI 状态</span>
        </div>
        <el-tag :type="store.aiReady ? 'success' : 'warning'">{{ store.aiReady ? "已配置" : "未配置" }}</el-tag>
        <p>{{ aiStatusText }}</p>
        <el-button size="small" @click="goSettings">打开设置</el-button>
      </div>
      <div class="inspector-block">
        <div class="section-title">
          <el-icon><DataAnalysis /></el-icon>
          <span>当前章节</span>
        </div>
        <p v-if="currentChapter">{{ currentChapter.title || "未命名章节" }}</p>
        <small v-if="currentChapter">{{ countWords(currentChapter.draft) }} 字 · 第 {{ currentChapter.order }} 章</small>
      </div>
      <div class="inspector-block">
        <div class="section-title">
          <el-icon><Memo /></el-icon>
          <span>状态</span>
        </div>
        <p>{{ store.errorText || store.statusText || "就绪" }}</p>
      </div>
    </aside>

    <el-dialog v-model="createDialogVisible" title="创建书籍" width="420px">
      <el-form label-position="top">
        <el-form-item label="书名">
          <el-input v-model="createForm.title" placeholder="可留空" />
        </el-form-item>
        <el-form-item label="频道">
          <el-segmented v-model="createForm.audienceChannel" :options="audienceOptions" />
        </el-form-item>
        <el-form-item label="题材">
          <el-select v-model="createForm.novelType" filterable>
            <el-option v-for="type in createNovelTypes" :key="type.id" :label="type.label" :value="type.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateBook">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  CircleCheck,
  Connection,
  DataAnalysis,
  Delete,
  EditPen,
  Files,
  FolderChecked,
  MagicStick,
  Memo,
  Notebook,
  Plus,
  Position,
  Search,
  Tickets,
  TrendCharts,
  Upload,
} from "@element-plus/icons-vue";
import {
  analyzeNovelChaptersImport,
  analyzeNovelOutlineImport,
  analyzeNovelReadingPower,
  applyOutlineProposal,
  commitNovelChapter,
  fetchNovelCommits,
  fetchNovelContext,
  fetchNovelEvents,
  fetchNovelMemoryPack,
  fetchNovelMemoryStats,
  fetchNovelRagStats,
  generateOnboarding,
  generateOutlineProposals,
  searchNovelRag,
  streamNovelChapter,
  streamNovelReview,
  submitOnboarding,
} from "@/api/novel";
import { useNovelStore } from "@/stores/novel";
import type {
  NovelChapterGenerationResponse,
  NovelReviewIssue,
  NovelRagResult,
  OnboardingAnswer,
  OutlineBootstrapProposal,
} from "@/types";

const router = useRouter();
const store = useNovelStore();

const activeTab = ref("overview");
const settingsTab = ref("outline");
const selectedChapterId = ref("");
const createDialogVisible = ref(false);
const bootstrapLoading = ref(false);
const questionnaireLoading = ref(false);
const generating = ref(false);
const reviewing = ref(false);
const generationResult = ref<NovelChapterGenerationResponse | null>(null);
const reviewResults = ref(new Map<string, { title: string; issues: NovelReviewIssue[] }>());
const readingPower = ref<Record<string, unknown> | null>(null);
const readingPowerStats = ref<Record<string, unknown> | null>(null);
const commits = ref<unknown[]>([]);
const events = ref<unknown[]>([]);
const contextPreview = ref<unknown>(null);
const memoryStats = ref<Record<string, unknown> | null>(null);
const ragStats = ref<Record<string, unknown> | null>(null);
const ragQuery = ref("");
const ragResults = ref<NovelRagResult[]>([]);
const generationAbort = ref<AbortController | null>(null);
const reviewAbort = ref<AbortController | null>(null);
const outlineProposals = ref<OutlineBootstrapProposal[]>([]);
const onboardingAnswers = ref<OnboardingAnswer[]>([]);
const importOutlineText = ref("");
const importChaptersText = ref("");
const importingOutline = ref(false);
const importingChapters = ref(false);
const importSummary = ref("");

const createForm = reactive({
  title: "",
  audienceChannel: "male",
  novelType: "xuanyi",
});

const inspirationQuestions = [
  { id: "core", title: "核心爽点/情绪", placeholder: "读者最应该被什么吸引？" },
  { id: "lead", title: "主角起点", placeholder: "身份、困境、优势、短板" },
  { id: "hook", title: "开局钩子", placeholder: "前三章发生什么不可逆事件？" },
  { id: "power", title: "成长体系", placeholder: "力量、职业、系统、资源或关系网" },
  { id: "conflict", title: "主线冲突", placeholder: "敌人、压力、目标、代价" },
  { id: "cast", title: "关键角色", placeholder: "伙伴、对手、感情线或师徒线" },
  { id: "world", title: "世界规则", placeholder: "平台、时代、地图、行业或禁忌" },
  { id: "taboo", title: "禁写/雷点", placeholder: "不想出现的套路、设定或表达" },
];

const inspirationAnswers = reactive<Record<string, string>>({});

const audienceOptions = [
  { label: "男频", value: "male" },
  { label: "女频", value: "female" },
];

const currentBook = computed(() => store.books.find((book) => book.id === store.activeBookId));
const currentChapter = computed(() => store.sortedChapters.find((chapter) => chapter.id === selectedChapterId.value) || store.sortedChapters[0] || null);
const totalWords = computed(() => store.sortedChapters.reduce((sum, chapter) => sum + countWords(chapter.draft), 0));
const currentNovelTypes = computed(() => {
  const project = store.project;
  const types = store.novelTypes?.types || [];
  return types.filter((type) => type.audienceChannel === (project?.meta.audienceChannel || "male"));
});
const createNovelTypes = computed(() => (store.novelTypes?.types || []).filter((type) => type.audienceChannel === createForm.audienceChannel));
const aiStatusText = computed(() => {
  const settings = store.buildAiSettings();
  if (!settings.enabled) return "生成、续写、开书方案会禁用；本地审查仍可使用。";
  return `${settings.provider} · ${settings.model}`;
});
const reviewEntries = computed(() =>
  Array.from(reviewResults.value.entries()).map(([chapterId, value]) => ({ chapterId, ...value }))
);
const contextPreviewText = computed(() => JSON.stringify(contextPreview.value || { memoryStats: memoryStats.value, ragStats: ragStats.value }, null, 2));
const qualitySnapshot = computed(() =>
  JSON.stringify({ readingPower: readingPower.value, readingPowerStats: readingPowerStats.value, commits: commits.value, events: events.value }, null, 2)
);

watch(
  () => store.sortedChapters.map((chapter) => chapter.id).join(","),
  () => {
    if (!selectedChapterId.value || !store.sortedChapters.some((chapter) => chapter.id === selectedChapterId.value)) {
      selectedChapterId.value = store.sortedChapters[0]?.id || "";
    }
  }
);

watch(
  () => createNovelTypes.value,
  (types) => {
    if (types.length && !types.some((type) => type.id === createForm.novelType)) {
      createForm.novelType = types[0].id;
    }
  },
  { immediate: true }
);

onMounted(async () => {
  await store.loadAll();
  selectedChapterId.value = store.sortedChapters[0]?.id || "";
  await loadSideStats();
  if (store.project?.onboarding?.questions?.length) {
    onboardingAnswers.value = store.project.onboarding.questions.map((question) => ({
      questionId: question.id,
      question: question.title,
      answer: store.project?.onboarding.answers.find((item) => item.questionId === question.id)?.answer || "",
    }));
  }
});

function countWords(text?: string) {
  return (text || "").replace(/[\s\p{P}\p{S}]/gu, "").length;
}

function linesToText(lines?: string[]) {
  return (lines || []).join("\n");
}

function textToLines(value: string | number) {
  return String(value)
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function nonBlank(value: unknown) {
  return String(value || "").trim();
}

function importedLines(value: unknown) {
  return Array.isArray(value)
    ? value.map((item) => nonBlank(item)).filter(Boolean)
    : textToLines(nonBlank(value));
}

function mergeLines(base: string[], extra: string[]) {
  return Array.from(new Set([...(base || []), ...extra].filter(Boolean)));
}

type ImportAnalysis = {
  title?: string;
  synopsis?: string;
  premise?: string;
  tone?: string;
  targetLength?: string;
  styleRules?: string[];
  worldRules?: string[];
  outlineNodes?: Array<Record<string, unknown>>;
  chapters?: Array<Record<string, unknown>>;
  characters?: Array<Record<string, unknown>>;
  foreshadowing?: Array<Record<string, unknown>>;
};

function applyImportAnalysis(analysis: ImportAnalysis) {
  if (!store.project) return { outlineCount: 0, chapterCount: 0, characterCount: 0, foreshadowingCount: 0 };
  const outlineNodes = Array.isArray(analysis.outlineNodes) ? analysis.outlineNodes : [];
  const chapters = Array.isArray(analysis.chapters) ? analysis.chapters : [];
  const characters = Array.isArray(analysis.characters) ? analysis.characters : [];
  const foreshadowing = Array.isArray(analysis.foreshadowing) ? analysis.foreshadowing : [];

  store.updateProject((project) => {
    if (!project.meta.title && nonBlank(analysis.title)) project.meta.title = nonBlank(analysis.title);
    if (!project.meta.synopsis && nonBlank(analysis.synopsis)) project.meta.synopsis = nonBlank(analysis.synopsis);
    if (!project.meta.premise && nonBlank(analysis.premise)) project.meta.premise = nonBlank(analysis.premise);
    if (!project.meta.tone && nonBlank(analysis.tone)) project.meta.tone = nonBlank(analysis.tone);
    if (!project.meta.targetLength && nonBlank(analysis.targetLength)) project.meta.targetLength = nonBlank(analysis.targetLength);
    project.meta.styleRules = mergeLines(project.meta.styleRules, importedLines(analysis.styleRules));
    project.meta.worldRules = mergeLines(project.meta.worldRules, importedLines(analysis.worldRules));

    const outlineStart = Math.max(0, ...project.outlineNodes.map((node) => node.order || 0));
    outlineNodes.forEach((node, index) => {
      project.outlineNodes.push({
        id: crypto.randomUUID(),
        order: outlineStart + index + 1,
        title: nonBlank(node.title) || `导入节点 ${outlineStart + index + 1}`,
        summary: nonBlank(node.summary),
        objective: nonBlank(node.objective),
        keyConflict: nonBlank(node.keyConflict),
        mustKeep: importedLines(node.mustKeep),
        forbidden: importedLines(node.forbidden),
      });
    });

    const chapterStart = Math.max(0, ...project.chapters.map((chapter) => chapter.order || 0));
    chapters.forEach((chapter, index) => {
      project.chapters.push({
        id: crypto.randomUUID(),
        order: chapterStart + index + 1,
        title: nonBlank(chapter.title) || `导入章节 ${chapterStart + index + 1}`,
        summary: nonBlank(chapter.summary),
        purpose: nonBlank(chapter.purpose),
        outlineNodeIds: [],
        characterIds: [],
        foreshadowingIds: [],
        mandatoryBeats: [],
        forbiddenContent: [],
        notes: "",
        draft: nonBlank(chapter.content) || nonBlank(chapter.draft),
      });
    });

    characters.forEach((character) => {
      project.characters.push({
        id: crypto.randomUUID(),
        name: nonBlank(character.name),
        role: nonBlank(character.role),
        profile: nonBlank(character.profile),
        motivation: nonBlank(character.motivation),
        constraint: nonBlank(character.constraint),
        relationships: nonBlank(character.relationships),
      });
    });

    foreshadowing.forEach((item) => {
      const status = nonBlank(item.status);
      project.foreshadowing.push({
        id: crypto.randomUUID(),
        title: nonBlank(item.title),
        setup: nonBlank(item.setup),
        payoff: nonBlank(item.payoff),
        plannedReveal: nonBlank(item.plannedReveal),
        status: ["planned", "revealed", "paid_off"].includes(status) ? status : "planned",
        notes: "",
      });
    });
  });

  return {
    outlineCount: outlineNodes.length,
    chapterCount: chapters.length,
    characterCount: characters.length,
    foreshadowingCount: foreshadowing.length,
  };
}

function handleAudienceChange() {
  if (!store.project) return;
  const types = currentNovelTypes.value;
  store.project.meta.novelType = types[0]?.id || "";
  handleNovelTypeChange();
}

function handleNovelTypeChange() {
  if (!store.project) return;
  const type = store.novelTypes?.types.find((item) => item.id === store.project?.meta.novelType);
  const audience = store.novelTypes?.audiences.find((item) => item.id === store.project?.meta.audienceChannel);
  store.project.meta.genre = `${audience?.label || ""}${type?.label ? ` · ${type.label}` : ""}`.trim();
  store.markDirty();
}

async function handleCreateBook() {
  await store.createBook({
    title: createForm.title.trim(),
    audienceChannel: createForm.audienceChannel,
    novelType: createForm.novelType,
  });
  createDialogVisible.value = false;
  selectedChapterId.value = store.sortedChapters[0]?.id || "";
}

async function handleAddChapter() {
  store.addChapter();
  selectedChapterId.value = store.sortedChapters.at(-1)?.id || "";
}

function addOutlineNode() {
  store.updateProject((project) => {
    const order = Math.max(0, ...project.outlineNodes.map((node) => node.order || 0)) + 1;
    project.outlineNodes.push({
      id: crypto.randomUUID(),
      order,
      title: `阶段 ${order}`,
      summary: "",
      objective: "",
      keyConflict: "",
      mustKeep: [],
      forbidden: [],
    });
  });
}

function removeOutlineNode(id: string) {
  store.updateProject((project) => {
    project.outlineNodes = project.outlineNodes.filter((node) => node.id !== id).map((node, index) => ({ ...node, order: index + 1 }));
    project.chapters = project.chapters.map((chapter) => ({
      ...chapter,
      outlineNodeIds: chapter.outlineNodeIds.filter((nodeId) => nodeId !== id),
    }));
  });
}

function addCharacter() {
  store.updateProject((project) => {
    project.characters.push({ id: crypto.randomUUID(), name: "", role: "", profile: "", motivation: "", constraint: "", relationships: "" });
  });
}

function removeCharacter(id: string) {
  store.updateProject((project) => {
    project.characters = project.characters.filter((character) => character.id !== id);
    project.chapters = project.chapters.map((chapter) => ({
      ...chapter,
      characterIds: chapter.characterIds.filter((characterId) => characterId !== id),
    }));
  });
}

function addForeshadowing() {
  store.updateProject((project) => {
    project.foreshadowing.push({
      id: crypto.randomUUID(),
      title: "",
      setup: "",
      payoff: "",
      plannedReveal: "",
      status: "planned",
      notes: "",
    });
  });
}

function removeForeshadowing(id: string) {
  store.updateProject((project) => {
    project.foreshadowing = project.foreshadowing.filter((item) => item.id !== id);
  });
}

async function handleGenerateProposals() {
  if (!store.aiReady) return;
  bootstrapLoading.value = true;
  try {
    const answers = inspirationQuestions.map((question) => ({
      questionId: question.id,
      question: question.title,
      answer: inspirationAnswers[question.id] || "",
    }));
    outlineProposals.value = await generateOutlineProposals(store.buildAiSettings(), answers);
    ElMessage.success("方案已生成");
  } catch (error: any) {
    ElMessage.error(error.message || "方案生成失败");
  } finally {
    bootstrapLoading.value = false;
  }
}

async function handleApplyProposal(proposal: OutlineBootstrapProposal) {
  try {
    const envelope = await applyOutlineProposal(proposal);
    store.applyEnvelope(envelope);
    ElMessage.success("方案已应用");
  } catch (error: any) {
    ElMessage.error(error.message || "应用方案失败");
  }
}

async function handleGenerateQuestionnaire() {
  if (!store.aiReady) return;
  questionnaireLoading.value = true;
  try {
    const result = await generateOnboarding(store.buildAiSettings());
    onboardingAnswers.value = result.questions.map((question) => ({
      questionId: question.id,
      question: question.title,
      answer: "",
    }));
    if (store.project) store.project.onboarding.questions = result.questions;
    ElMessage.success("问卷已生成");
  } catch (error: any) {
    ElMessage.error(error.message || "问卷生成失败");
  } finally {
    questionnaireLoading.value = false;
  }
}

async function handleSubmitQuestionnaire() {
  try {
    const project = await submitOnboarding(onboardingAnswers.value);
    if (store.project) store.project = project;
    store.markDirty();
    await store.saveCurrent();
    ElMessage.success("问卷已提交");
  } catch (error: any) {
    ElMessage.error(error.message || "提交失败");
  }
}

async function handleGenerate(continueMode: boolean) {
  if (!currentChapter.value || !store.aiReady) return;
  await store.saveCurrent();
  const abort = new AbortController();
  generationAbort.value = abort;
  generating.value = true;
  generationResult.value = null;
  try {
    await streamNovelChapter(
      currentChapter.value.id,
      store.buildAiSettings(),
      {
        onDelta: (_chunk, accumulated) => {
          if (currentChapter.value) currentChapter.value.draft = accumulated;
        },
        onDone: async (result) => {
          generationResult.value = result;
          await store.loadAll();
          selectedChapterId.value = result.chapterId;
          ElMessage[result.accepted ? "success" : "warning"](result.accepted ? "生成完成" : result.rejectionReason || "生成未通过");
        },
        onCancelled: (message) => ElMessage.info(message),
        onError: (message) => ElMessage.error(message),
      },
      abort.signal,
      continueMode
    );
  } catch (error: any) {
    if (error.name !== "AbortError") ElMessage.error(error.message || "生成失败");
  } finally {
    generating.value = false;
    generationAbort.value = null;
  }
}

function cancelGeneration() {
  generationAbort.value?.abort();
}

async function handleReviewAll() {
  await store.saveCurrent();
  const abort = new AbortController();
  reviewAbort.value = abort;
  reviewResults.value = new Map();
  reviewing.value = true;
  try {
    await streamNovelReview(
      store.aiReady ? store.buildAiSettings() : undefined,
      {
        onProgress: (_chapterId, title, index, total) => {
          store.statusText = `正在审查 ${index + 1}/${total}：${title}`;
        },
        onResult: (chapterId, title, issues) => {
          if (issues.length) {
            const next = new Map(reviewResults.value);
            next.set(chapterId, { title, issues });
            reviewResults.value = next;
          }
        },
        onDone: (reviewedCount, issueCount) => {
          ElMessage.success(`审查完成：${reviewedCount} 章，${issueCount} 个问题`);
        },
        onError: (message) => ElMessage.error(message),
      },
      abort.signal
    );
  } catch (error: any) {
    if (error.name !== "AbortError") ElMessage.error(error.message || "审查失败");
  } finally {
    reviewing.value = false;
    reviewAbort.value = null;
  }
}

function applyIssueFix(chapterId: string, issue: NovelReviewIssue) {
  const chapter = store.project?.chapters.find((item) => item.id === chapterId);
  if (!chapter || !issue.original) return;
  chapter.draft = chapter.draft.replace(issue.original, issue.suggestion || "");
  store.markDirty();
}

async function handleAnalyzeReadingPower() {
  if (!currentChapter.value || !store.activeBookId) return;
  try {
    readingPower.value = await analyzeNovelReadingPower(store.activeBookId, currentChapter.value.order, currentChapter.value.draft);
    ElMessage.success("追读力分析完成");
  } catch (error: any) {
    ElMessage.error(error.message || "追读力分析失败");
  }
}

async function handleCommitChapter() {
  if (!currentChapter.value || !store.activeBookId) return;
  await store.saveCurrent();
  try {
    const text = currentChapter.value.draft || "";
    const lastLine = text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean).at(-1) || "";
    await commitNovelChapter(store.activeBookId, currentChapter.value.order, {
      chapter_text: text,
      summary_text: currentChapter.value.summary,
      extraction_result: {
        chapter_meta: {
          hook: { content: lastLine },
        },
      },
    });
    await loadQualityPanels();
    ElMessage.success("章节已提交");
  } catch (error: any) {
    ElMessage.error(error.message || "提交失败");
  }
}

async function loadQualityPanels() {
  if (!store.activeBookId) return;
  const [rpStats, commitResult, eventResult] = await Promise.allSettled([
    fetchNovelReadingPowerStatsSafe(),
    fetchNovelCommits(store.activeBookId),
    fetchNovelEvents(store.activeBookId),
  ]);
  if (rpStats.status === "fulfilled") readingPowerStats.value = rpStats.value;
  if (commitResult.status === "fulfilled") commits.value = commitResult.value;
  if (eventResult.status === "fulfilled") events.value = eventResult.value;
}

async function fetchNovelReadingPowerStatsSafe() {
  if (!store.activeBookId) return {};
  const { fetchNovelReadingPowerStats } = await import("@/api/novel");
  return fetchNovelReadingPowerStats(store.activeBookId);
}

async function loadSideStats() {
  if (!store.activeBookId) return;
  const [memory, rag] = await Promise.allSettled([
    fetchNovelMemoryStats(store.activeBookId),
    fetchNovelRagStats(store.activeBookId),
  ]);
  if (memory.status === "fulfilled") memoryStats.value = memory.value;
  if (rag.status === "fulfilled") ragStats.value = rag.value;
}

async function loadContextPreview() {
  if (!currentChapter.value || !store.activeBookId) return;
  contextPreview.value = await fetchNovelContext(store.activeBookId, currentChapter.value.order);
}

async function loadMemoryPack() {
  if (!currentChapter.value || !store.activeBookId) return;
  contextPreview.value = await fetchNovelMemoryPack(store.activeBookId, currentChapter.value.order);
}

async function handleRagSearch() {
  if (!ragQuery.value.trim() || !store.activeBookId) return;
  ragResults.value = await searchNovelRag(store.activeBookId, ragQuery.value.trim());
}

async function handleImportOutline() {
  if (!importOutlineText.value.trim()) {
    ElMessage.warning("请先粘贴要导入的大纲文本");
    return;
  }
  importingOutline.value = true;
  try {
    const analysis = await analyzeNovelOutlineImport(importOutlineText.value, store.aiReady ? store.buildAiSettings() : undefined) as ImportAnalysis;
    const result = applyImportAnalysis(analysis);
    importSummary.value = `已合并：${result.outlineCount} 个大纲节点，${result.characterCount} 个角色，${result.foreshadowingCount} 条伏笔`;
    importOutlineText.value = "";
    ElMessage.success("大纲已合并，请保存");
  } catch (error: any) {
    ElMessage.error(error.message || "导入大纲失败");
  } finally {
    importingOutline.value = false;
  }
}

async function handleImportChapters() {
  if (!importChaptersText.value.trim()) {
    ElMessage.warning("请先粘贴要导入的章节文本");
    return;
  }
  importingChapters.value = true;
  try {
    const analysis = await analyzeNovelChaptersImport(importChaptersText.value, store.aiReady ? store.buildAiSettings() : undefined) as ImportAnalysis;
    const result = applyImportAnalysis(analysis);
    importSummary.value = `已合并：${result.chapterCount} 个章节`;
    importChaptersText.value = "";
    selectedChapterId.value = store.sortedChapters.at(-1)?.id || selectedChapterId.value;
    ElMessage.success("章节已合并，请保存");
  } catch (error: any) {
    ElMessage.error(error.message || "导入章节失败");
  } finally {
    importingChapters.value = false;
  }
}

function goSettings() {
  router.push("/settings");
}
</script>

<style scoped>
.novel-workspace {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 280px;
  gap: 14px;
  min-height: calc(100vh - 112px);
}

.library-pane,
.inspector-pane,
.workspace-main,
.empty-main {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.library-pane,
.inspector-pane {
  padding: 14px;
  overflow: hidden;
}

.workspace-main,
.empty-main {
  min-width: 0;
  padding: 16px;
  overflow: auto;
}

.pane-head,
.workspace-toolbar,
.list-toolbar,
.row-title,
.writer-actions,
.inline-actions,
.chapter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.eyebrow {
  margin: 0 0 4px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

h2,
h3,
h4,
p {
  margin: 0;
}

.book-list,
.chapter-list,
.editable-list,
.proposal-list,
.rag-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.book-list {
  margin: 14px 0 18px;
}

.book-row,
.chapter-row {
  width: 100%;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  padding: 10px;
  text-align: left;
  color: var(--color-text);
  cursor: pointer;
  transition: border-color 0.2s var(--transition-smooth), background 0.2s var(--transition-smooth);
}

.book-row.active,
.chapter-row.active {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
}

.book-title {
  display: block;
  font-size: 14px;
  font-weight: 650;
}

.book-meta,
.chapter-row small,
.inspector-block small {
  display: block;
  margin-top: 4px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.chapter-head {
  padding-top: 8px;
  border-top: 1px solid var(--color-border);
  color: var(--color-text);
  font-weight: 650;
}

.workspace-toolbar {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.workspace-tabs {
  margin-top: 10px;
}

.panel-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.overview-grid,
.quality-grid,
.knowledge-grid,
.split-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.8fr);
  gap: 14px;
}

.form-panel,
.stats-panel,
.proposal-panel,
.editor-panel,
.inspector-block,
.edit-row,
.review-entry,
.rag-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  padding: 14px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--color-text);
  font-weight: 650;
}

.import-panel {
  grid-column: 1 / -1;
}

.import-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.import-button {
  margin-top: 8px;
}

.import-summary {
  margin-top: 10px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.two-cols,
.chapter-bindings {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.stats-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  align-content: start;
}

.stat-item {
  padding: 14px;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
}

.stat-item strong {
  display: block;
  color: var(--color-text);
  font-size: 24px;
}

.stat-item span {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.question-list,
.question-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.question-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--color-text);
  font-size: 13px;
  font-weight: 600;
}

.proposal-panel p,
.rag-item p,
.inspector-block p {
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.proposal-meta {
  display: flex;
  gap: 6px;
  margin: 10px 0;
}

.edit-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.writing-layout {
  min-height: 620px;
}

.editor-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.draft-input :deep(.el-textarea__inner) {
  min-height: 460px !important;
  line-height: 1.8;
  font-size: 15px;
}

.word-count {
  margin-left: auto;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.compact-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.review-entry {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.issue-row {
  border-left: 3px solid var(--color-primary);
  padding-left: 10px;
}

.issue-row p {
  color: var(--color-text);
}

.issue-row small {
  display: block;
  margin: 4px 0 8px;
  color: var(--color-text-secondary);
}

.json-preview {
  min-height: 180px;
  max-height: 430px;
  overflow: auto;
  margin: 0;
  padding: 12px;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.55;
}

.inspector-pane {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 1280px) {
  .novel-workspace {
    grid-template-columns: 230px minmax(0, 1fr);
  }
  .inspector-pane {
    display: none;
  }
}

@media (max-width: 860px) {
  .novel-workspace,
  .overview-grid,
  .quality-grid,
  .knowledge-grid,
  .split-main,
  .question-list,
  .question-grid,
  .import-grid,
  .two-cols,
  .chapter-bindings {
    grid-template-columns: 1fr;
  }
  .library-pane {
    max-height: 360px;
    overflow: auto;
  }
}
</style>
