import request from '@/utils/request';
import { useEffect, useState } from 'react';

export default function Orders() {
    const [list, setList] = useState<any[]>([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(1);
    const [status, setStatus] = useState('');
    const [keyword, setKeyword] = useState('');

    const fetchList = () => {
        request.get('/orders', { params: { page, size: 20, status: status || undefined, keyword: keyword || undefined } })
            .then((res: any) => { setList(res.data.items); setTotal(res.data.total); });
    };

    useEffect(() => { fetchList(); }, [page, status]);

    const handleRefund = async (id: number) => {
        if (!confirm('确认退款？此操作不可撤销')) return;
        await request.post(`/orders/${id}/refund`);
        fetchList();
    };

    const statusMap: Record<string, { label: string; cls: string }> = {
        pending: { label: '待支付', cls: 'badge-warning' },
        paid: { label: '已支付', cls: 'badge-success' },
        refunded: { label: '已退款', cls: 'badge-danger' },
        cancelled: { label: '已取消', cls: 'badge-default' },
    };

    return (
        <div>
            <h1 style={{ fontSize: 22, fontWeight: 600, marginBottom: 20 }}>订单管理</h1>
            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                <input type="text" placeholder="搜索订单号/用户名" value={keyword} onChange={(e) => setKeyword(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && fetchList()} style={{ width: 220 }} />
                <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(1); }}>
                    <option value="">全部状态</option>
                    <option value="pending">待支付</option>
                    <option value="paid">已支付</option>
                    <option value="refunded">已退款</option>
                    <option value="cancelled">已取消</option>
                </select>
                <button className="btn-outline btn-sm" onClick={() => { setPage(1); fetchList(); }}>搜索</button>
            </div>

            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table>
                    <thead>
                        <tr><th>订单号</th><th>用户</th><th>套餐</th><th>金额</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                        {list.map((order) => (
                            <tr key={order.id}>
                                <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{order.orderNo}</td>
                                <td>{order.userName || order.userId}</td>
                                <td>{order.planName}</td>
                                <td style={{ fontWeight: 600 }}>¥{order.amount}</td>
                                <td>
                                    <span className={`badge ${statusMap[order.status]?.cls || 'badge-default'}`}>
                                        {statusMap[order.status]?.label || order.status}
                                    </span>
                                </td>
                                <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{order.createdAt?.slice(0, 16).replace('T', ' ')}</td>
                                <td>
                                    {order.status === 'paid' && (
                                        <button className="btn-sm btn-danger" onClick={() => handleRefund(order.id)}>退款</button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        {list.length === 0 && (
                            <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 24 }}>暂无订单</td></tr>
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