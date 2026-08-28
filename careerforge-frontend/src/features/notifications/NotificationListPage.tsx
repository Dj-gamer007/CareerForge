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

import { useNavigate } from 'react-router-dom';
import { NotificationResponse } from '@/types/notification.types';
import { useAuthStore } from '@/features/auth/authStore';

export function NotificationListPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.notifications.list(page),
    queryFn: () => notificationService.getNotifications({ page, size: 15 }),
    refetchInterval: 2000,
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

  const handleCardClick = (notif: NotificationResponse) => {
    if (!notif.read && !notif.isRead) {
      markAsReadMutation.mutate(notif.id);
    }
    const link = getNotificationLink(notif);
    if (link) {
      navigate(link);
    }
  };

  if (isLoading && !data) return <LoadingSpinner text="Loading notification history..." />;
  if (isError) {
    return (
      <ErrorState
        error={error}
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
                className={`transition-all cursor-pointer ${
                  notif.read || notif.isRead ? 'bg-white opacity-90 hover:bg-slate-50' : 'bg-indigo-50/40 border-indigo-200 hover:bg-indigo-50/70'
                }`}
                onClick={() => handleCardClick(notif)}
              >
                <CardContent className="p-4 sm:p-5 flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3">
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 mt-0.5 ${
                        notif.read || notif.isRead ? 'bg-slate-100 text-slate-500' : 'bg-indigo-600 text-white'
                      }`}
                    >
                      <Info className="w-4 h-4" />
                    </div>
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <h4 className="text-sm font-bold text-slate-900">{notif.title}</h4>
                        {!notif.read && !notif.isRead && (
                          <span className="w-2 h-2 rounded-full bg-indigo-600 inline-block shrink-0" />
                        )}
                      </div>
                      <p className="text-xs text-slate-600 leading-relaxed">{notif.message}</p>
                      {notif.actorName && (
                        <span className="text-[10px] text-indigo-600 font-medium block">
                          Updated by {notif.actorName}
                        </span>
                      )}
                      <span className="text-[10px] text-slate-400 block pt-1">{formatDateTime(notif.createdAt)}</span>
                    </div>
                  </div>

                  {!notif.read && (
                    <Button
                      size="sm"
                      variant="ghost"
                      className="text-xs text-slate-500 hover:text-indigo-600 shrink-0"
                      onClick={(e) => {
                        e.stopPropagation();
                        markAsReadMutation.mutate(notif.id);
                      }}
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
            currentPage={data.page ?? data.number ?? 0}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            pageSize={data.size}
            onPageChange={(newPage) => setPage(Number.isFinite(newPage) ? newPage : 0)}
          />
        </div>
      )}
    </div>
  );
}
