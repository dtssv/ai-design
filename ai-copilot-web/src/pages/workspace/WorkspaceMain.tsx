import type { CodeFile } from '@/stores/workspace';
import { useWorkspaceStore } from '@/stores/workspace';
import type { SseEventData } from '@/utils/request';
import request, { downloadFile, fetchSSE } from '@/utils/request';
import { buildSandboxHtml } from '@/utils/sandboxBuilder';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import styles from './WorkspaceMain.module.css';

/** 分享信息 */
interface ShareInfo {
    shareToken: string;
    shareUrl: string;
    mcpEndpoint: string;
    mcpToken: string;
    snapshotId?: number;
    version?: number;
}

/** 附件项 */
interface Attachment {
    url: string;
    name: string;
    size: number;
    type: 'image' | 'file';
}

/** 结构化代码输出标记 */
const CODE_OUTPUT_START = '<<<AI_CODE_OUTPUT>>>';
const CODE_OUTPUT_END = '<<<END_AI_CODE_OUTPUT>>>';

const SOURCE_LABEL: Record<string, string> = { personal: '个人', team: '团队', public: '公共' };

/** 从流式文本中尝试解析结构化代码文件 */
function parseCodeFilesFromStream(text: string): CodeFile[] | null {
    const startIdx = text.indexOf(CODE_OUTPUT_START);
    const endIdx = text.indexOf(CODE_OUTPUT_END);
    if (startIdx < 0 || endIdx <= startIdx) return null;
    const jsonBlock = text.substring(startIdx + CODE_OUTPUT_START.length, endIdx).trim();
    try {
        const files: Array<{ path: string; language?: string; content?: string; action?: string }> = JSON.parse(jsonBlock);
        return files
            .filter(f => f.action !== 'delete')
            .map(f => ({
                path: f.path,
                language: f.language || 'txt',
                content: f.content || '',
                action: f.action || 'create',
            }));
    } catch {
        return null;
    }
}

/** 从AI回复中提取修改说明（代码标记之前的文字） */
function extractExplanation(text: string): string {
    const startIdx = text.indexOf(CODE_OUTPUT_START);
    if (startIdx > 0) return text.substring(0, startIdx).trim();
    return text;
}

