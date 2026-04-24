import request from '@/utils/request';
import { useEffect, useState } from 'react';

export default function Teams() {
    const [teams, setTeams] = useState<any[]>([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(1);
    const [keyword, setKeyword] = useState('');

    const fetchTeams = () => {
        request.get('/teams', { params: { page, size: 20, keyword: keyword || undefined } })
            .then((res: any) => { setTeams(res.data.items); setTotal(res.data.total); });
    };

    useEffect(() => { fetchTeams(); }, [page]);

    const toggleStatus = async (id: number, status: string) => {
        const newStatus = status === 'active' ? 'disabled' : 'active';
        await request.put(`/teams/${id}/status`, { status: newStatus });
        fetchTeams();
    };

    return (
        <div>
            <h1 style={{ fontSize: 22, fontWeight: 600, marginBottom: 20 }}>团队管理</h1>
            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                <input placeholder="搜索团队名称" value={keyword} onChange={(e) => setKeyword(e.target.value)} style={{ width: 260 }} />
                <button className="btn-primary" onClick={() => { setPage(1); fetchTeams(); }}>搜索</button>
            </div>
            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table>
                    <thead>
                        <tr><th>ID</th><th>名称</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                        {teams.map((t) => (
                            <tr key={t.id}>
                                <td>{t.id}</td>
                                <td>{t.name}</td>
                                <td>
                                    <span className={`badge ${t.status === 'active' ? 'badge-success' : 'badge-danger'}`}>
                                        {t.status === 'active' ? '正常' : t.status}
                                    </span>
                                </td>
                                <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{t.createdAt?.slice(0, 10)}</td>
                                <td>
                                    <button className={`btn-sm ${t.status === 'active' ? 'btn-danger' : 'btn-primary'}`}
                                        onClick={() => toggleStatus(t.id, t.status)}>
                                        {t.status === 'active' ? '禁用' : '启用'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {teams.length === 0 && (
                            <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 24 }}>暂无数据</td></tr>
                        )}
                    </tbody>
                </table>
            </div>
            <div className="pagination">
                <button className="btn-outline btn-sm" disabled={page <= 1} onClick={() => setPage(page - 1)}>上一页</button>
                <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>第 {page} 页 / 共 {Math.ceil(total / 20)} 页</span>
                <button className="btn-outline btn-sm" disabled={page * 20 >= total} onClick={() => setPage(page + 1)}>下一页</button>
            </div>
        </div>
    );
}