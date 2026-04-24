import request from '@/utils/request';
import { useEffect, useState } from 'react';

export default function KnowledgeReview() {
    const [list, setList] = useState<any[]>([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(1);
    const [reviewStatus, setReviewStatus] = useState('');

    const fetchList = () => {
        request.get('/knowledge-bases', { params: { page, size: 20, reviewStatus: reviewStatus || undefined } })
            .then((res: any) => { setList(res.data.items); setTotal(res.data.total); });
    };

    useEffect(() => { fetchList(); }, [page, reviewStatus]);

    const review = async (id: number, action: string) => {
        await request.put(`/knowledge-bases/${id}/review`, { action });
        fetchList();
    };

    return (
        <div>
            <h1 style={{ fontSize: 22, fontWeight: 600, marginBottom: 20 }}>知识库管理</h1>
            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                <select value={reviewStatus} onChange={(e) => { setReviewStatus(e.target.value); setPage(1); }}>
                    <option value="">全部状态</option>
                    <option value="pending">待审核</option>
                    <option value="approved">已通过</option>
                    <option value="rejected">已拒绝</option>
                </select>
            </div>
            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table>
                    <thead>
                        <tr><th>ID</th><th>名称</th><th>类型</th><th>审核状态</th><th>创建时间</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                        {list.map((kb) => (
                            <tr key={kb.id}>
                                <td>{kb.id}</td>
                                <td>{kb.name}</td>
                                <td><span className="badge badge-default">{kb.type}</span></td>
                                <td>
                                    <span className={`badge ${kb.reviewStatus === 'approved' ? 'badge-success' : kb.reviewStatus === 'pending' ? 'badge-warning' : kb.reviewStatus === 'rejected' ? 'badge-danger' : 'badge-default'}`}>
                                        {kb.reviewStatus}
                                    </span>
                                </td>
                                <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{kb.createdAt?.slice(0, 10)}</td>
                                <td style={{ display: 'flex', gap: 4 }}>
                                    {kb.reviewStatus === 'pending' && (
                                        <>
                                            <button className="btn-sm btn-primary" onClick={() => review(kb.id, 'approve')}>通过</button>
                                            <button className="btn-sm btn-danger" onClick={() => review(kb.id, 'reject')}>拒绝</button>
                                        </>
                                    )}
                                    {kb.visibility === 'public' && (
                                        <button className="btn-sm btn-outline" onClick={() => { request.put(`/knowledge-bases/${kb.id}/unpublish`).then(fetchList); }}>下架</button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        {list.length === 0 && (
                            <tr><td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 24 }}>暂无数据</td></tr>
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