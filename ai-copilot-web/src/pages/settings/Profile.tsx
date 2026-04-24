import { useAuthStore } from '@/stores/auth';
import request from '@/utils/request';
import { useState } from 'react';
import styles from './Settings.module.css';

export default function Profile() {
    const { user, setUser } = useAuthStore();
    const [nickname, setNickname] = useState(user?.nickname || '');
    const [phone, setPhone] = useState(user?.phone || '');
    const [saved, setSaved] = useState(false);

    const handleSave = async () => {
        const res: any = await request.put('/user/profile', { nickname, phone });
        setUser(res.data);
        setSaved(true);
        setTimeout(() => setSaved(false), 2000);
    };

    return (
        <div className={styles.container}>
            <h1 className={styles.title}>个人资料</h1>
            <p className={styles.subtitle}>管理你的账号信息</p>

            <div className={styles.profileForm}>
                <div className={styles.field}>
                    <label>邮箱</label>
                    <input value={user?.email || ''} disabled />
                </div>
                <div className={styles.field}>
                    <label>昵称</label>
                    <input value={nickname} onChange={(e) => setNickname(e.target.value)} />
                </div>
                <div className={styles.field}>
                    <label>手机号</label>
                    <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="可选" />
                </div>
                <div style={{ marginTop: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
                    <button className="btn-primary" onClick={handleSave}>保存</button>
                    {saved && <span style={{ color: 'var(--success)', fontSize: 13 }}>已保存</span>}
                </div>
            </div>
        </div>
    );
}