import request from '@/utils/request';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import styles from './Knowledge.module.css';

interface Entry {
    id: number;
    title: string;
    contentType: string;
    content: string;
    status: string;
    createdAt: string;
}

export default function KnowledgeDetail() {
    const { id } = useParams();
    const [baseName, setBaseName] = useState('');
    const [entries, setEntries] = useState<Entry[]>([]);
    const [showAdd, setShowAdd] = useState(false);
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [contentType, setContentType] = useState('component');

    const fetchEntries = async () => {
        const baseRes: any = await request.get(`/knowledge-bases/${id}`);
        setBaseName(baseRes.data?.name || '');
        const entryRes: any = await request.get(`/knowledge-bases/${id}/entries`);
        setEntries(entryRes.data || []);
    };

    useEffect(() => { fetchEntries(); }, [id]);

    const handleAdd = async () => {
        if (!title.trim() || !content.trim()) return;
        await request.post(`/knowledge-bases/${id}/entries`, { title, content, contentType });
        setShowAdd(false);
        setTitle('');
        setContent('');
        fetchEntries();
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <div>
                    <h1 className={styles.title}>{baseName}</h1>
                    <p className={styles.subtitle}>{entries.length} 条知识条目</p>
                </div>
                <button className="btn-primary" onClick={() => setShowAdd(true)}>+ 新增条目</button>
            </div>

            <div className={styles.entryList}>
                {entries.map((entry) => (
                    <div key={entry.id} className={styles.entryItem}>
                        <div>
                            <div className={styles.entryTitle}>{entry.title}</div>
                            <div className={styles.entryMeta}>{entry.contentType} · {entry.status} · {new Date(entry.createdAt).toLocaleDateString()}</div>
                        </div>
                        <div className={styles.entryActions}>
                            <button className="btn-outline" style={{ padding: '4px 10px', fontSize: 12 }}>查看</button>
                        </div>
                    </div>
                ))}
                {entries.length === 0 && <div className={styles.empty}>暂无条目</div>}
            </div>

            {showAdd && (
                <div className={styles.modal} onClick={() => setShowAdd(false)}>
                    <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                        <h2>新增知识条目</h2>
                        <div className={styles.field}>
                            <label>标题</label>
                            <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="条目标题" autoFocus />
                        </div>
                        <div className={styles.field}>
                            <label>类型</label>
                            <select value={contentType} onChange={(e) => setContentType(e.target.value)}>
                                <option value="component">组件规范</option>
                                <option value="design_system">设计系统</option>
                                <option value="code_snippet">代码片段</option>
                                <option value="prompt_template">Prompt模板</option>
                            </select>
                        </div>
                        <div className={styles.field}>
                            <label>内容</label>
                            <textarea value={content} onChange={(e) => setContent(e.target.value)} placeholder="输入知识内容..." rows={6} />
                        </div>
                        <div className={styles.modalActions}>
                            <button className="btn-outline" onClick={() => setShowAdd(false)}>取消</button>
                            <button className="btn-primary" onClick={handleAdd}>添加</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}