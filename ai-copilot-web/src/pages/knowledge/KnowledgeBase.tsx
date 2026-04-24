import request from '@/utils/request';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Knowledge.module.css';

interface KB {
    id: number;
    name: string;
    description: string;
    scope: string;
    entryCount: number;
    status: string;
    createdAt: string;
}

export default function KnowledgeBase() {
    const [list, setList] = useState<KB[]>([]);
    const [showCreate, setShowCreate] = useState(false);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [scope, setScope] = useState('private');
    const navigate = useNavigate();

    const fetchList = async () => {
        const res: any = await request.get('/knowledge-bases/mine');
        setList(res.data || []);
    };

    useEffect(() => { fetchList(); }, []);

    const handleCreate = async () => {
        if (!name.trim()) return;
        await request.post('/knowledge-bases', { name, description, scope });
        setShowCreate(false);
        setName('');
        setDescription('');
        fetchList();
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <div>
                    <h1 className={styles.title}>知识库</h1>
                    <p className={styles.subtitle}>管理AI生成所需的设计规范和组件库</p>
                </div>
                <button className="btn-primary" onClick={() => setShowCreate(true)}>+ 创建知识库</button>
            </div>

            <div className={styles.grid}>
                {list.map((kb) => (
                    <div key={kb.id} className={styles.card} onClick={() => navigate(`/knowledge/${kb.id}`)}>
                        <div className={styles.cardTop}>
                            <span className={styles.scope}>{kb.scope === 'private' ? '私有' : kb.scope === 'team' ? '团队' : '公开'}</span>
                            <span className={styles.status}>{kb.status}</span>
                        </div>
                        <h3>{kb.name}</h3>
                        <p className={styles.desc}>{kb.description || '暂无描述'}</p>
                        <div className={styles.cardFooter}>
                            <span>{kb.entryCount || 0} 条目</span>
                            <span>{new Date(kb.createdAt).toLocaleDateString()}</span>
                        </div>
                    </div>
                ))}
            </div>

            {list.length === 0 && <div className={styles.empty}>暂无知识库</div>}

            {showCreate && (
                <div className={styles.modal} onClick={() => setShowCreate(false)}>
                    <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                        <h2>创建知识库</h2>
                        <div className={styles.field}>
                            <label>名称</label>
                            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="知识库名称" autoFocus />
                        </div>
                        <div className={styles.field}>
                            <label>描述</label>
                            <textarea value={description} onChange={(e) => setDescription(e.target.value)} placeholder="简短描述" rows={3} />
                        </div>
                        <div className={styles.field}>
                            <label>可见范围</label>
                            <select value={scope} onChange={(e) => setScope(e.target.value)}>
                                <option value="private">私有</option>
                                <option value="team">团队</option>
                                <option value="public">公开</option>
                            </select>
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