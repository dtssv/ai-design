import { useAuthStore } from '@/stores/auth';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import styles from './Auth.module.css';

export default function Register() {
    const [searchParams] = useSearchParams();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [nickname, setNickname] = useState('');
    const [inviteCode, setInviteCode] = useState(searchParams.get('inviteCode') || '');
    const [error, setError] = useState('');
    const { register, loading } = useAuthStore();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        try {
            await register(email, password, nickname, inviteCode || undefined);
            navigate('/dashboard');
        } catch (err: any) {
            setError(err.message || '注册失败');
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.card}>
                <div className={styles.header}>
                    <div className={styles.logoIcon}>AI</div>
                    <h1 className={styles.title}>创建账号</h1>
                    <p className={styles.subtitle}>开始使用 AI Copilot 生成前端页面</p>
                </div>
                <form onSubmit={handleSubmit} className={styles.form}>
                    {error && <div className={styles.error}>{error}</div>}
                    <div className={styles.field}>
                        <label>昵称</label>
                        <input
                            type="text"
                            placeholder="你的昵称"
                            value={nickname}
                            onChange={(e) => setNickname(e.target.value)}
                            required
                        />
                    </div>
                    <div className={styles.field}>
                        <label>邮箱</label>
                        <input
                            type="email"
                            placeholder="your@email.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>
                    <div className={styles.field}>
                        <label>密码</label>
                        <input
                            type="password"
                            placeholder="至少6位密码"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            minLength={6}
                        />
                    </div>
                    <div className={styles.field}>
                        <label>团队邀请码（可选）</label>
                        <input
                            type="text"
                            placeholder="输入邀请码加入已有团队"
                            value={inviteCode}
                            onChange={(e) => setInviteCode(e.target.value)}
                        />
                    </div>
                    <button type="submit" className={`btn-primary ${styles.submitBtn}`} disabled={loading}>
                        {loading ? '注册中...' : '注册'}
                    </button>
                </form>
                <div className={styles.footer}>
                    已有账号？<Link to="/login">去登录</Link>
                </div>
            </div>
        </div>
    );
}