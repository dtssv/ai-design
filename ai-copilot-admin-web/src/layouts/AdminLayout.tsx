import { useAuthStore } from '@/stores/auth';
import { useEffect } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';

const navItems = [
    { path: '/dashboard', label: '数据看板' },
    { path: '/users', label: '用户管理' },
    { path: '/teams', label: '团队管理' },
    { path: '/knowledge', label: '知识库管理' },
    { path: '/api-keys', label: 'API-Key池' },
    { path: '/quota', label: '额度套餐' },
    { path: '/orders', label: '订单管理' },
    { path: '/configs', label: '系统配置' },
    { path: '/logs', label: '操作日志' },
];

export default function AdminLayout() {
    const { token, user, fetchProfile, logout } = useAuthStore();
    const navigate = useNavigate();

    useEffect(() => {
        if (!token) { navigate('/login'); return; }
        if (!user) { fetchProfile().catch(() => { logout(); }); }
    }, [token]);

    if (!token) return null;

    return (
        <div style={{ display: 'flex', height: '100vh' }}>
            <aside style={{
                width: 200, background: '#18181b', borderRight: '1px solid var(--border)',
                display: 'flex', flexDirection: 'column', padding: '16px 8px',
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px', marginBottom: 24 }}>
                    <span style={{
                        background: 'var(--primary)', color: '#fff', fontWeight: 700, fontSize: 12,
                        width: 28, height: 28, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 6,
                    }}>AI</span>
                    <span style={{ fontSize: 16, fontWeight: 600 }}>Admin</span>
                </div>
                <nav style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
                    {navItems.map((item) => (
                        <NavLink key={item.path} to={item.path}
                            style={({ isActive }) => ({
                                display: 'block', padding: '8px 12px', borderRadius: 6, fontSize: 13,
                                color: isActive ? 'var(--primary)' : 'var(--text-secondary)',
                                background: isActive ? 'rgba(99,102,241,0.12)' : 'transparent',
                                textDecoration: 'none', transition: 'all 0.15s',
                            })}
                        >
                            {item.label}
                        </NavLink>
                    ))}
                </nav>
                <div style={{
                    display: 'flex', alignItems: 'center', gap: 8, padding: '12px 8px',
                    borderTop: '1px solid var(--border)', marginTop: 8,
                }}>
                    <div style={{
                        width: 32, height: 32, borderRadius: '50%', background: 'var(--primary)',
                        color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontWeight: 600, fontSize: 13, flexShrink: 0,
                    }}>{user?.nickname?.[0] || 'A'}</div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 12, fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            {user?.nickname || '管理员'}
                        </div>
                    </div>
                    <button onClick={logout} style={{
                        background: 'transparent', color: 'var(--text-secondary)', padding: 4, borderRadius: 4, fontSize: 12,
                    }}>退出</button>
                </div>
            </aside>
            <main style={{ flex: 1, overflowY: 'auto', padding: 24 }}>
                <Outlet />
            </main>
        </div>
    );
}