import request from '@/utils/request';
import { useEffect, useState } from 'react';

export default function Configs() {
    const [list, setList] = useState<any[]>([]);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editValue, setEditValue] = useState('');
    const [group, setGroup] = useState('');

    const fetchList = () => {
        request.get('/configs', { params: { group: group || undefined } })
            .then((res: any) => setList(res.data || []));
    };

    useEffect(() => { fetchList(); }, [group]);

    const save = async (id: number) => {
        await request.put(`/configs/${id}`, { value: editValue });
        setEditingId(null);
        fetchList();
    };

    const groups = [...new Set(list.map((c: any) => c.group).filter(Boolean))];

    return (
        <div>
            <h1 style={{ fontSize: 22, fontWeight: 600, marginBottom: 20 }}>系统配置</h1>
            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                <select value={group} onChange={(e) => setGroup(e.target.value)}>
                    <option value="">全部分组</option>
                    {groups.map((g) => <option key={g} value={g}>{g}</option>)}
                </select>
            </div>

            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table>
                    <thead>
                        <tr><th>配置键</th><th>分组</th><th>当前值</th><th>描述</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                        {list.map((config) => (
                            <tr key={config.id}>
                                <td style={{ fontFamily: 'monospace', fontSize: 12, fontWeight: 500 }}>{config.configKey}</td>
                                <td><span className="badge badge-default">{config.group || '-'}</span></td>
                                <td>
                                    {editingId === config.id ? (
                                        <input
                                            type="text"
                                            value={editValue}
                                            onChange={(e) => setEditValue(e.target.value)}
                                            onKeyDown={(e) => e.key === 'Enter' && save(config.id)}
                                            style={{ width: '100%', minWidth: 200 }}
                                            autoFocus
                                        />
                                    ) : (
                                        <span style={{ fontFamily: 'monospace', fontSize: 12, wordBreak: 'break-all' }}>{config.configValue}</span>
                                    )}
                                </td>
                                <td style={{ fontSize: 12, color: 'var(--text-secondary)', maxWidth: 200 }}>{config.description || '-'}</td>
                                <td>
                                    {editingId === config.id ? (
                                        <div style={{ display: 'flex', gap: 4 }}>
                                            <button className="btn-sm btn-primary" onClick={() => save(config.id)}>保存</button>
                                            <button className="btn-sm btn-outline" onClick={() => setEditingId(null)}>取消</button>
                                        </div>
                                    ) : (
                                        <button className="btn-sm btn-outline" onClick={() => { setEditingId(config.id); setEditValue(config.configValue); }}>编辑</button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        {list.length === 0 && (
                            <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 24 }}>暂无配置</td></tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}