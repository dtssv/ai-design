import { useAuthStore } from '@/stores/auth';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Login() {
    const [account, setAccount] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const { login, loading } = useAuthStore();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        try {
            await login(account, password);
            navigate('/dashboard');
        } catch (err: any) {
            setError(err.message || '登录失败');
        }
    };

    return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', background: 'var(--bg)' }}>
            <div className="card" style={{ width: 380, padding: 32 }}>
                <div style={{ textAlign: 'center', marginBottom: 24 }}>
                    <div style={{
                        width: 48, height: 48, background: 'var(--primary)', borderRadius: 12, margin: '0 auto 12px',
                        display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontWeight: 700, fontSize: 18,
                    }}>AI</div>
                    <h1 style={{ fontSize: 20, fontWeight: 600, marginBottom: 4 }}>管理后台</h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>请使用管理员账号登录</p>
                </div>
                <form onSubmit={handleSubmit}>
                    {error && <div style={{ background: 'rgba(239,68,68,0.1)', color: 'var(--danger)', padding: '8px 12px', borderRadius: 6, fontSize: 13, marginBottom: 12 }}>{error}</div>}
                    <div style={{ marginBottom: 16 }}>
                        <label style={{ display: 'block', fontSize: 13, marginBottom: 4, color: 'var(--text-secondary)' }}>邮箱 / 手机号</label>
                        <input style={{ width: '100%' }} value={account} onChange={(e) => setAccount(e.target.value)} placeholder="admin@example.com" required />
                    </div>
                    <div style={{ marginBottom: 20 }}>
                        <label style={{ display: 'block', fontSize: 13, marginBottom: 4, color: 'var(--text-secondary)' }}>密码</label>
                        <input type="password" style={{ width: '100%' }} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="输入密码" required />
                    </div>
                    <button type="submit" className="btn-primary" style={{ width: '100%', padding: '10px 0', fontSize: 15 }} disabled={loading}>
                        {loading ? '登录中...' : '登录'}
                    </button>
                </form>
            </div>
        </div>
    );
}