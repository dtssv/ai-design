import request from '@/utils/request';
import { useEffect, useState } from 'react';

export default function Users() {
    const [users, setUsers] = useState<any[]>([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(1);
    const [keyword, setKeyword] = useState('');

    const fetchUsers = () => {
        request.get('/users', { params: { page, size: 20, keyword: keyword || undefined } })
            .then((res: any) => { setUsers(res.data.items); setTotal(res.data.total); });
    };

    useEffect(() => { fetchUsers(); }, [page]);

    const toggleStatus = async (id: number, status: string) => {
        const newStatus = status === 'active' ? 'disabled' : 'active';
        await request.put(`/users/${id}/status`, { status: newStatus });
        fetchUsers();
    };

    return (
        <div>
            <h1 style={{ fontSize: 22, fontWeight: 600, marginBottom: 20 }}>用户管理</h1>
            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                <input placeholder="搜索邮箱/手机/昵称" value={keyword} onChange={(e) => setKeyword(e.target.value)} style={{ width: 260 }} />
                <button className="btn-primary" onClick={() => { setPage(1); fetchUsers(); }}>搜索</button>
            </div>
            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table>
                    <thead>
                        <tr>
                            <th>ID</th><th>邮箱</th><th>手机</th><th>昵称</th><th>角色</th><th>状态</th><th>注册时间</th><th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users.map((u) => (
                            <tr key={u.id}>
                                <td>{u.id}</td>
                                <td>{u.email || '-'}</td>
                                <td>{u.phone || '-'}</td>
                                <td>{u.nickname}</td>
                                <td><span className="badge badge-default">{u.role}</span></td>
                                <td>
                                    <span className={`badge ${u.status === 'active' ? 'badge-success' : 'badge-danger'}`}>
                                        {u.status === 'active' ? '正常' : '已禁用'}
                                    </span>
                                </td>
                                <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{u.createdAt?.slice(0, 10)}</td>
                                <td>
                                    <button className={`btn-sm ${u.status === 'active' ? 'btn-danger' : 'btn-primary'}`}
                                        onClick={() => toggleStatus(u.id, u.status)}>
                                        {u.status === 'active' ? '禁用' : '启用'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {users.length === 0 && (
                            <tr><td colSpan={8} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 24 }}>暂无数据</td></tr>
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