import request from '@/utils/request';
import { useEffect, useState } from 'react';

export default function QuotaPlans() {
    const [freeQuota, setFreeQuota] = useState<any>(null);
    const [plans, setPlans] = useState<any[]>([]);
    const [editingFree, setEditingFree] = useState(false);
    const [freeForm, setFreeForm] = useState({ dailyTokens: 0, dailyRequests: 0 });

    const fetchData = () => {
        request.get('/quota/free').then((res: any) => {
            setFreeQuota(res.data);
            setFreeForm({ dailyTokens: res.data?.dailyTokens || 0, dailyRequests: res.data?.dailyRequests || 0 });
        });
        request.get('/quota/plans').then((res: any) => setPlans(res.data || []));
    };

    useEffect(() => { fetchData(); }, []);

    const saveFreeQuota = async () => {
        await request.put('/quota/free', freeForm);
        setEditingFree(false);
        fetchData();
    };

    const togglePlan = async (id: number, enabled: boolean) => {
        await request.put(`/quota/plans/${id}`, { enabled: !enabled });
        fetchData();
    };

    return (
        <div>
            <h1 style={{ fontSize: 22, fontWeight: 600, marginBottom: 20 }}>额度套餐管理</h1>

            {/* 免费额度配置 */}
            <div className="card" style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                    <h2 style={{ fontSize: 16, fontWeight: 600 }}>免费额度配置</h2>
                    {!editingFree && <button className="btn-outline btn-sm" onClick={() => setEditingFree(true)}>编辑</button>}
                </div>
                {editingFree ? (
                    <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
                        <div>
                            <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>每日 Token 额度</label>
                            <input type="number" value={freeForm.dailyTokens} onChange={(e) => setFreeForm({ ...freeForm, dailyTokens: +e.target.value })} />
                        </div>
                        <div>
                            <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>每日请求次数</label>
                            <input type="number" value={freeForm.dailyRequests} onChange={(e) => setFreeForm({ ...freeForm, dailyRequests: +e.target.value })} />
                        </div>
                        <button className="btn-primary btn-sm" onClick={saveFreeQuota}>保存</button>
                        <button className="btn-outline btn-sm" onClick={() => setEditingFree(false)}>取消</button>
                    </div>
                ) : (
                    <div style={{ display: 'flex', gap: 32 }}>
                        <div>
                            <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>每日 Token 额度</div>
                            <div style={{ fontSize: 20, fontWeight: 600 }}>{freeQuota?.dailyTokens?.toLocaleString() || '-'}</div>
                        </div>
                        <div>
                            <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>每日请求次数</div>
                            <div style={{ fontSize: 20, fontWeight: 600 }}>{freeQuota?.dailyRequests?.toLocaleString() || '-'}</div>
                        </div>
                    </div>
                )}
            </div>

            {/* 付费套餐列表 */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <h2 style={{ fontSize: 16, fontWeight: 600 }}>付费套餐</h2>
            </div>
            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table>
                    <thead>
                        <tr><th>ID</th><th>套餐名称</th><th>价格</th><th>Token 额度</th><th>有效期</th><th>状态</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                        {plans.map((plan) => (
                            <tr key={plan.id}>
                                <td>{plan.id}</td>
                                <td style={{ fontWeight: 500 }}>{plan.name}</td>
                                <td>¥{plan.price}</td>
                                <td>{plan.tokenQuota?.toLocaleString()}</td>
                                <td>{plan.validDays} 天</td>
                                <td>
                                    <span className={`badge ${plan.enabled ? 'badge-success' : 'badge-default'}`}>
                                        {plan.enabled ? '上架' : '下架'}
                                    </span>
                                </td>
                                <td>
                                    <button className={`btn-sm ${plan.enabled ? 'btn-danger' : 'btn-primary'}`} onClick={() => togglePlan(plan.id, plan.enabled)}>
                                        {plan.enabled ? '下架' : '上架'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {plans.length === 0 && (
                            <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 24 }}>暂无套餐</td></tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}