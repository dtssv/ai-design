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
            })
            .catch((err) => setError(err.message || '加载失败'))
            .finally(() => setLoading(false));
    }, [token]);

    const sandboxHtml = useMemo(() => {
        if (!data?.files?.length) return '';
        return buildSandboxHtml(data.files);
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
                <div className={styles.headerRight}>
                    <span className={styles.headerPowered}>AI Copilot 分享</span>
                </div>
            </div>

            {/* 内容区域 */}
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
        </div>
    );
}