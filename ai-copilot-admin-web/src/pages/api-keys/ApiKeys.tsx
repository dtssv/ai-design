import request from '@/utils/request';
import { useEffect, useState } from 'react';

export default function ApiKeys() {
    const [list, setList] = useState<any[]>([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(1);
    const [provider, setProvider] = useState('');
    const [showAdd, setShowAdd] = useState(false);
    const [form, setForm] = useState({ provider: '', keyValue: '', dailyLimit: 1000 });

    const fetchList = () => {
        request.get('/api-keys/pool', { params: { page, size: 20, provider: provider || undefined } })
            .then((res: any) => { setList(res.data.items); setTotal(res.data.total); });
    };

    useEffect(() => { fetchList(); }, [page, provider]);

    const addKey = async () => {
        await request.post('/api-keys', form);
        setShowAdd(false);
        setForm({ provider: '', keyValue: '', dailyLimit: 1000 });
        fetchList();
    };

    const toggleStatus = async (id: number, currentStatus: string) => {
        const action = currentStatus === 'active' ? 'disable' : 'enable';
        await request.put(`/api-keys/${id}/status`, { action });
        fetchList();
    };

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                <h1 style={{ fontSize: 22, fontWeight: 600 }}>API-Key 池管理</h1>
                <button className="btn-primary" onClick={() => setShowAdd(!showAdd)}>添加 Key</button>
            </div>

            {showAdd && (
                <div className="card" style={{ marginBottom: 16, display: 'flex', gap: 8, alignItems: 'flex-end' }}>
                    <div>
                        <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>Provider</label>
                        <select value={form.provider} onChange={(e) => setForm({ ...form, provider: e.target.value })}>
                            <option value="">请选择</option>
                            <option value="openai">OpenAI</option>
                            <option value="anthropic">Anthropic</option>
                            <option value="deepseek">DeepSeek</option>
                        </select>
                    </div>
                    <div style={{ flex: 1 }}>
                        <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>Key Value</label>
                        <input type="text" value={form.keyValue} onChange={(e) => setForm({ ...form, keyValue: e.target.value })} placeholder="sk-..." style={{ width: '100%' }} />
                    </div>
                    <div>
                        <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>日限额</label>
                        <input type="number" value={form.dailyLimit} onChange={(e) => setForm({ ...form, dailyLimit: +e.target.value })} style={{ width: 100 }} />
                    </div>
                    <button className="btn-primary btn-sm" onClick={addKey}>保存</button>
                    <button className="btn-outline btn-sm" onClick={() => setShowAdd(false)}>取消</button>
                </div>
            )}

            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                <select value={provider} onChange={(e) => { setProvider(e.target.value); setPage(1); }}>
                    <option value="">全部 Provider</option>
                    <option value="openai">OpenAI</option>
                    <option value="anthropic">Anthropic</option>
                    <option value="deepseek">DeepSeek</option>
                </select>
            </div>

            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table>
                    <thead>
                        <tr><th>ID</th><th>Provider</th><th>Key (前缀)</th><th>日限额</th><th>今日用量</th><th>状态</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                        {list.map((key) => (
                            <tr key={key.id}>
                                <td>{key.id}</td>
                                <td><span className="badge badge-default">{key.provider}</span></td>
                                <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{key.keyPrefix}...</td>
                                <td>{key.dailyLimit?.toLocaleString()}</td>
                                <td>{key.todayUsage?.toLocaleString()}</td>
                                <td>
                                    <span className={`badge ${key.status === 'active' ? 'badge-success' : 'badge-danger'}`}>{key.status}</span>
                                </td>
                                <td>
                                    <button className={`btn-sm ${key.status === 'active' ? 'btn-danger' : 'btn-primary'}`} onClick={() => toggleStatus(key.id, key.status)}>
                                        {key.status === 'active' ? '禁用' : '启用'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {list.length === 0 && (
                            <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 24 }}>暂无数据</td></tr>
                        )}
                    </tbody>
                </table>
            </div>
            <div className="pagination">
                <button className="btn-outline btn-sm" disabled={page <= 1} onClick={() => setPage(page - 1)}>上一页</button>
                <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>第 {page} 页</span>
                <button className="btn-outline btn-sm" disabled={page * 20 >= total} onClick={() => setPage(page + 1)}>下一页</button>
            </div>
        </div>
    );
}