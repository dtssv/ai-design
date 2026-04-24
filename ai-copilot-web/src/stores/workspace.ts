import request from '@/utils/request';
import { create } from 'zustand';

export interface Workspace {
    id: number;
    name: string;
    description?: string;
    teamId?: number;
    generationMode: string;
    status: string;
    createdAt: string;
}

export interface Conversation {
    id: number;
    title: string;
    workspaceId: number;
    generationMode: string;
    createdAt: string;
}

export interface Message {
    id: number;
    conversationId: number;
    role: 'user' | 'assistant';
    content: string;
    codeSnapshotId?: number;
    tokenUsage?: number;
    modelUsed?: string;
    createdAt: string;
}

export interface SnapshotMeta {
    id: number;
    version: number;
    generationMode: string;
    fileCount: number;
    createdAt: string;
}

export interface CodeFile {
    path: string;
    language: string;
    content: string;
    action?: string;
}

export interface ModelOption {
    provider: string;
    modelName: string;
}

export interface ApiKeyOption {
    id: number;
    name: string;
    provider: string;
    modelName: string;
    teamId?: number;
    teamName?: string;
}

export interface PlanOption {
    id: number;
    name: string;
    type: string;
    targetType: string;
    monthlyTokenLimit: number;
    description?: string;
}

export interface GenerationOptions {
    platformModels: ModelOption[];
    personalKeys: ApiKeyOption[];
    teamKeys: ApiKeyOption[];
    plans: PlanOption[];
}

export interface KnowledgeBaseItem {
    id: number;
    name: string;
    description?: string;
    source: 'personal' | 'team' | 'public';
    type: string;
    ownerName: string;
    teamName?: string;
    teamId?: number;
    usageCount: number;
    createdAt?: string;
}

interface WorkspaceState {
    workspaces: Workspace[];
    currentWorkspace: Workspace | null;
    conversations: Conversation[];
    currentConversation: Conversation | null;
    messages: Message[];
    snapshots: SnapshotMeta[];
    currentSnapshotId: number | null;
    currentFiles: CodeFile[];
    selectedFilePath: string | null;
    loading: boolean;
    generationOptions: GenerationOptions | null;
    visibleKnowledgeBases: KnowledgeBaseItem[];

    fetchWorkspaces: () => Promise<void>;
    setCurrentWorkspace: (ws: Workspace) => void;
    createWorkspace: (data: Partial<Workspace>) => Promise<Workspace>;
    fetchConversations: (workspaceId: number) => Promise<void>;
    setCurrentConversation: (conv: Conversation) => void;
    fetchMessages: (conversationId: number) => Promise<void>;
    addMessage: (msg: Message) => void;
    fetchSnapshots: (conversationId: number) => Promise<void>;
    loadSnapshot: (snapshotId: number) => Promise<void>;
    setSelectedFilePath: (path: string | null) => void;
    setCurrentFilesFromJson: (filesJson: string) => void;
    fetchGenerationOptions: () => Promise<void>;
    fetchVisibleKnowledgeBases: () => Promise<void>;
}

export const useWorkspaceStore = create<WorkspaceState>((set, get) => ({
    workspaces: [],
    currentWorkspace: null,
    conversations: [],
    currentConversation: null,
    messages: [],
    snapshots: [],
    currentSnapshotId: null,
    currentFiles: [],
    selectedFilePath: null,
    loading: false,
    generationOptions: null,
    visibleKnowledgeBases: [],

    fetchWorkspaces: async () => {
        set({ loading: true });
        const res: any = await request.get('/workspaces');
        set({ workspaces: res.data, loading: false });
    },

    setCurrentWorkspace: (ws) => set({ currentWorkspace: ws }),

    createWorkspace: async (data) => {
        const res: any = await request.post('/workspaces', data);
        const ws = res.data;
        set({ workspaces: [...get().workspaces, ws] });
        return ws;
    },

    fetchConversations: async (workspaceId) => {
        const res: any = await request.get(`/workspaces/${workspaceId}/conversations`);
        set({ conversations: res.data });
    },

    setCurrentConversation: (conv) =>
        set({ currentConversation: conv, snapshots: [], currentSnapshotId: null, currentFiles: [], selectedFilePath: null }),

    fetchMessages: async (conversationId) => {
        const res: any = await request.get(`/conversations/${conversationId}/messages`);
        set({ messages: res.data });
    },

    addMessage: (msg) => set({ messages: [...get().messages, msg] }),

    fetchSnapshots: async (conversationId) => {
        const res: any = await request.get(`/conversations/${conversationId}/snapshots`);
        set({ snapshots: res.data });
    },

    loadSnapshot: async (snapshotId) => {
        const res: any = await request.get(`/snapshots/${snapshotId}`);
        const snapshot = res.data;
        let files: CodeFile[] = [];
        if (snapshot.files) {
            try { files = JSON.parse(snapshot.files); } catch { files = []; }
        }
        set({ currentSnapshotId: snapshotId, currentFiles: files, selectedFilePath: files.length > 0 ? files[0].path : null });
    },

    setSelectedFilePath: (path) => set({ selectedFilePath: path }),

    setCurrentFilesFromJson: (filesJson) => {
        try {
            const files: CodeFile[] = JSON.parse(filesJson);
            set({ currentFiles: files, selectedFilePath: files.length > 0 ? files[0].path : null });
        } catch {
            set({ currentFiles: [], selectedFilePath: null });
        }
    },

    fetchGenerationOptions: async () => {
        const res: any = await request.get('/generation/options');
        set({ generationOptions: res.data });
    },

    fetchVisibleKnowledgeBases: async () => {
        const res: any = await request.get('/knowledge-bases/visible');
        set({ visibleKnowledgeBases: res.data });
    },
}));