import AdminLayout from '@/layouts/AdminLayout';
import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';

const Login = lazy(() => import('@/pages/auth/Login'));
const Dashboard = lazy(() => import('@/pages/dashboard/Dashboard'));
const Users = lazy(() => import('@/pages/users/Users'));
const Teams = lazy(() => import('@/pages/teams/Teams'));
const KnowledgeReview = lazy(() => import('@/pages/knowledge/KnowledgeReview'));
const ApiKeys = lazy(() => import('@/pages/api-keys/ApiKeys'));
const QuotaPlans = lazy(() => import('@/pages/quota/QuotaPlans'));
const Orders = lazy(() => import('@/pages/orders/Orders'));
const Configs = lazy(() => import('@/pages/configs/Configs'));
const Logs = lazy(() => import('@/pages/logs/Logs'));

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
        path: '/login',
        element: wrap(Login),
    },
    {
        path: '/',
        element: <AdminLayout />,
        children: [
            { index: true, element: <Navigate to="/dashboard" replace /> },
            { path: 'dashboard', element: wrap(Dashboard) },
            { path: 'users', element: wrap(Users) },
            { path: 'teams', element: wrap(Teams) },
            { path: 'knowledge', element: wrap(KnowledgeReview) },
            { path: 'api-keys', element: wrap(ApiKeys) },
            { path: 'quota', element: wrap(QuotaPlans) },
            { path: 'orders', element: wrap(Orders) },
            { path: 'configs', element: wrap(Configs) },
            { path: 'logs', element: wrap(Logs) },
        ],
    },
]);

export default router;