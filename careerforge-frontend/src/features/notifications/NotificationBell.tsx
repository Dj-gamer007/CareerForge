import { useState } from 'react';
import { Bell, CheckCheck, ExternalLink } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationService } from '@/services/notification.service';
import { queryKeys } from '@/lib/queryClient';
import { formatDate } from '@/lib/utils';
import { Link, useNavigate } from 'react-router-dom';
import { NotificationResponse } from '@/types/notification.types';
import { useAuthStore } from '@/features/auth/authStore';

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const isAccountDisabled = useAuthStore((state) => state.isAccountDisabled);

  // Poll unread count regularly for real-time notification badge updates only when active
  const { data: unreadCount = 0 } = useQuery({
    queryKey: queryKeys.notifications.unreadCount,
    queryFn: () => notificationService.getUnreadCount(),
    enabled: !isAccountDisabled,
    refetchInterval: 2000,
    refetchIntervalInBackground: false,
  });

  const { data: notificationsData } = useQuery({
    queryKey: queryKeys.notifications.list(0),
    queryFn: () => notificationService.getNotifications({ page: 0, size: 5 }),
    enabled: isOpen && !isAccountDisabled,
    refetchInterval: isOpen && !isAccountDisabled ? 2000 : false,
    refetchIntervalInBackground: false,
    placeholderData: (prev) => prev,
  });

  const markAsReadMutation = useMutation({
    mutationFn: (id: number) => notificationService.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const user = useAuthStore((state) => state.user);

  const getNotificationLink = (notif: NotificationResponse) => {
    const role = user?.role;
    const type = notif.type;
    const entityType = notif.relatedEntityType;
    const entityId = notif.relatedEntityId;
    const title = notif.title?.toLowerCase() || '';

    if (role === 'ROLE_STUDENT') {
      if (
        type === 'APPLICATION_SHORTLISTED' ||
        type === 'APPLICATION_ACCEPTED' ||
        type === 'APPLICATION_REJECTED' ||
        type === 'APPLICATION_UPDATED' ||
        type === 'APPLICATION_UPDATE' ||
        type === 'APPLICATION_SUBMITTED' ||
        type === 'INTERVIEW_INVITE' ||
        type === 'INTERVIEW_SCHEDULED' ||
        type === 'INTERVIEW_RESCHEDULED' ||
        entityType === 'APPLICATION' ||
        title.includes('application') ||
        title.includes('interview')
      ) {
        return '/student/applications';
      }
      if (type === 'JOB_RECOMMENDATION' || type === 'JOB_POSTING_PUBLISHED' || entityType === 'JOB' || title.includes('job')) {
        return '/jobs';
      }
      if (type === 'ACCOUNT_DISABLED') {
        return '/student/profile';
      }
    }

    if (role === 'ROLE_RECRUITER') {
      if (
        type === 'COMPANY_VERIFIED' ||
        type === 'COMPANY_VERIFICATION_REJECTED' ||
        type === 'COMPANY_VERIFICATION_PENDING' ||
        entityType === 'COMPANY' ||
        title.includes('company')
      ) {
        return '/recruiter/company';
      }
      if (
        type === 'JOB_POSTING_CLOSED' ||
        type === 'JOB_POSTING_DRAFTED' ||
        type === 'JOB_POSTING_ARCHIVED' ||
        type === 'JOB_POSTING_PUBLISHED' ||
        entityType === 'JOB' ||
        title.includes('job')
      ) {
        return entityId && type === 'JOB_POSTING_DRAFTED' ? `/recruiter/jobs/${entityId}/edit` : '/recruiter/jobs';
      }
      if (entityType === 'APPLICATION' || type === 'APPLICATION_SUBMITTED' || title.includes('application')) {
        return '/recruiter/jobs';
      }
      if (type === 'ACCOUNT_DISABLED') {
        return '/recruiter/profile';
      }
    }

    if (role === 'ROLE_ADMIN') {
      if (
        type === 'COMPANY_VERIFIED' ||
        type === 'COMPANY_VERIFICATION_REJECTED' ||
        type === 'COMPANY_VERIFICATION_PENDING' ||
        entityType === 'COMPANY' ||
        title.includes('company')
      ) {
        return '/admin/companies';
      }
      if (
        type === 'JOB_POSTING_CLOSED' ||
        type === 'JOB_POSTING_DRAFTED' ||
        type === 'JOB_POSTING_ARCHIVED' ||
        type === 'JOB_POSTING_PUBLISHED' ||
        entityType === 'JOB' ||
        title.includes('job') ||
        title.includes('moderation')
      ) {
        return '/admin/jobs';
      }
      if (type === 'ACCOUNT_DISABLED' || entityType === 'USER' || title.includes('user') || title.includes('account')) {
        return '/admin/users';
      }
      if (entityType === 'APPLICATION' || title.includes('application')) {
        return '/admin/dashboard';
      }
    }

    if (title.includes('company')) return '/admin/companies';
    if (title.includes('job')) return '/jobs';
    if (title.includes('application')) return '/student/applications';
    return null;
  };

  const handleNotificationClick = (notif: NotificationResponse) => {
    if (!notif.read && !notif.isRead) {
      markAsReadMutation.mutate(notif.id);
    }
    setIsOpen(false);
    const link = getNotificationLink(notif);
    if (link) {
      navigate(link);
    }
  };

  return (
    <div className="relative">
      <button
        type="button"
        className="relative p-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors focus:outline-none"
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Notifications"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-rose-600 text-[10px] font-bold text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)} />
          <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-xl bg-white shadow-xl border border-slate-200 z-50 overflow-hidden">
            <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 bg-slate-50">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-sm text-slate-900">Notifications</span>
                {unreadCount > 0 && (
                  <span className="px-1.5 py-0.5 text-xs font-medium bg-indigo-100 text-indigo-700 rounded-full">
                    {unreadCount} new
                  </span>
                )}
              </div>
              {unreadCount > 0 && (
                <button
                  type="button"
                  className="text-xs text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-1"
                  onClick={() => markAllMutation.mutate()}
                >
                  <CheckCheck className="w-3.5 h-3.5" />
                  Mark all read
                </button>
              )}
            </div>

            <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
              {notificationsData?.content && notificationsData.content.length > 0 ? (
                notificationsData.content.map((notif) => (
                  <div
                    key={notif.id}
                    className={`p-3 text-left transition-colors cursor-pointer ${
                      notif.read || notif.isRead ? 'bg-white hover:bg-slate-50' : 'bg-indigo-50/40 hover:bg-indigo-50/70'
                    }`}
                    onClick={() => handleNotificationClick(notif)}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <p className="text-xs font-semibold text-slate-900">{notif.title}</p>
                      <span className="text-[10px] text-slate-400 whitespace-nowrap">
                        {formatDate(notif.createdAt)}
                      </span>
                    </div>
                    <p className="text-xs text-slate-600 mt-1 line-clamp-2">{notif.message}</p>
                    {notif.actorName && (
                      <span className="text-[10px] text-indigo-600 font-medium block mt-1">
                        Updated by {notif.actorName}
                      </span>
                    )}
                  </div>
                ))
              ) : (
                <div className="p-6 text-center text-xs text-slate-500">No notifications yet</div>
              )}
            </div>

            <div className="p-2 border-t border-slate-100 text-center bg-slate-50">
              <Link
                to="/notifications"
                className="text-xs text-indigo-600 hover:text-indigo-800 font-medium inline-flex items-center gap-1"
                onClick={() => setIsOpen(false)}
              >
                View all notifications
                <ExternalLink className="w-3 h-3" />
              </Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
