import { NavLink } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/authStore';
import {
  LayoutDashboard,
  User,
  Briefcase,
  Bookmark,
  FileText,
  Building2,
  Users,
  ShieldCheck,
  Activity,
  BarChart3,
  ListFilter,
} from 'lucide-react';
import { cn } from '@/lib/utils';

export function Sidebar() {
  const user = useAuthStore((state) => state.user);

  const studentLinks = [
    { to: '/student/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/student/profile', label: 'My Profile', icon: User },
    { to: '/student/applications', label: 'Applications', icon: FileText },
    { to: '/student/saved-jobs', label: 'Saved Jobs', icon: Bookmark },
    { to: '/jobs', label: 'Explore Jobs', icon: Briefcase },
  ];

  const recruiterLinks = [
    { to: '/recruiter/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/recruiter/jobs', label: 'Job Postings', icon: Briefcase },
    { to: '/recruiter/company', label: 'Company Profile', icon: Building2 },
    { to: '/recruiter/profile', label: 'Recruiter Profile', icon: User },
  ];

  const adminLinks = [
    { to: '/admin/dashboard', label: 'Overview', icon: LayoutDashboard },
    { to: '/admin/users', label: 'User Directory', icon: Users },
    { to: '/admin/companies', label: 'Company Verification', icon: ShieldCheck },
    { to: '/admin/jobs', label: 'Job Moderation', icon: ListFilter },
    { to: '/admin/audit-logs', label: 'Audit Trail', icon: Activity },
    { to: '/admin/analytics', label: 'Platform Analytics', icon: BarChart3 },
  ];

  let links = studentLinks;
  if (user?.role === 'ROLE_RECRUITER') links = recruiterLinks;
  if (user?.role === 'ROLE_ADMIN') links = adminLinks;

  return (
    <aside className="w-64 bg-white border-r border-slate-200 shrink-0 min-h-[calc(100vh-4rem)] p-4 flex flex-col justify-between">
      <div className="space-y-1">
        <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wider text-slate-400">
          {user?.role === 'ROLE_ADMIN'
            ? 'Administration'
            : user?.role === 'ROLE_RECRUITER'
            ? 'Recruiter Workspace'
            : 'Student Workspace'}
        </div>
        {links.map((link) => {
          const Icon = link.icon;
          return (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-indigo-50 text-indigo-700 font-semibold shadow-xs'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                )
              }
            >
              <Icon className="w-4 h-4" />
              <span>{link.label}</span>
            </NavLink>
          );
        })}
      </div>
    </aside>
  );
}
