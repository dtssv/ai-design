import request from '@/utils/request';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Team.module.css';

interface Team {
    id: number;
    name: string;
    plan: string;
    memberCount: number;
    role: string;
    createdAt: string;
}

export default function TeamList() {
    const [teams, setTeams] = useState<Team[]>([]);
    const [showCreate, setShowCreate] = useState(false);
    const [showJoin, setShowJoin] = useState(false);
    const [name, setName] = useState('');
    const [inviteCode, setInviteCode] = useState('');
    const navigate = useNavigate();

    const fetchTeams = async () => {
        const res: any = await request.get('/teams');
        setTeams(res.data);
    };

    useEffect(() => { fetchTeams(); }, []);

    const handleCreate = async () => {
        if (!name.trim()) return;
        await request.post('/teams', { name });
        setShowCreate(false);
        setName('');
        fetchTeams();
    };

    const handleJoin = async () => {
        if (!inviteCode.trim()) return;
        await request.post('/teams/join', { inviteCode });
        setShowJoin(false);
        setInviteCode('');
        fetchTeams();
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <div>
                    <h1 className={styles.title}>我的团队</h1>
                    <p className={styles.subtitle}>管理和加入团队</p>
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                    <button className="btn-outline" onClick={() => setShowJoin(true)}>加入团队</button>
                    <button className="btn-primary" onClick={() => setShowCreate(true)}>+ 创建团队</button>
                </div>
            </div>

            <div className={styles.list}>
                {teams.map((team) => (
                    <div key={team.id} className={styles.card} onClick={() => navigate(`/teams/${team.id}`)}>
                        <div className={styles.cardLeft}>
                            <div className={styles.teamAvatar}>{team.name[0]}</div>
                            <div>
                                <h3>{team.name}</h3>
                                <p className={styles.meta}>{team.memberCount} 成员 · {team.plan}套餐 · {team.role}</p>
                            </div>
                        </div>
                        <span className={styles.arrow}>&rarr;</span>
                    </div>
                ))}
                {teams.length === 0 && <div className={styles.empty}>还没有团队，创建或加入一个吧</div>}
            </div>

            {/* 创建弹窗 */}
            {showCreate && (
                <div className={styles.modal} onClick={() => setShowCreate(false)}>
                    <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                        <h2>创建团队</h2>
                        <div className={styles.field}>
                            <label>团队名称</label>
                            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="输入团队名称" autoFocus />
                        </div>
                        <div className={styles.modalActions}>
                            <button className="btn-outline" onClick={() => setShowCreate(false)}>取消</button>
                            <button className="btn-primary" onClick={handleCreate}>创建</button>
                        </div>
                    </div>
                </div>
            )}

            {/* 加入弹窗 */}
            {showJoin && (
                <div className={styles.modal} onClick={() => setShowJoin(false)}>
                    <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                        <h2>加入团队</h2>
                        <div className={styles.field}>
                            <label>邀请码</label>
                            <input value={inviteCode} onChange={(e) => setInviteCode(e.target.value)} placeholder="输入团队邀请码" autoFocus />
                        </div>
                        <div className={styles.modalActions}>
                            <button className="btn-outline" onClick={() => setShowJoin(false)}>取消</button>
                            <button className="btn-primary" onClick={handleJoin}>加入</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}