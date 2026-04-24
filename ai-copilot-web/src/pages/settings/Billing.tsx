import request from '@/utils/request';
import { useEffect, useState } from 'react';
import styles from './Settings.module.css';

interface UsageOverview {
    month: number;
    free_quota: { total: number; used: number; remaining: number };
}

interface Plan {
    id: number;
    name: string;
    type: string;
    monthlyTokenLimit: number;
    price: number;
    status: string;
}

export default function Billing() {
    const [overview, setOverview] = useState<UsageOverview | null>(null);
    const [plans, setPlans] = useState<Plan[]>([]);

    useEffect(() => {
        request.get('/usage/overview').then((res: any) => setOverview(res.data));
        request.get('/billing/plans').then((res: any) => setPlans(res.data || []));
    }, []);

    const total = overview?.free_quota?.total || 0;
    const used = overview?.free_quota?.used || 0;
    const usagePercent = total > 0 ? Math.min(100, (used / total) * 100) : 0;

    return (
        <div className={styles.container}>
            <h1 className={styles.title}>计费与额度</h1>
            <p className={styles.subtitle}>查看当前套餐和用量</p>

            {overview && (
                <div className={styles.quotaCard}>
                    <div className={styles.quotaHeader}>
                        <span className={styles.planBadge}>免费额度</span>
                        <span className={styles.expires}>{overview.month} 月用量</span>
                    </div>
                    <div className={styles.usageBar}>
                        <div className={styles.usageBarInner} style={{ width: `${usagePercent}%` }} />
                    </div>
                    <div className={styles.usageText}>
                        已使用 {used.toLocaleString()} / {total.toLocaleString()} tokens ({usagePercent.toFixed(1)}%)
                    </div>
                </div>
            )}

            <h2 style={{ fontSize: 20, marginTop: 40, marginBottom: 20 }}>可用套餐</h2>
            <div className={styles.planGrid}>
                {plans.map((p) => (
                    <div key={p.id} className={styles.planCard}>
                        <h3>{p.name}</h3>
                        <div className={styles.price}>
                            {p.price > 0 ? `¥${p.price}/月` : '免费'}
                        </div>
                        <ul className={styles.features}>
                            <li>{p.monthlyTokenLimit >= 10000 ? `${(p.monthlyTokenLimit / 10000).toFixed(0)}万` : p.monthlyTokenLimit.toLocaleString()} tokens/月</li>
                        </ul>
                        <button className="btn-primary" style={{ width: '100%', marginTop: 12 }}>
                            {p.price > 0 ? '升级' : '当前'}
                        </button>
                    </div>
                ))}
            </div>
        </div>
    );
}