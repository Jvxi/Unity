export interface SectionInfo {
  name: string;
  rva: string;
  virtualAddress: number;
  virtualSize: number;
  rawSize: number;
  rawPointer: number;
  characteristics: string;
}

export interface ExportInfo {
  name: string;
  rva: string;
  rvaValue: number;
  ordinal: number;
  forwarder: boolean;
  forwarderName: string;
}

export interface ImportFunction {
  name: string;
  hint: number;
  thunkRva: string;
}

export interface ImportInfo {
  dllName: string;
  functions: ImportFunction[];
}

export interface DebugInfo {
  type: string;
  pdbPath: string;
  guid: string;
  age: number;
}

export interface DataDirectoryEntry {
  name: string;
  index: number;
  rva: string;
  rvaValue: number;
  size: number;
  present: boolean;
}

export interface PeInfo {
  fileName: string;
  fileSize: number;
  machine: string;
  numberOfSections: number;
  timeDateStamp: number;
  characteristics: string[];
  magic: string;
  imageBase: number;
  sectionAlignment: number;
  fileAlignment: number;
  subsystem: string;
  sizeOfImage: number;
  sizeOfHeaders: number;
  checkSum: number;
  dllCharacteristics: string[];
  sections: SectionInfo[];
  exportCount: number;
  exports: ExportInfo[];
  importCount: number;
  imports: ImportInfo[];
  debugInfo: DebugInfo[];
  tlsCallbacks: number[];
  dataDirectories: DataDirectoryEntry[];
  hasCertificate: boolean;
  certificateInfo: string;
  coffSymbols: any[];
  guardCFFunctions: number;
  parserSources: string[];
}

export interface VFunctionInfo {
  index: number;
  rva: string;
  rvaValue: number;
  va: string;
  vaValue: number;
  note: string;
}

export interface VtableInfo {
  rva: string;
  rvaValue: number;
  va: string;
  vaValue: number;
  functionCount: number;
  detectionMethod: string;
  relatedSymbol: string;
  rttiTypeName: string;
  functions: VFunctionInfo[];
  aiNote: string;
}

