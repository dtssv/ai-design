import { useWorkspaceStore } from '@/stores/workspace';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Dashboard.module.css';

export default function Dashboard() {
    const { workspaces, fetchWorkspaces, createWorkspace, loading } = useWorkspaceStore();
    const [showCreate, setShowCreate] = useState(false);
    const [name, setName] = useState('');
    const [mode, setMode] = useState('prototype');
    const navigate = useNavigate();

    useEffect(() => { fetchWorkspaces(); }, []);

    const handleCreate = async () => {
        if (!name.trim()) return;
        const ws = await createWorkspace({ name, mode });
        setShowCreate(false);
        setName('');
        navigate(`/workspace/${ws.id}`);
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <div>
                    <h1 className={styles.title}>工作台</h1>
                    <p className={styles.subtitle}>管理你的所有工作区</p>
                </div>
                <button className="btn-primary" onClick={() => setShowCreate(true)}>
                    + 新建工作区
                </button>
            </div>

            {loading && workspaces.length === 0 && (
                <div className={styles.empty}>加载中...</div>
            )}

            {!loading && workspaces.length === 0 && (
                <div className={styles.empty}>
                    <div className={styles.emptyIcon}>
                        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="3" y="3" width="18" height="18" rx="2" /><path d="M12 8v8" /><path d="M8 12h8" /></svg>
                    </div>
                    <p>还没有工作区</p>
                    <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>点击"新建工作区"开始你的 AI 前端开发之旅</p>
                </div>
            )}

            <div className={styles.grid}>
                {workspaces.map((ws) => (
                    <div key={ws.id} className={styles.card} onClick={() => navigate(`/workspace/${ws.id}`)}>
                        <div className={styles.cardHeader}>
                            <span className={styles.cardMode}>{ws.mode === 'prototype' ? '原型' : '开发'}</span>
                            <span className={styles.cardStatus}>{ws.status}</span>
                        </div>
                        <h3 className={styles.cardTitle}>{ws.name}</h3>
                        <p className={styles.cardDesc}>{ws.description || '暂无描述'}</p>
                        <div className={styles.cardFooter}>
                            <span>{new Date(ws.createdAt).toLocaleDateString()}</span>
                        </div>
                    </div>
                ))}
            </div>

            {/* 创建工作区弹窗 */}
            {showCreate && (
                <div className={styles.modal} onClick={() => setShowCreate(false)}>
                    <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                        <h2>新建工作区</h2>
                        <div className={styles.modalField}>
                            <label>名称</label>
                            <input
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="给工作区起个名字"
                                autoFocus
                            />
                        </div>
                        <div className={styles.modalField}>
                            <label>模式</label>
                            <div className={styles.modeSelect}>
                                <button
                                    className={`${styles.modeOption} ${mode === 'prototype' ? styles.modeActive : ''}`}
                                    onClick={() => setMode('prototype')}
                                >
                                    <strong>原型模式</strong>
                                    <span>快速生成页面原型，适合产品验证</span>
                                </button>
                                <button
                                    className={`${styles.modeOption} ${mode === 'development' ? styles.modeActive : ''}`}
                                    onClick={() => setMode('development')}
                                >
                                    <strong>开发模式</strong>
                                    <span>生成可部署的完整前端代码</span>
                                </button>
                            </div>
                        </div>
                        <div className={styles.modalActions}>
                            <button className="btn-outline" onClick={() => setShowCreate(false)}>取消</button>
                            <button className="btn-primary" onClick={handleCreate}>创建</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}