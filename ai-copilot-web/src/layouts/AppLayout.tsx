import { useAuthStore } from '@/stores/auth';
import { useEffect } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import styles from './AppLayout.module.css';

export default function AppLayout() {
    const { token, user, fetchProfile, logout } = useAuthStore();
    const navigate = useNavigate();

    useEffect(() => {
        if (!token) {
            navigate('/login');
            return;
        }
        if (!user) {
            fetchProfile().catch(() => {
                logout();
            });
        }
    }, [token]);

    if (!token) return null;

    return (
        <div className={styles.layout}>
            <aside className={styles.sidebar}>
                <div className={styles.logo}>
                    <span className={styles.logoIcon}>AI</span>
                    <span className={styles.logoText}>Copilot</span>
                </div>
                <nav className={styles.nav}>
                    <NavLink to="/dashboard" className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></svg>
                        工作台
                    </NavLink>
                    <NavLink to="/teams" className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></svg>
                        团队
                    </NavLink>
                    <NavLink to="/knowledge" className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" /></svg>
                        知识库
                    </NavLink>
                    <NavLink to="/api-keys" className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4" /></svg>
                        API-Key
                    </NavLink>
                    <NavLink to="/billing" className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="1" y="4" width="22" height="16" rx="2" ry="2" /><line x1="1" y1="10" x2="23" y2="10" /></svg>
                        计费
                    </NavLink>
                </nav>
                <div className={styles.userSection}>
                    <div className={styles.avatar}>{user?.nickname?.[0] || 'U'}</div>
                    <div className={styles.userInfo}>
                        <div className={styles.userName}>{user?.nickname || '用户'}</div>
                        <div className={styles.userEmail}>{user?.email}</div>
                    </div>
                    <button className={styles.logoutBtn} onClick={logout} title="退出">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" /></svg>
                    </button>
                </div>
            </aside>
            <main className={styles.main}>
                <Outlet />
            </main>
        </div>
    );
}