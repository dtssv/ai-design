import AppLayout from '@/layouts/AppLayout';
import { lazy, Suspense } from 'react';
import { createBrowserRouter } from 'react-router-dom';

// 懒加载页面
const Home = lazy(() => import('@/pages/home/Home'));
const Login = lazy(() => import('@/pages/auth/Login'));
const Register = lazy(() => import('@/pages/auth/Register'));
const Dashboard = lazy(() => import('@/pages/dashboard/Dashboard'));
const WorkspaceMain = lazy(() => import('@/pages/workspace/WorkspaceMain'));
const TeamList = lazy(() => import('@/pages/team/TeamList'));
const TeamDetail = lazy(() => import('@/pages/team/TeamDetail'));
const KnowledgeBase = lazy(() => import('@/pages/knowledge/KnowledgeBase'));
const KnowledgeDetail = lazy(() => import('@/pages/knowledge/KnowledgeDetail'));
const ApiKeys = lazy(() => import('@/pages/settings/ApiKeys'));
const Billing = lazy(() => import('@/pages/settings/Billing'));
const Profile = lazy(() => import('@/pages/settings/Profile'));
const SharePreview = lazy(() => import('@/pages/share/SharePreview'));

const Loading = () => (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', color: '#a1a1aa' }}>
        加载中...
    </div>
);

const wrap = (Component: React.LazyExoticComponent<() => JSX.Element>) => (
    <Suspense fallback={<Loading />}>
        <Component />
    </Suspense>
);

const router = createBrowserRouter([
    {
        path: '/',
        element: wrap(Home),
    },
    {
        path: '/login',
        element: wrap(Login),
    },
    {
        path: '/register',
        element: wrap(Register),
    },
    {
        path: '/share/:token',
        element: wrap(SharePreview),
    },
    {
        path: '/',
        element: <AppLayout />,
        children: [
            { path: 'dashboard', element: wrap(Dashboard) },
            { path: 'workspace/:id', element: wrap(WorkspaceMain) },
            { path: 'teams', element: wrap(TeamList) },
            { path: 'teams/:id', element: wrap(TeamDetail) },
            { path: 'knowledge', element: wrap(KnowledgeBase) },
            { path: 'knowledge/:id', element: wrap(KnowledgeDetail) },
            { path: 'api-keys', element: wrap(ApiKeys) },
            { path: 'billing', element: wrap(Billing) },
            { path: 'profile', element: wrap(Profile) },
        ],
    },
]);

export default router;