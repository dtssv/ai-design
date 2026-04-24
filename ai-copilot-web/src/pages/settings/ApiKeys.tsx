import request from '@/utils/request';
import { useEffect, useState } from 'react';
import styles from './Settings.module.css';

interface ApiKey {
    id: number;
    name: string;
    apiKeyEncrypted: string;
    provider: string;
    modelName: string;
    status: string;
    createdAt: string;
}

const emptyForm = { name: '', provider: 'openai', modelName: 'gpt-4', apiKeyEncrypted: '', apiBaseUrl: '' };

export default function ApiKeys() {
    const [keys, setKeys] = useState<ApiKey[]>([]);
    const [showModal, setShowModal] = useState(false);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [form, setForm] = useState({ ...emptyForm });

    const fetchKeys = async () => {
        const res: any = await request.get('/api-keys/personal');
        setKeys(res.data || []);
    };

    useEffect(() => { fetchKeys(); }, []);

    const openAdd = () => {
        setEditingId(null);
        setForm({ ...emptyForm });
        setShowModal(true);
    };

    const openEdit = (key: ApiKey) => {
        setEditingId(key.id);
        setForm({
            name: key.name,
            provider: key.provider,
            modelName: key.modelName,
            apiKeyEncrypted: '',
            apiBaseUrl: '',
        });
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setEditingId(null);
        setForm({ ...emptyForm });
    };

    const handleSubmit = async () => {
        if (!form.name) return;
        // 新增时 Key 必填，编辑时可选（不填则不更新）
        if (!editingId && !form.apiKeyEncrypted) return;

        if (editingId) {
            await request.put(`/api-keys/personal/${editingId}`, form);
        } else {
            await request.post('/api-keys/personal', form);
        }
        closeModal();
        fetchKeys();
    };

    const isEditing = editingId !== null;

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <div>
                    <h1 className={styles.title}>API-Key 管理</h1>
                    <p className={styles.subtitle}>管理你的 AI 模型 API 密钥</p>
                </div>
                <button className="btn-primary" onClick={openAdd}>+ 添加 Key</button>
            </div>

            <div className={styles.table}>
                <div className={styles.tableHeader}>
                    <span>名称</span><span>Provider</span><span>模型</span><span>Key</span><span>状态</span><span>操作</span>
                </div>
                {keys.map((k) => (
                    <div key={k.id} className={styles.tableRow}>
                        <span>{k.name}</span>
                        <span>{k.provider}</span>
                        <span>{k.modelName}</span>
                        <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{k.apiKeyEncrypted}</span>
                        <span className={k.status === 'active' ? styles.active : styles.inactive}>{k.status}</span>
                        <span>
                            <button className="btn-outline" style={{ padding: '4px 12px', fontSize: 12 }} onClick={() => openEdit(k)}>修改</button>
                        </span>
                    </div>
                ))}
                {keys.length === 0 && <div className={styles.emptyRow}>暂无 API Key</div>}
            </div>

            {showModal && (
                <div className={styles.modal} onClick={closeModal}>
                    <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                        <h2>{isEditing ? '修改 API Key' : '添加 API Key'}</h2>
                        <div className={styles.field}>
                            <label>名称</label>
                            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="给Key起个名字" />
                        </div>
                        <div className={styles.field}>
                            <label>Provider</label>
                            <select value={form.provider} onChange={(e) => setForm({ ...form, provider: e.target.value })}>
                                <option value="openai">OpenAI</option>
                                <option value="anthropic">Anthropic</option>
                                <option value="deepseek">DeepSeek</option>
                                <option value="custom">自定义</option>
                            </select>
                        </div>
                        <div className={styles.field}>
                            <label>模型名称</label>
                            <input value={form.modelName} onChange={(e) => setForm({ ...form, modelName: e.target.value })} />
                        </div>
                        <div className={styles.field}>
                            <label>API Key{isEditing ? '（留空则不修改）' : ''}</label>
                            <input value={form.apiKeyEncrypted} onChange={(e) => setForm({ ...form, apiKeyEncrypted: e.target.value })} placeholder="sk-..." type="password" />
                        </div>
                        {form.provider === 'custom' && (
                            <div className={styles.field}>
                                <label>API Base URL</label>
                                <input value={form.apiBaseUrl} onChange={(e) => setForm({ ...form, apiBaseUrl: e.target.value })} placeholder="https://api.example.com/v1" />
                            </div>
                        )}
                        <div className={styles.modalActions}>
                            <button className="btn-outline" onClick={closeModal}>取消</button>
                            <button className="btn-primary" onClick={handleSubmit}>{isEditing ? '保存' : '添加'}</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}