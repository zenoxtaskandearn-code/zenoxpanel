import React from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { LayoutDashboard, Smartphone, Images, Activity, Settings, UserCircle, LogOut, Sun, Moon, Shield } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

const nav = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/devices', label: 'Devices', icon: Smartphone },
  { to: '/gallery', label: 'Gallery', icon: Images },
  { to: '/activity', label: 'Activity Logs', icon: Activity },
  { to: '/settings', label: 'Settings', icon: Settings },
  { to: '/profile', label: 'Profile', icon: UserCircle },
]

export default function AppLayout() {
  const { user, logout } = useAuth()
  const { theme, toggle } = useTheme()
  const navigate = useNavigate()

  const onLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100">
      <div className="flex min-h-screen">
        <aside className="hidden md:flex md:w-64 md:flex-col border-r bg-white/80 dark:bg-slate-900/60 backdrop-blur">
          <div className="h-16 px-5 flex items-center gap-2 border-b">
            <div className="h-8 w-8 rounded-lg bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 flex items-center justify-center">
              <Shield size={16} />
            </div>
            <div>
              <div className="text-sm font-semibold leading-tight">Gallery Admin</div>
              <div className="text-xs text-slate-500">Remote Management</div>
            </div>
          </div>
          <nav className="flex-1 p-3 space-y-1">
            {nav.map(item => {
              const Icon = item.icon
              return (
                <NavLink key={item.to} to={item.to} end={item.end}
                  className={({ isActive }) => `flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition ${
                    isActive ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}>
                  <Icon size={16} /> {item.label}
                </NavLink>
              )
            })}
          </nav>
          <div className="p-3 border-t space-y-2">
            <button onClick={toggle} className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800">
              {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />} {theme === 'dark' ? 'Light mode' : 'Dark mode'}
            </button>
            <button onClick={onLogout} className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/30">
              <LogOut size={16} /> Sign out
            </button>
          </div>
        </aside>
        <div className="flex-1 flex flex-col min-w-0">
          <header className="h-16 border-b bg-white/70 dark:bg-slate-900/50 backdrop-blur px-4 md:px-8 flex items-center justify-between sticky top-0 z-10">
            <div className="md:hidden font-semibold text-sm">Gallery Admin</div>
            <div className="hidden md:block text-sm text-slate-500">Authenticated administration console</div>
            <div className="flex items-center gap-3">
              <button onClick={toggle} className="md:hidden p-2 rounded-md hover:bg-slate-100 dark:hover:bg-slate-800" aria-label="Toggle theme">
                {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
              </button>
              <div className="text-right hidden sm:block">
                <div className="text-sm font-medium">{user?.name || 'Admin'}</div>
                <div className="text-xs text-slate-500">{user?.email}</div>
              </div>
              <div className="h-8 w-8 rounded-full bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-xs font-semibold">
                {(user?.name || user?.email || 'A').slice(0,1).toUpperCase()}
              </div>
            </div>
          </header>
          <main className="flex-1 p-4 md:p-8">
            <Outlet />
          </main>
        </div>
      </div>
      <nav className="md:hidden fixed bottom-0 inset-x-0 border-t bg-white/95 dark:bg-slate-900/95 backdrop-blur grid grid-cols-5 text-xs">
        {nav.slice(0,5).map(item => {
          const Icon = item.icon
          return (
            <NavLink key={item.to} to={item.to} end={item.end} className={({isActive}) => `flex flex-col items-center justify-center py-2 gap-1 ${isActive ? 'text-slate-900 dark:text-white' : 'text-slate-500'}`}>
              <Icon size={16} /><span>{item.label.split(' ')[0]}</span>
            </NavLink>
          )
        })}
      </nav>
      <div className="md:hidden h-16" />
    </div>
  )
}
