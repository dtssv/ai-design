import request from '@/utils/request';
import { useEffect, useState } from 'react';

export default function Dashboard() {
    const [data, setData] = useState<any>(null);

    useEffect(() => {
        request.get('/dashboard/overview').then((res: any) => setData(res.data));
    }, []);

    const cards = data ? [
        { label: '总用户数', value: data.totalUsers, sub: `今日新增 ${data.todayNewUsers}` },
        { label: '总团队数', value: data.totalTeams },
        { label: '总工作区数', value: data.totalWorkspaces },
        { label: '今日生成量', value: data.todayGenerations },
    ] : [];

    return (
        <div>
            <h1 style={{ fontSize: 22, fontWeight: 600, marginBottom: 24 }}>数据看板</h1>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 32 }}>
                {cards.map((card, i) => (
                    <div key={i} className="card" style={{ padding: 20 }}>
                        <div style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 8 }}>{card.label}</div>
                        <div style={{ fontSize: 28, fontWeight: 700 }}>{card.value ?? '-'}</div>
                        {card.sub && <div style={{ fontSize: 12, color: 'var(--success)', marginTop: 4 }}>{card.sub}</div>}
                    </div>
                ))}
            </div>
            {!data && <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 40 }}>加载中...</div>}

            {data && (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                    <div className="card">
                        <h3 style={{ fontSize: 15, marginBottom: 12 }}>用户增长</h3>
                        <div style={{ color: 'var(--text-secondary)', fontSize: 13, padding: 24, textAlign: 'center' }}>
                            图表区域（可对接 ECharts）
                        </div>
                    </div>
                    <div className="card">
                        <h3 style={{ fontSize: 15, marginBottom: 12 }}>Token消耗趋势</h3>
                        <div style={{ color: 'var(--text-secondary)', fontSize: 13, padding: 24, textAlign: 'center' }}>
                            图表区域（可对接 ECharts）
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}