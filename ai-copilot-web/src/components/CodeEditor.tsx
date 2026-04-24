import Editor from '@monaco-editor/react';
import { useCallback, useEffect, useRef, useState } from 'react';

/** 文件扩展名 → Monaco 语言映射 */
const LANG_MAP: Record<string, string> = {
    js: 'javascript',
    jsx: 'javascript',
    ts: 'typescript',
    tsx: 'typescript',
    css: 'css',
    scss: 'scss',
    less: 'less',
    html: 'html',
    json: 'json',
    md: 'markdown',
    xml: 'xml',
    yaml: 'yaml',
    yml: 'yaml',
    py: 'python',
    java: 'java',
    sql: 'sql',
    sh: 'shell',
    vue: 'html',
    svg: 'xml',
};

function resolveLanguage(filePath: string, language?: string): string {
    if (language && language !== 'txt') return language;
    const ext = filePath.split('.').pop()?.toLowerCase() || '';
    return LANG_MAP[ext] || 'plaintext';
}

export interface CodeEditorProps {
    /** 文件路径 */
    filePath: string;
    /** 语言标识 */
    language?: string;
    /** 文件内容（仅作为初始值 / 外部重置值） */
    value: string;
    /** 是否只读 */
    readOnly?: boolean;
    /** 自动保存回调：编辑停止 N 秒后触发，参数为最新内容 */
    onAutoSave?: (filePath: string, newValue: string) => void;
    /** 自动保存延迟（毫秒），默认 10000 */
    autoSaveDelay?: number;
}

export default function CodeEditor({
    filePath,
    language,
    value,
    readOnly = false,
    onAutoSave,
    autoSaveDelay = 10000,
}: CodeEditorProps) {
    const lang = resolveLanguage(filePath, language);

    // 编辑器内部维护的内容，不回写外部 store
    const [localValue, setLocalValue] = useState(value);
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const savedValueRef = useRef(value);
    const [saveStatus, setSaveStatus] = useState<'saved' | 'unsaved' | 'saving'>('saved');

    // 外部 value 变化时（切换文件 / 加载快照），重置本地编辑态
    useEffect(() => {
        setLocalValue(value);
        savedValueRef.current = value;
        setSaveStatus('saved');
        if (timerRef.current) {
            clearTimeout(timerRef.current);
            timerRef.current = null;
        }
    }, [filePath, value]);

    // 组件卸载时，若有未保存内容，立即触发保存
    const onAutoSaveRef = useRef(onAutoSave);
    onAutoSaveRef.current = onAutoSave;
    const filePathRef = useRef(filePath);
    filePathRef.current = filePath;
    const localValueRef = useRef(localValue);
    localValueRef.current = localValue;

    useEffect(() => {
        return () => {
            if (timerRef.current) {
                clearTimeout(timerRef.current);
                timerRef.current = null;
            }
            if (localValueRef.current !== savedValueRef.current) {
                onAutoSaveRef.current?.(filePathRef.current, localValueRef.current);
            }
        };
    }, []);

    const handleChange = useCallback(
        (newValue: string | undefined) => {
            const v = newValue ?? '';
            setLocalValue(v);
            localValueRef.current = v;

            if (v !== savedValueRef.current) {
                setSaveStatus('unsaved');
            } else {
                setSaveStatus('saved');
            }

            // 重置自动保存定时器
            if (timerRef.current) clearTimeout(timerRef.current);
            if (!readOnly) {
                timerRef.current = setTimeout(() => {
                    const latest = localValueRef.current;
                    if (latest !== savedValueRef.current) {
                        setSaveStatus('saving');
                        onAutoSaveRef.current?.(filePathRef.current, latest);
                        savedValueRef.current = latest;
                        setTimeout(() => setSaveStatus('saved'), 600);
                    }
                    timerRef.current = null;
                }, autoSaveDelay);
            }
        },
        [readOnly, autoSaveDelay],
    );

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '6px 16px', fontSize: 12, color: '#999',
                borderBottom: '1px solid var(--border, #2d2d30)', background: '#1e1e22',
                fontFamily: "'Menlo','Monaco','Courier New',monospace", flexShrink: 0,
            }}>
                <span>{filePath}</span>
                {!readOnly && (
                    <span style={{
                        fontSize: 11, padding: '1px 6px', borderRadius: 3,
                        background: saveStatus === 'unsaved' ? 'rgba(245,158,11,0.15)' : saveStatus === 'saving' ? 'rgba(99,102,241,0.15)' : 'rgba(16,185,129,0.15)',
                        color: saveStatus === 'unsaved' ? '#f59e0b' : saveStatus === 'saving' ? '#6366f1' : '#10b981',
                    }}>
                        {saveStatus === 'unsaved' ? '未保存' : saveStatus === 'saving' ? '保存中...' : '已保存'}
                    </span>
                )}
            </div>
            <div style={{ flex: 1, minHeight: 0 }}>
                <Editor
                    language={lang}
                    value={localValue}
                    theme="vs-dark"
                    onChange={handleChange}
                    options={{
                        readOnly, minimap: { enabled: false }, fontSize: 13, lineHeight: 20,
                        scrollBeyondLastLine: false, wordWrap: 'on', tabSize: 2,
                        automaticLayout: true, padding: { top: 12 },
                    }}
                />
            </div>
        </div>
    );
}