export interface AnalysisResult {
  peInfo: PeInfo;
  vtables: VtableInfo[];
  aiSummary: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

export interface ModelInfo {
  id: string;
  name: string;
  description: string;
}

export interface ProviderInfo {
  id: string;
  name: string;
  apiUrl?: string;
  apiFormat?: string;
  models: ModelInfo[];
}

export interface NovelAiSettings {
  enabled: boolean;
  provider: string;
  baseUrl: string;
  apiKey: string;
  model: string;
  temperature: number;
  maxTokens: number;
  contextWindowSize: number;
  systemPrompt: string;
}

export interface NovelProjectMeta {
  title: string;
  synopsis: string;
  genre: string;
  premise: string;
  tone: string;
  targetLength: string;
  styleRules: string[];
  worldRules: string[];
  strictMode: boolean;
  publishPlatform: string;
  audienceChannel: string;
  novelType: string;
}

export interface NovelAudienceInfo {
  id: string;
  label: string;
  description: string;
}

export interface NovelTypeInfo {
  id: string;
  label: string;
  audienceChannel: string;
  description: string;
  writingHints: string[];
}

export interface NovelTypeCatalogResponse {
  audiences: NovelAudienceInfo[];
  types: NovelTypeInfo[];
}

export interface PublishPlatformInfo {
  id: string;
  label: string;
  description: string;
  writingRules: string[];
}

export interface NovelOutlineNode {
  id: string;
  order: number;
  title: string;
  summary: string;
  objective: string;
  keyConflict: string;
  mustKeep: string[];
  forbidden: string[];
}

export interface NovelCharacterProfile {
  id: string;
  name: string;
  role: string;
  profile: string;
  motivation: string;
  constraint: string;
  relationships: string;
}

export interface NovelForeshadowingItem {
  id: string;
  title: string;
  setup: string;
  payoff: string;
  plannedReveal: string;
  status: string;
  notes: string;
}

export interface NovelChapter {
  id: string;
  order: number;
  title: string;
  summary: string;
  purpose: string;
  outlineNodeIds: string[];
  characterIds: string[];
  foreshadowingIds: string[];
  mandatoryBeats: string[];
  forbiddenContent: string[];
  notes: string;
  draft: string;
}

export interface OnboardingQuestion {
  id: string;
  title: string;
  hint: string;
  placeholder: string;
}

export interface OnboardingAnswer {
  questionId: string;
  question: string;
  answer: string;
}

export interface NovelOnboardingState {
  completed: boolean;
  questions: OnboardingQuestion[];
  answers: OnboardingAnswer[];
}

export interface NovelProject {
  meta: NovelProjectMeta;
  aiSettings: NovelAiSettings;
  onboarding: NovelOnboardingState;
  outlineNodes: NovelOutlineNode[];
  characters: NovelCharacterProfile[];
  foreshadowing: NovelForeshadowingItem[];
  chapters: NovelChapter[];
  updatedAt: string;
}

export interface NovelBookSummary {
  id: string;
  title: string;
  genre: string;
  updatedAt: string;
  chapterCount: number;
  onboardingCompleted: boolean;
}

export interface NovelLibraryIndex {
  activeBookId: string;
  books: NovelBookSummary[];
}

export interface NovelProjectEnvelope {
  bookId: string;
  project: NovelProject;
}

export interface NovelComplianceReport {
  passed: boolean;
  metaLabelHits: string[];
  narrationMetaHits: string[];
  missingMandatoryBeats: string[];
  forbiddenHits: string[];
  missingOutlineAnchors: string[];
  missingChapterAnchors: string[];
  groundedOutlineTitles: string[];
  groundedCharacterNames: string[];
}

export interface NovelChapterGenerationResponse {
  chapterId: string;
  provider: string;
  accepted: boolean;
  draft: string;
  promptPreview: string;
  compliance: NovelComplianceReport;
  rejectionReason: string;
  warnings: string[];
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
}

export interface NovelReviewIssue {
  original: string;
  description: string;
  suggestion: string;
}

export interface OutlineBootstrapProposal {
  id: string;
  name: string;
  pitch: string;
  premise: string;
  tone: string;
  targetLength: string;
  styleRules: string[];
  worldRules: string[];
  outlineNodes: Array<{
    title: string;
    summary: string;
    objective: string;
    keyConflict: string;
    mustKeep: string[];
    forbidden: string[];
  }>;
  characters: Array<{
    name: string;
    role: string;
    profile: string;
    motivation: string;
    constraint: string;
    relationships: string;
  }>;
}

export interface NovelRagResult {
  chunkId: string;
  text: string;
  score: number;
  sourceType?: string;
  chapterNumber?: number;
}

export interface NovelMemoryPack {
  workingMemory?: unknown[];
  episodicMemory?: unknown[];
  semanticMemory?: unknown[];
  scratchpad?: unknown;
  [key: string]: unknown;
}

export interface NovelReadingPower {
  chapterNumber?: number;
  overallScore?: number;
  hookScore?: number;
  pacingScore?: number;
  emotionScore?: number;
  suspenseScore?: number;
  suggestions?: string[];
  [key: string]: unknown;
}

export interface NovelCommitRecord {
  bookId?: string;
  chapterNumber?: number;
  status?: string;
  createdAt?: string;
  [key: string]: unknown;
}

export interface NovelStoryEvent {
  eventId?: string;
  eventType?: string;
  subject?: string;
  chapterNumber?: number;
  description?: string;
  [key: string]: unknown;
}

export interface SettingsState {
  apiKey: string;
  selectedProvider: string;
  selectedModel: string;
  aiApiUrl: string;
  menuCollapsed: boolean;
}

// Chat types
export interface ChatMessage {
  id: number;
  senderId: number;
  receiverId?: number;
  groupId?: number;
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM';
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
  createdAt: string;
  recalled: boolean;
  senderNickname?: string;
  senderAvatarUrl?: string;
}

export interface ChatSession {
  id: number;
  type: 'PRIVATE' | 'GROUP';
  targetUserId?: number;
  targetGroupId?: number;
  targetNickname?: string;
  targetAvatarUrl?: string;
  targetOnlineStatus?: string;
  lastMessageId?: number;
  lastMessageContent?: string;
  lastMessageSenderId?: number;
  lastMessageType?: string;
  unreadCount: number;
  updatedAt: string;
}

export interface ChatGroup {
  id: number;
  name: string;
  avatarUrl: string;
  ownerId: number;
  memberCount: number;
  createdAt: string;
}

export interface ChatGroupMember {
  id: number;
  userId: number;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  nickname: string;
  avatarUrl: string;
  onlineStatus: string;
  mutedUntil?: string;
  joinedAt: string;
}

export interface UserProfile {
  id: number;
  nickname: string;
  email: string;
  avatarUrl: string;
  bio: string;
  gender: string;
  birthday: string;
  onlineStatus: string;
  createdAt: string;
}

export interface UserSearchResult {
  id: number;
  nickname: string;
  avatarUrl: string;
  onlineStatus: string;
}
