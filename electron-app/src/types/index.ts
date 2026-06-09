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
  models: ModelInfo[];
}

export interface SettingsState {
  apiKey: string;
  selectedProvider: string;
  selectedModel: string;
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