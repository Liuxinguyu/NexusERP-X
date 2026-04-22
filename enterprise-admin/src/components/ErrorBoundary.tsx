import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props { children: ReactNode }
interface State { hasError: boolean }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack)
  }

  render() {
    if (!this.state.hasError) return this.props.children
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 p-8">
        <div className="text-center space-y-6 max-w-md">
          <div className="text-6xl">⚠</div>
          <h1 className="text-xl font-black text-slate-900">页面出现异常</h1>
          <p className="text-sm text-slate-500">请尝试刷新页面，如果问题持续请联系管理员。</p>
          <button
            onClick={() => window.location.reload()}
            className="px-8 py-3 bg-indigo-600 text-white rounded-2xl text-sm font-black shadow-lg hover:bg-indigo-500 transition-all"
          >
            刷新页面
          </button>
        </div>
      </div>
    )
  }
}
