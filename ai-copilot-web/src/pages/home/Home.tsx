import { useAuthStore } from '@/stores/auth';
import { Link, useNavigate } from 'react-router-dom';
import styles from './Home.module.css';

const FEATURES = [
    {
        icon: '🤖',
        bg: 'rgba(99, 102, 241, 0.12)',
        title: 'AI 智能对话',
        desc: '基于大语言模型，支持多轮上下文对话、代码生成、文档撰写，让 AI 成为你的全能助手。',
    },
    {
        icon: '📚',
        bg: 'rgba(34, 197, 94, 0.12)',
        title: '知识库增强',
        desc: '上传私有文档构建专属知识库，AI 回答基于你的数据，精准度提升 10 倍。',
    },
    {
        icon: '👥',
        bg: 'rgba(168, 85, 247, 0.12)',
        title: '团队协作',
        desc: '创建团队空间，共享知识库与对话历史，统一管理团队成员与权限。',
    },
    {
        icon: '🔑',
        bg: 'rgba(245, 158, 11, 0.12)',
        title: 'API 集成',
        desc: '提供标准 RESTful API 和 API-Key 管理，轻松集成到现有业务系统。',
    },
    {
        icon: '📊',
        bg: 'rgba(59, 130, 246, 0.12)',
        title: '用量监控',
        desc: '实时查看 Token 消耗与请求统计，灵活的额度套餐满足不同规模需求。',
    },
    {
        icon: '🛡️',
        bg: 'rgba(239, 68, 68, 0.12)',
        title: '安全可控',
        desc: '企业级数据隔离，支持私有化部署，所有数据加密存储，安全合规有保障。',
    },
];

const STATS = [
    { value: '10K+', label: '活跃用户' },
    { value: '500万+', label: '日均对话' },
    { value: '99.9%', label: '服务可用率' },
    { value: '50ms', label: '平均响应' },
];

export default function Home() {
    const { token } = useAuthStore();
    const navigate = useNavigate();

    return (
        <div>
            {/* 导航栏 */}
            <nav className={styles.nav}>
                <div className={styles.navLogo}>
                    <div className={styles.navLogoIcon}>AI</div>
                    <span className={styles.navLogoText}>AI Copilot</span>
                </div>
                <div className={styles.navActions}>
                    {token ? (
                        <button
                            className={`${styles.navBtn} ${styles.navBtnPrimary}`}
                            onClick={() => navigate('/dashboard')}
                        >
                            进入工作台
                        </button>
                    ) : (
                        <>
                            <Link to="/login" className={`${styles.navBtn} ${styles.navBtnGhost}`}>登录</Link>
                            <Link to="/register" className={`${styles.navBtn} ${styles.navBtnPrimary}`}>免费注册</Link>
                        </>
                    )}
                </div>
            </nav>

            {/* Hero */}
            <section className={styles.hero}>
                <div className={styles.heroBg} />
                <div className={styles.heroBadge}>
                    <span className={styles.heroBadgeDot} />
                    全新发布 — 知识库增强检索上线
                </div>
                <h1 className={styles.heroTitle}>
                    用 AI 重新定义<br /><span className={styles.heroGradient}>你的工作方式</span>
                </h1>
                <p className={styles.heroDesc}>
                    AI Copilot 是面向团队和个人的智能助手平台。结合私有知识库与大语言模型，
                    让每一次对话都精准高效，释放你的创造力。
                </p>
                <div className={styles.heroActions}>
                    <button
                        className={`${styles.heroBtn} ${styles.heroBtnPrimary}`}
                        onClick={() => navigate(token ? '/dashboard' : '/register')}
                    >
                        {token ? '进入工作台' : '免费开始使用'}
                    </button>
                    <button
                        className={`${styles.heroBtn} ${styles.heroBtnOutline}`}
                        onClick={() => document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' })}
                    >
                        了解更多
                    </button>
                </div>
            </section>

            {/* 功能特性 */}
            <section className={styles.features} id="features">
                <div className={styles.sectionHeader}>
                    <div className={styles.sectionTag}>核心能力</div>
                    <h2 className={styles.sectionTitle}>为效率而生的全方位 AI 平台</h2>
                    <p className={styles.sectionDesc}>
                        从智能对话到知识管理，从个人助手到团队协作，一站式覆盖你的 AI 需求。
                    </p>
                </div>
                <div className={styles.featureGrid}>
                    {FEATURES.map((f) => (
                        <div key={f.title} className={styles.featureCard}>
                            <div className={styles.featureIcon} style={{ background: f.bg }}>{f.icon}</div>
                            <h3 className={styles.featureTitle}>{f.title}</h3>
                            <p className={styles.featureDesc}>{f.desc}</p>
                        </div>
                    ))}
                </div>
            </section>

            {/* 数据统计 */}
            <section className={styles.stats}>
                <div className={styles.statsGrid}>
                    {STATS.map((s) => (
                        <div key={s.label}>
                            <div className={styles.statValue}>{s.value}</div>
                            <div className={styles.statLabel}>{s.label}</div>
                        </div>
                    ))}
                </div>
            </section>

            {/* 底部 CTA */}
            <section className={styles.cta}>
                <h2 className={styles.ctaTitle}>准备好提升效率了吗？</h2>
                <p className={styles.ctaDesc}>注册即可获得免费额度，无需信用卡，即刻体验 AI Copilot 的强大能力。</p>
                <button
                    className={`${styles.heroBtn} ${styles.heroBtnPrimary}`}
                    onClick={() => navigate(token ? '/dashboard' : '/register')}
                >
                    {token ? '进入工作台' : '立即免费注册'}
                </button>
            </section>

            {/* Footer */}
            <footer className={styles.footer}>
                <span>&copy; 2025 AI Copilot. All rights reserved.</span>
                <div className={styles.footerLinks}>
                    <a href="#">隐私政策</a>
                    <a href="#">服务条款</a>
                    <a href="#">帮助中心</a>
                </div>
            </footer>
        </div>
    );
}