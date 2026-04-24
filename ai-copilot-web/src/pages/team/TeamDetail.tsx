import request from '@/utils/request';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import styles from './Team.module.css';

interface TeamInfo {
    id: number;
    name: string;
    description: string;
    inviteCode: string;
    status: string;
}

interface Member {
    id: number;
    userId: number;
    role: string;
    status: string;
}

export default function TeamDetail() {
    const { id } = useParams();
    const [team, setTeam] = useState<TeamInfo | null>(null);
    const [members, setMembers] = useState<Member[]>([]);
    const [showInviteModal, setShowInviteModal] = useState(false);
    const [inviteEmail, setInviteEmail] = useState('');
    const [inviteLoading, setInviteLoading] = useState(false);
    const [inviteMsg, setInviteMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    useEffect(() => {
        const fetchDetail = async () => {
            const [teamRes, membersRes]: any[] = await Promise.all([
                request.get(`/teams/${id}`),
                request.get(`/teams/${id}/members`),
            ]);
            setTeam(teamRes.data);
            setMembers(membersRes.data);
        };
        fetchDetail();
    }, [id]);

    const fetchMembers = async () => {
        const res: any = await request.get(`/teams/${id}/members`);
        setMembers(res.data);
    };

    const handleApprove = async (userId: number, approve: boolean) => {
        const action = approve ? 'approve' : 'reject';
        await request.put(`/teams/${id}/members/${userId}/review`, { action });
        await fetchMembers();
    };

    const handleInvite = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!inviteEmail.trim()) return;
        setInviteLoading(true);
        setInviteMsg(null);
        try {
            await request.post(`/teams/${id}/invite`, { email: inviteEmail.trim() });
            setInviteMsg({ type: 'success', text: '邀请邮件已发送！' });
            setInviteEmail('');
        } catch (err: any) {
            setInviteMsg({ type: 'error', text: err.message || '发送失败' });
        } finally {
            setInviteLoading(false);
        }
    };

    const closeInviteModal = () => {
        setShowInviteModal(false);
        setInviteEmail('');
        setInviteMsg(null);
    };

    if (!team) return <div className={styles.container}>加载中...</div>;

    return (
        <div className={styles.container}>
            <div className={styles.detailHeader}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h1 className={styles.title}>{team.name}</h1>
                    <button className="btn-primary" onClick={() => setShowInviteModal(true)}>
                        邀请成员
                    </button>
                </div>
                {team.description && (
                    <p className={styles.subtitle}>{team.description}</p>
                )}
            </div>

            <div className={styles.inviteSection}>
                <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 8 }}>邀请码</p>
                <span className={styles.inviteCode}>{team.inviteCode}</span>
            </div>

            <h2 style={{ fontSize: 18, marginBottom: 16 }}>成员 ({members.length})</h2>
            <div className={styles.memberList}>
                {members.map((m) => (
                    <div key={m.id} className={styles.memberItem}>
                        <div className={styles.memberInfo}>
                            <div className={styles.memberAvatar}>U</div>
                            <div>
                                <div className={styles.memberName}>用户 {m.userId}</div>
                                <div className={styles.memberEmail}>{m.role}</div>
                            </div>
                        </div>
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                            <span className={styles.memberRole}>{m.role}</span>
                            {m.status === 'pending' ? (
                                <>
                                    <button className="btn-primary" style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => handleApprove(m.userId, true)}>通过</button>
                                    <button className="btn-outline" style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => handleApprove(m.userId, false)}>拒绝</button>
                                </>
                            ) : (
                                <span className={styles.memberStatus}>{m.status}</span>
                            )}
                        </div>
                    </div>
                ))}
            </div>

            {showInviteModal && (
                <div className={styles.modal} onClick={closeInviteModal}>
                    <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                        <h2>邀请成员加入团队</h2>
                        <form onSubmit={handleInvite}>
                            <div className={styles.field}>
                                <label>被邀请人邮箱</label>
                                <input
                                    type="email"
                                    placeholder="请输入邮箱地址"
                                    value={inviteEmail}
                                    onChange={(e) => setInviteEmail(e.target.value)}
                                    required
                                    autoFocus
                                />
                            </div>
                            {inviteMsg && (
                                <div className={inviteMsg.type === 'success' ? styles.inviteSuccess : styles.inviteError}>
                                    {inviteMsg.text}
                                </div>
                            )}
                            <div className={styles.modalActions}>
                                <button type="button" className="btn-outline" onClick={closeInviteModal}>取消</button>
                                <button type="submit" className="btn-primary" disabled={inviteLoading}>
                                    {inviteLoading ? '发送中...' : '发送邀请'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}