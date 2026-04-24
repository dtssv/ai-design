import request from '@/utils/request';
import { useEffect, useState } from 'react';

export default function Logs() {
    const [list, setList] = useState<any[]>([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(1);
    const [keyword, setKeyword] = useState('');
    const [actionType, setActionType] = useState('');

    const fetchList = () => {
        request.get('/logs', { params: { page, size: 20, keyword: keyword || undefined, actionType: actionType || undefined } })
            .then((res: any) => { setList(res.data.items); setTotal(res.data.total); });
    };

    useEffect(() => { fetchList(); }, [page, actionType]);

    return (
        <div>
            <h1 style={{ fontSize: 22, fontWeight: 600, marginBottom: 20 }}>操作日志</h1>
            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                <input type="text" placeholder="搜索操作人/描述" value={keyword} onChange={(e) => setKeyword(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && fetchList()} style={{ width: 220 }} />
                <select value={actionType} onChange={(e) => { setActionType(e.target.value); setPage(1); }}>
                    <option value="">全部类型</option>
                    <option value="login">登录</option>
                    <option value="user_manage">用户管理</option>
                    <option value="team_manage">团队管理</option>
                    <option value="knowledge_review">知识库审核</option>
                    <option value="config_update">配置修改</option>
                    <option value="order_manage">订单管理</option>
                </select>
                <button className="btn-outline btn-sm" onClick={() => { setPage(1); fetchList(); }}>搜索</button>
            </div>

            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table>
                    <thead>
                        <tr><th>ID</th><th>操作人</th><th>操作类型</th><th>描述</th><th>IP 地址</th><th>操作时间</th></tr>
                    </thead>
                    <tbody>
                        {list.map((log) => (
                            <tr key={log.id}>
                                <td>{log.id}</td>
                                <td>{log.operatorName || log.operatorId}</td>
                                <td><span className="badge badge-default">{log.actionType}</span></td>
                                <td style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{log.description}</td>
                                <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{log.ipAddress || '-'}</td>
                                <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{log.createdAt?.slice(0, 19).replace('T', ' ')}</td>
                            </tr>
                        ))}
                        {list.length === 0 && (
                            <tr><td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 24 }}>暂无日志</td></tr>
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