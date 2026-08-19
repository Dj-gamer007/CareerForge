import { useState } from 'react';
import { Bell, CheckCheck, ExternalLink } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationService } from '@/services/notification.service';
import { queryKeys } from '@/lib/queryClient';
import { formatDate } from '@/lib/utils';
import { Link } from 'react-router-dom';

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const queryClient = useQueryClient();

  // Poll unread count every 30s as per Phase 6 specification
  const { data: unreadCount = 0 } = useQuery({
    queryKey: queryKeys.notifications.unreadCount,
    queryFn: () => notificationService.getUnreadCount(),
    refetchInterval: 30000,
  });

  const { data: notificationsData } = useQuery({
    queryKey: queryKeys.notifications.list(0),
    queryFn: () => notificationService.getNotifications({ page: 0, size: 5 }),
    enabled: isOpen,
  });

  const markAsReadMutation = useMutation({
    mutationFn: (id: number) => notificationService.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unreadCount });
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.list() });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unreadCount });
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.list() });
    },
  });

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
                    className={`p-3 text-left transition-colors ${
                      notif.read ? 'bg-white' : 'bg-indigo-50/40 hover:bg-indigo-50/70'
                    }`}
                    onClick={() => {
                      if (!notif.read) markAsReadMutation.mutate(notif.id);
                    }}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <p className="text-xs font-semibold text-slate-900">{notif.title}</p>
                      <span className="text-[10px] text-slate-400 whitespace-nowrap">
                        {formatDate(notif.createdAt)}
                      </span>
                    </div>
                    <p className="text-xs text-slate-600 mt-1 line-clamp-2">{notif.message}</p>
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
