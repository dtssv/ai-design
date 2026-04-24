import type { CodeFile } from '@/stores/workspace';
import { buildSandboxHtml } from '@/utils/sandboxBuilder';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import styles from './SharePreview.module.css';

interface ShareData {
    workspaceName: string;
    version: number;
    files: CodeFile[];
    createdAt: string;
}

export default function SharePreview() {
    const { token } = useParams<{ token: string }>();
    const [data, setData] = useState<ShareData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [tab, setTab] = useState<'preview' | 'code'>('preview');
    const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null);
    const iframeRef = useRef<HTMLIFrameElement>(null);

    useEffect(() => {
        if (!token) return;
        setLoading(true);
        fetch(`/api/v1/share/${token}`)
            .then(async (res) => {
                const json = await res.json();
                if (json.code !== 0) throw new Error(json.message || '获取分享内容失败');
                return json.data;
            })
            .then((d: ShareData) => {
                setData(d);
                if (d.files?.length > 0) setSelectedFilePath(d.files[0].path);
            })
            .catch((err) => setError(err.message || '加载失败'))
            .finally(() => setLoading(false));
    }, [token]);

    const sandboxHtml = useMemo(() => {
        if (!data?.files?.length) return '';
        return buildSandboxHtml(data.files);
    }, [data?.files]);

    const selectedFile = useMemo(
        () => data?.files?.find((f) => f.path === selectedFilePath) || null,
        [data?.files, selectedFilePath],
    );

    const fileTree = useMemo(() => {
        if (!data?.files) return [];
        return buildFileTree(data.files);
    }, [data?.files]);

    if (loading) {
        return (
            <div className={styles.loading}>
                <div className={styles.spinner} />
                <p>加载分享内容中...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className={styles.error}>
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" y1="9" x2="9" y2="15" />
                    <line x1="9" y1="9" x2="15" y2="15" />
                </svg>
                <h2>无法访问</h2>
                <p>{error}</p>
            </div>
        );
    }

    if (!data) return null;

    return (
        <div className={styles.layout}>
            {/* 顶部栏 */}
            <div className={styles.header}>
                <div className={styles.headerLeft}>
                    <span className={styles.headerTitle}>{data.workspaceName}</span>
                    <span className={styles.headerVersion}>v{data.version}</span>
                </div>
                <div className={styles.headerTabs}>
                    <button
                        className={`${styles.headerTab} ${tab === 'preview' ? styles.headerTabActive : ''}`}
                        onClick={() => setTab('preview')}
                    >
                        预览
                    </button>
                    <button
                        className={`${styles.headerTab} ${tab === 'code' ? styles.headerTabActive : ''}`}
                        onClick={() => setTab('code')}
                    >
                        代码
                    </button>
                </div>
                <div className={styles.headerRight}>
                    <span className={styles.headerPowered}>AI Copilot 分享</span>
                </div>
            </div>

            {/* 内容区域 */}
            {tab === 'preview' ? (
                <div className={styles.previewArea}>
                    {sandboxHtml ? (
                        <iframe
                            ref={iframeRef}
                            srcDoc={sandboxHtml}
                            className={styles.iframe}
                            sandbox="allow-scripts allow-same-origin allow-forms allow-modals allow-popups"
                        />
                    ) : (
                        <div className={styles.empty}>
                            <p>暂无可预览的内容</p>
                        </div>
                    )}
                </div>
            ) : (
                <div className={styles.codeLayout}>
                    <div className={styles.filePanel}>
                        <div className={styles.filePanelHeader}>文件</div>
                        <div className={styles.fileList}>
                            {fileTree.map((n) => (
                                <FileNode
                                    key={n.path}
                                    node={n}
                                    selectedPath={selectedFilePath}
                                    onSelect={setSelectedFilePath}
                                />
                            ))}
                        </div>
                    </div>
                    <div className={styles.codeArea}>
                        {selectedFile ? (
                            <div className={styles.codeViewer}>
                                <div className={styles.codeHeader}>{selectedFile.path}</div>
                                <pre className={styles.codeContent}>{selectedFile.content}</pre>
                            </div>
                        ) : (
                            <div className={styles.empty}>
                                <p>选择左侧文件查看代码</p>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

// ===================== 文件树 =====================
interface TreeNode {
    name: string;
    path: string;
    isDir: boolean;
    children: TreeNode[];
}

function buildFileTree(files: CodeFile[]): TreeNode[] {
    const root: TreeNode[] = [];
    for (const file of files) {
        const parts = file.path.split('/');
        let current = root;
        let pathSoFar = '';
        for (let i = 0; i < parts.length; i++) {
            pathSoFar += (i > 0 ? '/' : '') + parts[i];
            const isLast = i === parts.length - 1;
            let existing = current.find((n) => n.name === parts[i] && n.isDir === !isLast);
            if (!existing) {
                existing = { name: parts[i], path: pathSoFar, isDir: !isLast, children: [] };
                current.push(existing);
            }
            current = existing.children;
        }
    }
    return root;
}

function FileNode({
    node,
    selectedPath,
    onSelect,
    depth = 0,
}: {
    node: TreeNode;
    selectedPath: string | null;
    onSelect: (p: string) => void;
    depth?: number;
}) {
    const [open, setOpen] = useState(true);
    if (node.isDir) {
        return (
            <div>
                <div
                    className={styles.treeDir}
                    style={{ paddingLeft: 8 + depth * 14 }}
                    onClick={() => setOpen(!open)}
                >
                    <span className={styles.treeDirIcon}>{open ? '\u25BE' : '\u25B8'}</span>
                    <span>{node.name}</span>
                </div>
                {open &&
                    node.children.map((c) => (
                        <FileNode
                            key={c.path}
                            node={c}
                            selectedPath={selectedPath}
                            onSelect={onSelect}
                            depth={depth + 1}
                        />
                    ))}
            </div>
        );
    }
    return (
        <div
            className={`${styles.treeFile} ${selectedPath === node.path ? styles.treeFileActive : ''}`}
            style={{ paddingLeft: 22 + depth * 14 }}
            onClick={() => onSelect(node.path)}
        >
            {node.name}
        </div>
    );
}