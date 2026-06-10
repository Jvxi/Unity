# AI 模型来源

本项目的默认 AI 厂商与模型只添加已在官方文档中出现、且能通过当前后端协议调用的模型。

核实日期：2026-06-10。

| 厂商 | 后端协议 | 来源 / 接口 |
| --- | --- | --- |
| DeepSeek | OpenAI Chat Completions 兼容 | https://api-docs.deepseek.com/quick_start/pricing |
| OpenAI | Responses API | https://platform.openai.com/docs/models |
| Anthropic Claude | Messages API | https://docs.anthropic.com/en/docs/about-claude/models/overview |
| Google Gemini | OpenAI Chat Completions 兼容 | https://ai.google.dev/gemini-api/docs/openai |
| 阿里云百炼 / Qwen | OpenAI Chat Completions 兼容 | https://help.aliyun.com/zh/model-studio/getting-started/models |
| 小米 MiMo | OpenAI Chat Completions 兼容 | https://api.xiaomimimo.com/v1/chat/completions |
| 智谱 GLM | OpenAI Chat Completions 兼容 | https://docs.bigmodel.cn/cn/guide/start/model-overview |
| Moonshot Kimi | OpenAI Chat Completions 兼容 | https://platform.moonshot.ai/docs/pricing/chat |
| xAI Grok | OpenAI Chat Completions 兼容 | https://docs.x.ai/docs/models |
| 自定义厂商 | OpenAI Chat Completions 兼容 | 用户在前端设置页填写接口链接和模型 ID |

注意事项：

- 百炼里的 MiMo、Kimi、GLM 是通过阿里云百炼 API Key 调用的第三方模型；小米 MiMo 独立厂商入口使用小米 MiMo API Key。
- 自定义厂商不对应固定官方厂商，主要用于兼容中转站、代理网关或自建 OpenAI 兼容接口。
- 桌面端设置页只有一个 API Key 输入框，用户需要按当前选择的厂商填写对应厂商的 Key。
- 如果厂商官方下线或重命名模型，应同步更新后端 `DeepSeekService.defaultProviders()` 和前端 `SettingsView.vue` 的兜底列表。
