import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = { children: ReactNode }

type State = { error: Error | null }

export class RouteErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[RouteErrorBoundary]', error, info.componentStack)
  }

  private handleRetry = () => {
    this.setState({ error: null })
    window.location.reload()
  }

  render() {
    if (this.state.error) {
      return (
        <div className="flex min-h-[40vh] flex-col items-center justify-center gap-4 p-12 text-center">
          <p className="text-sm font-black text-rose-600">页面块加载失败</p>
          <p className="max-w-md text-xs font-bold text-slate-500">
            {this.state.error.message || '请检查网络后重试'}
          </p>
          <button
            type="button"
            onClick={this.handleRetry}
            className="rounded-2xl bg-indigo-600 px-6 py-3 text-xs font-black text-white shadow-lg shadow-indigo-200 hover:bg-indigo-500"
          >
            重新加载
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