export default function WorkspaceMain() {
    const { id } = useParams<{ id: string }>();
    const {
        conversations, currentConversation, messages, snapshots,
        currentSnapshotId, currentFiles, selectedFilePath,
        generationOptions, visibleKnowledgeBases,
        fetchConversations, setCurrentConversation, fetchMessages,
        addMessage, fetchSnapshots, loadSnapshot, setSelectedFilePath,
        fetchGenerationOptions, fetchVisibleKnowledgeBases,
    } = useWorkspaceStore();

    const [input, setInput] = useState('');
    const [generating, setGenerating] = useState(false);
    const [streamText, setStreamText] = useState('');
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const iframeRef = useRef<HTMLIFrameElement>(null);

    // ========== 右侧面板 Tab ==========
    const [rightTab, setRightTab] = useState<'code' | 'preview'>('code');

    // ========== 流式解析出的代码文件（实时） ==========
    const [streamFiles, setStreamFiles] = useState<CodeFile[]>([]);

    // ========== Key / 模型选择 ==========
    const [apiKeySource, setApiKeySource] = useState<'platform' | 'personal' | 'team'>('platform');
    const [selectedKeyId, setSelectedKeyId] = useState<number | null>(null);
    const [selectedModel, setSelectedModel] = useState<string>('');

    // ========== 附件上传 ==========
    const [attachments, setAttachments] = useState<Attachment[]>([]);
    const [uploading, setUploading] = useState(false);
    const imageInputRef = useRef<HTMLInputElement>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    // ========== 知识库选择 ==========
    const [showKbPicker, setShowKbPicker] = useState(false);
    const [selectedKbIds, setSelectedKbIds] = useState<number[]>([]);
    const [kbFilter, setKbFilter] = useState<'all' | 'personal' | 'team' | 'public'>('all');

    useEffect(() => { if (id) fetchConversations(Number(id)); }, [id]);
    useEffect(() => { fetchGenerationOptions(); fetchVisibleKnowledgeBases(); }, []);
    useEffect(() => {
        if (currentConversation) { fetchMessages(currentConversation.id); fetchSnapshots(currentConversation.id); }
    }, [currentConversation?.id]);
    useEffect(() => { if (snapshots.length > 0 && !currentSnapshotId) loadSnapshot(snapshots[0].id); }, [snapshots]);
    useEffect(() => { messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages, streamText]);

    // 流式文本变化时实时解析代码文件
    useEffect(() => {
        if (!streamText) { setStreamFiles([]); return; }
        const parsed = parseCodeFilesFromStream(streamText);
        if (parsed && parsed.length > 0) {
            setStreamFiles(parsed);
        }
    }, [streamText]);

    const availableModels = useMemo(() => {
        if (!generationOptions) return [];
        if (apiKeySource === 'platform') {
            return generationOptions.platformModels.map(m => ({ label: `${m.provider} / ${m.modelName}`, value: m.modelName }));
        }
        const keys = apiKeySource === 'personal' ? generationOptions.personalKeys : generationOptions.teamKeys;
        const key = keys.find(k => k.id === selectedKeyId);
        return key ? [{ label: `${key.provider} / ${key.modelName}`, value: key.modelName }] : [];
    }, [generationOptions, apiKeySource, selectedKeyId]);

    useEffect(() => { setSelectedKeyId(null); setSelectedModel(''); }, [apiKeySource]);

    const filteredKbs = useMemo(() => {
        if (kbFilter === 'all') return visibleKnowledgeBases;
        return visibleKnowledgeBases.filter(kb => kb.source === kbFilter);
    }, [visibleKnowledgeBases, kbFilter]);

    const toggleKb = (kbId: number) => {
        setSelectedKbIds(prev => prev.includes(kbId) ? prev.filter(x => x !== kbId) : [...prev, kbId]);
    };

    const handleUpload = async (file: File, type: 'image' | 'file') => {
        setUploading(true);
        try {
            const form = new FormData();
            form.append('file', file);
            form.append('type', type);
            const res: any = await request.post('/uploads', form, { headers: { 'Content-Type': 'multipart/form-data' } });
            setAttachments(prev => [...prev, { url: res.data.url, name: res.data.name, size: res.data.size, type }]);
        } catch (err: any) { alert(err?.message || '上传失败'); }
        finally { setUploading(false); }
    };
    const removeAttachment = (idx: number) => setAttachments(prev => prev.filter((_, i) => i !== idx));

    const createConversation = async () => {
        const res: any = await request.post(`/workspaces/${id}/conversations`, { title: '新对话' });
        await fetchConversations(Number(id));
        setCurrentConversation(res.data);
    };

    const handleSend = useCallback(async () => {
        if (!input.trim() || generating) return;
        let conv = currentConversation;
        if (!conv) {
            const res: any = await request.post(`/workspaces/${id}/conversations`, { title: '新对话' });
            await fetchConversations(Number(id)); conv = res.data; setCurrentConversation(conv!);
        }
        const convId = conv!.id;

        if (apiKeySource !== 'platform' || selectedKeyId) {
            await request.put(`/conversations/${convId}/api-key`, { api_key_source: apiKeySource, api_key_id: selectedKeyId });
        }

        const imageUrls = attachments.filter(a => a.type === 'image').map(a => a.url);
        const fileUrls = attachments.filter(a => a.type === 'file').map(a => a.url);
        const userMsg = { id: Date.now(), conversationId: convId, role: 'user' as const, content: input, createdAt: new Date().toISOString() };
        addMessage(userMsg);
        setInput(''); setAttachments([]); setGenerating(true); setStreamText(''); setStreamFiles([]);

        let fullText = '';
        fetchSSE(
            `/conversations/${convId}/generate`,
            {
                content: input,
                modelName: selectedModel || undefined,
                imageUrls: imageUrls.length ? imageUrls : undefined,
                fileUrls: fileUrls.length ? fileUrls : undefined,
                knowledgeBaseIds: selectedKbIds.length ? selectedKbIds : undefined,
            },
            (event: SseEventData) => {
                if (event.type === 'text_delta' && event.content) { fullText += event.content; setStreamText(fullText); }
                else if (event.type === 'done') {
                    addMessage({
                        id: event.messageId || Date.now() + 1, conversationId: convId, role: 'assistant',
                        content: fullText, codeSnapshotId: event.snapshotId, tokenUsage: event.tokenUsage,
                        modelUsed: event.model, createdAt: new Date().toISOString(),
                    });
                    setStreamText(''); setGenerating(false); setStreamFiles([]);
                    fetchSnapshots(convId).then(() => { if (event.snapshotId) loadSnapshot(event.snapshotId); });
                }
            },
            () => setGenerating(false),
            () => setGenerating(false),
        );
    }, [input, generating, currentConversation, id, apiKeySource, selectedKeyId, selectedModel, attachments, selectedKbIds]);

    const handleKeyDown = (e: React.KeyboardEvent) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); } };

    const displayFiles = useMemo(() => {
        if (streamFiles.length > 0) return streamFiles;
        return currentFiles;
    }, [streamFiles, currentFiles]);

    const fileTree = useMemo(() => buildFileTree(displayFiles), [displayFiles]);
    const selectedFile = useMemo(() => displayFiles.find(f => f.path === selectedFilePath) || null, [displayFiles, selectedFilePath]);
    const sandboxHtml = useMemo(() => buildSandboxHtml(displayFiles), [displayFiles]);

    const handleDownload = async () => {
        if (currentSnapshotId) {
            try {
                await downloadFile(`/snapshots/${currentSnapshotId}/download`, `code-v${snapshots.find(s => s.id === currentSnapshotId)?.version || 0}.zip`);
            } catch (err: any) {
                alert(err?.message || '下载失败');
            }
        }
    };
    const handleRefreshPreview = () => {
        if (iframeRef.current && sandboxHtml) iframeRef.current.srcdoc = sandboxHtml;
    };

    // ========== 分享功能 ==========
    const [shareModalOpen, setShareModalOpen] = useState(false);
    const [shareInfo, setShareInfo] = useState<ShareInfo | null>(null);
    const [shareLoading, setShareLoading] = useState(false);
    const [shareCopied, setShareCopied] = useState<'link' | 'mcp' | null>(null);

    const handleShare = async () => {
        if (!id) return;
        setShareLoading(true);
        setShareModalOpen(true);
        try {
            const payload: Record<string, unknown> = {};
            if (currentSnapshotId) {
                payload.snapshotId = currentSnapshotId;
            }
            const res: any = await request.post(`/workspaces/${id}/share`, payload);
            const data = res.data;
            const origin = window.location.origin;
            // 带用户 MCP 令牌的 MCP 服务端点（绑定到当前用户）
            const mcpEndpointUrl = `${origin}/api/v1/mcp/${data.mcpToken}`;
            setShareInfo({
                shareToken: data.shareToken,
                shareUrl: `${origin}/share/${data.shareToken}`,
                mcpEndpoint: mcpEndpointUrl,
                mcpToken: data.mcpToken,
                snapshotId: data.snapshotId,
                version: data.version,
            });
        } catch (err: any) {
            alert(err?.message || '分享失败');
            setShareModalOpen(false);
        } finally {
            setShareLoading(false);
        }
    };

    const handleCopyLink = async (text: string, type: 'link' | 'mcp') => {
        try {
            await navigator.clipboard.writeText(text);
            setShareCopied(type);
            setTimeout(() => setShareCopied(null), 2000);
        } catch {
            // fallback
            const textarea = document.createElement('textarea');
            textarea.value = text;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
            setShareCopied(type);
            setTimeout(() => setShareCopied(null), 2000);
        }
    };

    return (
        <div className={styles.layout}>
            {/* 左侧对话列表 */}
            <div className={styles.sidebar}>
                <div className={styles.sidebarHeader}>
                    <h3>对话</h3>
                    <button className="btn-primary" style={{ padding: '4px 10px', fontSize: 12 }} onClick={createConversation}>+</button>
                </div>
                <div className={styles.convList}>
                    {conversations.map(conv => (
                        <div key={conv.id} className={`${styles.convItem} ${currentConversation?.id === conv.id ? styles.convActive : ''}`}
                            onClick={() => setCurrentConversation(conv)}>
                            <span className={styles.convTitle}>{conv.title}</span>
                            <span className={styles.convDate}>{new Date(conv.createdAt).toLocaleDateString()}</span>
                        </div>
                    ))}
                    {conversations.length === 0 && <div className={styles.convEmpty}>暂无对话，点击 + 创建</div>}
                </div>
            </div>

            {/* 中间对话区 */}
            <div className={styles.chat}>
                <div className={styles.chatMessages}>
                    {messages.map(msg => (
                        <div key={msg.id} className={`${styles.message} ${msg.role === 'user' ? styles.messageUser : styles.messageAi}`}>
                            <div className={styles.messageAvatar}>{msg.role === 'user' ? 'U' : 'AI'}</div>
                            <div className={styles.messageBubble}>
                                <pre className={styles.messageContent}>
                                    {msg.role === 'assistant' ? extractExplanation(msg.content) : msg.content}
                                </pre>
                                {msg.role === 'assistant' && msg.codeSnapshotId && (
                                    <button className={styles.snapshotLink} onClick={() => loadSnapshot(msg.codeSnapshotId!)}>查看此版本代码</button>
                                )}
                            </div>
                        </div>
                    ))}
                    {generating && streamText && (
                        <div className={`${styles.message} ${styles.messageAi}`}>
                            <div className={styles.messageAvatar}>AI</div>
                            <div className={styles.messageBubble}>
                                <pre className={styles.messageContent}>{extractExplanation(streamText)}<span className={styles.cursor}>|</span></pre>
                                {streamFiles.length > 0 && (
                                    <div className={styles.streamFilesHint}>已识别 {streamFiles.length} 个代码文件，实时渲染到右侧 →</div>
                                )}
                            </div>
                        </div>
                    )}
                    <div ref={messagesEndRef} />
                </div>

                {/* 输入区 */}
                <div className={styles.chatInputWrapper}>
                    <div className={styles.optionsBar}>
                        <select className={styles.optionSelect} value={apiKeySource} onChange={e => setApiKeySource(e.target.value as any)}>
                            <option value="platform">默认套餐</option><option value="personal">个人 Key</option><option value="team">团队 Key</option>
                        </select>
                        {apiKeySource === 'personal' && generationOptions && (
                            <select className={styles.optionSelect} value={selectedKeyId ?? ''} onChange={e => { setSelectedKeyId(Number(e.target.value) || null); setSelectedModel(''); }}>
                                <option value="">选择 Key</option>
                                {generationOptions.personalKeys.map(k => <option key={k.id} value={k.id}>{k.name} ({k.provider}/{k.modelName})</option>)}
                            </select>
                        )}
                        {apiKeySource === 'team' && generationOptions && (
                            <select className={styles.optionSelect} value={selectedKeyId ?? ''} onChange={e => { setSelectedKeyId(Number(e.target.value) || null); setSelectedModel(''); }}>
                                <option value="">选择 Key</option>
                                {generationOptions.teamKeys.map(k => <option key={k.id} value={k.id}>{k.teamName} - {k.name} ({k.modelName})</option>)}
                            </select>
                        )}
                        {availableModels.length > 0 && (
                            <select className={styles.optionSelect} value={selectedModel} onChange={e => setSelectedModel(e.target.value)}>
                                <option value="">默认模型</option>
                                {availableModels.map(m => <option key={m.value} value={m.value}>{m.label}</option>)}
                            </select>
                        )}
                        <div className={styles.optionSpacer} />
                        <button className={`${styles.iconBtn} ${selectedKbIds.length > 0 ? styles.iconBtnActive : ''}`}
                            title="选择知识库" onClick={() => setShowKbPicker(!showKbPicker)}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                            </svg>
                            {selectedKbIds.length > 0 && <span className={styles.badge}>{selectedKbIds.length}</span>}
                        </button>
                        <button className={styles.iconBtn} title="上传图片" onClick={() => imageInputRef.current?.click()} disabled={uploading}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><path d="m21 15-5-5L5 21" />
                            </svg>
                        </button>
                        <button className={styles.iconBtn} title="上传文件" onClick={() => fileInputRef.current?.click()} disabled={uploading}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
                            </svg>
                        </button>
                        <input ref={imageInputRef} type="file" accept="image/*" hidden onChange={e => { const f = e.target.files?.[0]; if (f) handleUpload(f, 'image'); e.target.value = ''; }} />
                        <input ref={fileInputRef} type="file" hidden onChange={e => { const f = e.target.files?.[0]; if (f) handleUpload(f, 'file'); e.target.value = ''; }} />
                    </div>

                    {showKbPicker && (
                        <div className={styles.kbPicker}>
                            <div className={styles.kbPickerHeader}>
                                <span className={styles.kbPickerTitle}>选择知识库</span>
                                <button className={styles.kbPickerClose} onClick={() => setShowKbPicker(false)}>x</button>
                            </div>
                            <div className={styles.kbFilterRow}>
                                {(['all', 'personal', 'team', 'public'] as const).map(f => (
                                    <button key={f} className={`${styles.kbFilterBtn} ${kbFilter === f ? styles.kbFilterActive : ''}`} onClick={() => setKbFilter(f)}>
                                        {f === 'all' ? '全部' : SOURCE_LABEL[f]}
                                    </button>
                                ))}
                            </div>
                            <div className={styles.kbList}>
                                {filteredKbs.length === 0 && <div className={styles.kbEmpty}>暂无知识库</div>}
                                {filteredKbs.map(kb => (
                                    <label key={kb.id} className={styles.kbItem}>
                                        <input type="checkbox" checked={selectedKbIds.includes(kb.id)} onChange={() => toggleKb(kb.id)} />
                                        <div className={styles.kbItemBody}>
                                            <div className={styles.kbItemTop}>
                                                <span className={styles.kbName}>{kb.name}</span>
                                                <span className={styles.kbSource}>{SOURCE_LABEL[kb.source] || kb.source}{kb.teamName ? ` · ${kb.teamName}` : ''}</span>
                                            </div>
                                            {kb.description && <div className={styles.kbDesc}>{kb.description}</div>}
                                            <div className={styles.kbMeta}>
                                                <span>创建人: {kb.ownerName}</span>
                                                <span>使用 {kb.usageCount ?? 0} 次</span>
                                            </div>
                                        </div>
                                    </label>
                                ))}
                            </div>
                        </div>
                    )}

                    {attachments.length > 0 && (
                        <div className={styles.attachList}>
                            {attachments.map((a, i) => (
                                <div key={i} className={styles.attachItem}>
                                    {a.type === 'image' ? <img src={a.url} alt={a.name} className={styles.attachThumb} /> : <span className={styles.attachFile}>{a.name}</span>}
                                    <button className={styles.attachRemove} onClick={() => removeAttachment(i)}>x</button>
                                </div>
                            ))}
                        </div>
                    )}

                    {selectedKbIds.length > 0 && (
                        <div className={styles.attachList}>
                            {selectedKbIds.map(kbId => {
                                const kb = visibleKnowledgeBases.find(k => k.id === kbId);
                                return kb ? (
                                    <div key={kbId} className={styles.attachItem}>
                                        <span className={styles.attachFile}>{kb.name}</span>
                                        <button className={styles.attachRemove} onClick={() => toggleKb(kbId)}>x</button>
                                    </div>
                                ) : null;
                            })}
                        </div>
                    )}

                    <div className={styles.chatInput}>
                        <textarea value={input} onChange={e => setInput(e.target.value)} onKeyDown={handleKeyDown}
                            placeholder="描述你想要的页面效果..." rows={3} disabled={generating} />
                        <button className="btn-primary" onClick={handleSend} disabled={generating || !input.trim()}>{generating ? '生成中...' : '发送'}</button>
                    </div>
                </div>
            </div>

            {/* 右侧 */}
            <div className={styles.rightPanel}>
                <div className={styles.toolbar}>
                    <div className={styles.tabBar}>
                        <button className={`${styles.tab} ${rightTab === 'code' ? styles.tabActive : ''}`} onClick={() => setRightTab('code')}>
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <polyline points="16 18 22 12 16 6" /><polyline points="8 6 2 12 8 18" />
                            </svg>
                            代码
                        </button>
                        <button className={`${styles.tab} ${rightTab === 'preview' ? styles.tabActive : ''}`} onClick={() => setRightTab('preview')}>
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="2" y="3" width="20" height="14" rx="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" />
                            </svg>
                            预览
                        </button>
                    </div>
                    <div className={styles.versionSelect}>
                        <label>版本</label>
                        <select value={currentSnapshotId || ''} onChange={e => { if (e.target.value) loadSnapshot(Number(e.target.value)); }}>
                            {snapshots.map(s => <option key={s.id} value={s.id}>v{s.version} · {s.fileCount}文件 · {new Date(s.createdAt).toLocaleString()}</option>)}
                            {snapshots.length === 0 && <option value="">暂无版本</option>}
                        </select>
                    </div>
                    <div className={styles.toolbarActions}>
                        {rightTab === 'preview' && (
                            <button className="btn-outline" style={{ padding: '4px 10px', fontSize: 12 }} onClick={handleRefreshPreview} disabled={!sandboxHtml}>刷新预览</button>
                        )}
                        <button className="btn-outline" style={{ padding: '4px 10px', fontSize: 12 }} onClick={handleDownload} disabled={!currentSnapshotId}>下载ZIP</button>
                        <button className="btn-outline" style={{ padding: '4px 10px', fontSize: 12 }} onClick={handleShare} disabled={!currentSnapshotId && snapshots.length === 0}>分享</button>
                    </div>
                </div>

                {rightTab === 'code' ? (
                    <div className={styles.rightBody}>
                        <div className={styles.filePanel}>
                            <div className={styles.filePanelHeader}>
                                文件
                                {streamFiles.length > 0 && <span className={styles.streamBadge}>实时</span>}
                            </div>
                            <div className={styles.fileTree}>
                                {fileTree.map(n => <FileTreeNode key={n.path} node={n} selectedPath={selectedFilePath} onSelect={setSelectedFilePath} />)}
                                {displayFiles.length === 0 && <div className={styles.fileEmpty}>暂无文件</div>}
                            </div>
                        </div>
                        <div className={styles.previewArea}>
                            {selectedFile ? (
                                <div className={styles.codeViewer}>
                                    <div className={styles.codeHeader}>{selectedFile.path}</div>
                                    <pre className={styles.codeContent}>{selectedFile.content}</pre>
                                </div>
                            ) : (
                                <div className={styles.previewEmpty}>
                                    <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="2" y="3" width="20" height="14" rx="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" /></svg>
                                    <p>发送消息后，生成的代码将在这里展示和预览</p>
                                </div>
                            )}
                        </div>
                    </div>
                ) : (
                    <div className={styles.previewArea} style={{ flex: 1 }}>
                        {sandboxHtml ? (
                            <iframe ref={iframeRef} srcDoc={sandboxHtml} className={styles.iframe}
                                sandbox="allow-scripts allow-same-origin allow-forms allow-modals allow-popups" />
                        ) : (
                            <div className={styles.previewEmpty}>
                                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                                    <rect x="2" y="3" width="20" height="14" rx="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" />
                                </svg>
                                <p>暂无可预览的内容</p>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* 分享弹窗 */}
            {shareModalOpen && (
                <div className={styles.modalOverlay} onClick={() => setShareModalOpen(false)}>
                    <div className={styles.modalContent} onClick={e => e.stopPropagation()}>
                        <div className={styles.modalHeader}>
                            <h3 className={styles.modalTitle}>分享</h3>
                            <button className={styles.modalClose} onClick={() => setShareModalOpen(false)}>
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                                </svg>
                            </button>
                        </div>

                        {shareLoading ? (
                            <div className={styles.modalLoading}>生成分享链接中...</div>
                        ) : shareInfo ? (
                            <div className={styles.modalBody}>
                                {/* 分享版本信息 */}
                                <div className={styles.shareVersionInfo}>
                                    {shareInfo.version
                                        ? `分享版本: v${shareInfo.version}`
                                        : '分享最新版本'}
                                </div>

                                {/* 预览链接 */}
                                <div className={styles.shareSection}>
                                    <div className={styles.shareSectionTitle}>
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                                            <polyline points="15 3 21 3 21 9" /><line x1="10" y1="14" x2="21" y2="3" />
                                        </svg>
                                        预览链接
                                    </div>
                                    <p className={styles.shareSectionDesc}>任何人都可以通过此链接查看页面展示效果，无需登录</p>
                                    <div className={styles.shareLinkRow}>
                                        <input className={styles.shareLinkInput} readOnly value={shareInfo.shareUrl} />
                                        <button
                                            className={`btn-primary ${styles.shareCopyBtn}`}
                                            onClick={() => handleCopyLink(shareInfo.shareUrl, 'link')}
                                        >
                                            {shareCopied === 'link' ? '已复制' : '复制'}
                                        </button>
                                    </div>
                                </div>

                                {/* MCP 服务 */}
                                <div className={styles.shareSection}>
                                    <div className={styles.shareSectionTitle}>
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <rect x="2" y="3" width="20" height="14" rx="2" />
                                            <line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" />
                                        </svg>
                                        通过 MCP 获取代码
                                    </div>
                                    <p className={styles.shareSectionDesc}>
                                        将上方预览链接发送给已配置 MCP 服务的 AI 助手，即可自动获取代码文件。MCP 端点已绑定你的个人身份，仅可获取你分享的代码。
                                    </p>

                                    {/* MCP 配置说明 */}
                                    <div className={styles.mcpConfigGuide}>
                                        <div className={styles.mcpConfigTitle}>
                                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                <circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" />
                                            </svg>
                                            MCP 服务配置（绑定你的个人令牌，一次配置）
                                        </div>
                                        <p className={styles.mcpConfigDesc}>在 Cursor、Claude Desktop 等工具的 MCP 配置中添加（请勿泄露此地址）：</p>
                                        <pre className={styles.mcpConfigCode}>{`{
  "mcpServers": {
    "ai-copilot": {
      "url": "${shareInfo.mcpEndpoint}"
    }
  }
}`}</pre>
                                        <div className={styles.mcpConfigTitle} style={{ marginTop: 12 }}>
                                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10" />
                                            </svg>
                                            使用方式
                                        </div>
                                        <p className={styles.mcpConfigDesc}>配置完成后，直接将分享链接发给 AI 助手即可：</p>
                                        <pre className={styles.mcpConfigCode}>{`帮我获取这个链接的代码：${shareInfo.shareUrl}`}</pre>
                                        <p className={styles.mcpConfigDesc}>AI 助手会自动调用 <code className={styles.mcpInlineCode}>get_code</code> 工具，通过分享链接获取代码文件。</p>

                                        <details className={styles.mcpConfigDetails}>
                                            <summary className={styles.mcpConfigSummary}>工具参数说明</summary>
                                            <div className={styles.mcpConfigDetailBody}>
                                                <ul className={styles.mcpConfigToolList}>
                                                    <li><code>get_code</code> — 根据分享链接获取代码文件</li>
                                                    <li><code>share_url</code>（必填）— 分享链接地址</li>
                                                    <li><code>version</code>（可选）— 版本号，不传则获取最新版本</li>
                                                </ul>
                                            </div>
                                        </details>
                                    </div>
                                </div>
                            </div>
                        ) : null}
                    </div>
                </div>
            )}
        </div>
    );
}

// ===================== 文件树 =====================
interface TreeNode { name: string; path: string; isDir: boolean; children: TreeNode[]; }
function buildFileTree(files: CodeFile[]): TreeNode[] {
    const root: TreeNode[] = [];
    for (const file of files) {
        const parts = file.path.split('/'); let current = root; let pathSoFar = '';
        for (let i = 0; i < parts.length; i++) {
            pathSoFar += (i > 0 ? '/' : '') + parts[i]; const isLast = i === parts.length - 1;
            let existing = current.find(n => n.name === parts[i] && n.isDir === !isLast);
            if (!existing) { existing = { name: parts[i], path: pathSoFar, isDir: !isLast, children: [] }; current.push(existing); }
            current = existing.children;
        }
    }
    return root;
}
function FileTreeNode({ node, selectedPath, onSelect, depth = 0 }: { node: TreeNode; selectedPath: string | null; onSelect: (p: string) => void; depth?: number; }) {
    const [open, setOpen] = useState(true);
    if (node.isDir) return (
        <div>
            <div className={styles.treeDir} style={{ paddingLeft: 8 + depth * 14 }} onClick={() => setOpen(!open)}>
                <span className={styles.treeDirIcon}>{open ? '▾' : '▸'}</span><span>{node.name}</span>
            </div>
            {open && node.children.map(c => <FileTreeNode key={c.path} node={c} selectedPath={selectedPath} onSelect={onSelect} depth={depth + 1} />)}
        </div>
    );
    return (
        <div className={`${styles.treeFile} ${selectedPath === node.path ? styles.treeFileActive : ''}`}
            style={{ paddingLeft: 22 + depth * 14 }} onClick={() => onSelect(node.path)}>{node.name}</div>
    );
}