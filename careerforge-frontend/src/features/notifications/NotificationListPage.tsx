import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationService } from '@/services/notification.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { formatDateTime } from '@/lib/utils';
import { Bell, CheckCheck, CheckCircle, Info } from 'lucide-react';

export function NotificationListPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.notifications.list(page),
    queryFn: () => notificationService.getNotifications({ page, size: 15 }),
    refetchInterval: 2500,
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

  if (isLoading) return <LoadingSpinner text="Loading notification history..." />;
  if (isError) {
    return (
      <ErrorState
        title="Could not load notifications"
        message={(error as any)?.response?.data?.message || 'Failed to retrieve notification records'}
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Notifications & System Alerts"
        description="Review all real-time platform alerts, application updates, and moderation notifications"
        actions={
          <Button
            size="sm"
            variant="outline"
            onClick={() => markAllMutation.mutate()}
            isLoading={markAllMutation.isPending}
          >
            <CheckCheck className="w-4 h-4 mr-2" />
            Mark All as Read
          </Button>
        }
      />

      {!data || data.content.length === 0 ? (
        <EmptyState
          icon={<Bell className="w-8 h-8 text-slate-400" />}
          title="Inbox zero"
          description="You do not have any notifications at this time."
        />
      ) : (
        <div className="space-y-4">
          <div className="space-y-3">
            {data.content.map((notif) => (
              <Card
                key={notif.id}
                className={`transition-all ${
                  notif.read ? 'bg-white opacity-90' : 'bg-indigo-50/40 border-indigo-200'
                }`}
              >
                <CardContent className="p-4 sm:p-5 flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3">
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 mt-0.5 ${
                        notif.read ? 'bg-slate-100 text-slate-500' : 'bg-indigo-600 text-white'
                      }`}
                    >
                      <Info className="w-4 h-4" />
                    </div>
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <h4 className="text-sm font-bold text-slate-900">{notif.title}</h4>
                        {!notif.read && (
                          <span className="w-2 h-2 rounded-full bg-indigo-600 inline-block shrink-0" />
                        )}
                      </div>
                      <p className="text-xs text-slate-600 leading-relaxed">{notif.message}</p>
                      <span className="text-[10px] text-slate-400 block pt-1">{formatDateTime(notif.createdAt)}</span>
                    </div>
                  </div>

                  {!notif.read && (
                    <Button
                      size="sm"
                      variant="ghost"
                      className="text-xs text-slate-500 hover:text-indigo-600 shrink-0"
                      onClick={() => markAsReadMutation.mutate(notif.id)}
                    >
                      <CheckCircle className="w-3.5 h-3.5 mr-1" />
                      Mark Read
                    </Button>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>

          <PaginationControls
            currentPage={data.number}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            pageSize={data.size}
            onPageChange={(newPage) => setPage(newPage)}
          />
        </div>
      )}
    </div>
  );
}
