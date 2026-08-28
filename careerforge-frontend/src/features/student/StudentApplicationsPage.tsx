import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { applicationService } from '@/services/application.service';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge, getStatusBadgeVariant } from '@/components/ui/Badge';
import { Select } from '@/components/ui/Select';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { formatDate, formatDateTime } from '@/lib/utils';
import { FileText, Building2, Calendar, Ban, History } from 'lucide-react';
import { ApplicationStatus } from '@/types/application.types';
import { ApplicationHistoryModal } from './ApplicationHistoryModal';

export function StudentApplicationsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<ApplicationStatus | ''>('');
  const [selectedHistoryApp, setSelectedHistoryApp] = useState<{ id: number; jobTitle: string; companyName: string } | null>(null);

  // Applications list query with real-time auto-polling
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['student', 'applications', { page, size: 10, status: statusFilter || undefined }],
    queryFn: () =>
      applicationService.getStudentApplications({
        page,
        size: 10,
        status: statusFilter ? (statusFilter as ApplicationStatus) : undefined,
      }),
    refetchInterval: 2000,
    refetchIntervalInBackground: false,
    placeholderData: (prev) => prev,
  });

  const withdrawMutation = useMutation({
    mutationFn: (id: number) => applicationService.withdrawApplication(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student', 'applications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  if (isLoading && !data) return <LoadingSpinner text="Loading your application tracker..." />;
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
        title="Application Pipeline & Tracker"
        description="Monitor status transitions, interview dates, and match snapshots for your submitted applications"
        actions={
          <div className="w-full sm:w-56">
            <Select
              options={[
                { label: 'All Statuses', value: '' },
                { label: 'Applied', value: 'APPLIED' },
                { label: 'Under Review', value: 'UNDER_REVIEW' },
                { label: 'Shortlisted', value: 'SHORTLISTED' },
                { label: 'Interview Scheduled', value: 'INTERVIEW_SCHEDULED' },
                { label: 'Accepted', value: 'ACCEPTED' },
                { label: 'Rejected', value: 'REJECTED' },
                { label: 'Withdrawn', value: 'WITHDRAWN' },
              ]}
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as ApplicationStatus | '');
                setPage(0);
              }}
            />
          </div>
        }
      />

      {!data || data.content.length === 0 ? (
        <EmptyState
          icon={<FileText className="w-8 h-8 text-slate-400" />}
          title="No applications in pipeline"
          description="You currently have no applications matching this filter."
        />
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-4">
            {data.content.map((app) => (
              <Card key={app.id}>
                <CardContent className="p-6">
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                    <div className="space-y-2">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h3 className="text-base font-bold text-slate-900">{app.jobTitle}</h3>
                        <Badge variant={getStatusBadgeVariant(app.status)}>{app.status.replace('_', ' ')}</Badge>
                        {app.jobStatus === 'CLOSED' && (
                          <Badge variant="outline" className="bg-slate-100 text-slate-700 border-slate-300 font-medium">
                            Job Closed
                          </Badge>
                        )}
                        {app.jobStatus === 'ARCHIVED' && (
                          <Badge variant="outline" className="bg-slate-100 text-slate-700 border-slate-300 font-medium">
                            Job Archived
                          </Badge>
                        )}
                      </div>

                      <div className="flex items-center gap-4 text-xs text-slate-500 flex-wrap">
                        <span className="flex items-center gap-1 font-semibold text-slate-700">
                          <Building2 className="w-3.5 h-3.5" />
                          {app.companyName}
                        </span>
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3.5 h-3.5" />
                          Applied on {formatDate(app.appliedAt)}
                        </span>
                        {app.interviewScheduledAt && (
                          <span className="px-2 py-0.5 rounded bg-amber-50 text-amber-800 font-bold border border-amber-200">
                            Interview Scheduled: {formatDateTime(app.interviewScheduledAt)}
                          </span>
                        )}
                      </div>

                      <div className="text-xs text-slate-500 pt-1">
                        Compatibility Score at Submission:{' '}
                        <span className="font-extrabold text-slate-900">
                          {app.matchScoreAtApplication?.toFixed(1)}%
                        </span>
                      </div>
                    </div>

                    {/* Action buttons */}
                    <div className="flex items-center gap-2 shrink-0">
                      <Button
                        variant="outline"
                        size="sm"
                        className="text-primary-600 border-primary-200 hover:bg-primary-50"
                        onClick={() => setSelectedHistoryApp({ id: app.id, jobTitle: app.jobTitle, companyName: app.companyName })}
                      >
                        <History className="w-3.5 h-3.5 mr-1" />
                        View History
                      </Button>

                      {/* Self-Withdraw Action */}
                      {(app.status === 'APPLIED' ||
                        app.status === 'UNDER_REVIEW' ||
                        app.status === 'SHORTLISTED' ||
                        app.status === 'INTERVIEW_SCHEDULED') && (
                        <Button
                          variant="outline"
                          size="sm"
                          className="text-rose-600 border-rose-200 hover:bg-rose-50"
                          onClick={() => withdrawMutation.mutate(app.id)}
                          isLoading={withdrawMutation.isPending}
                        >
                          <Ban className="w-3.5 h-3.5 mr-1" />
                          Withdraw
                        </Button>
                      )}
                    </div>
                  </div>
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

      {/* Timeline Modal */}
      <ApplicationHistoryModal
        isOpen={!!selectedHistoryApp}
        onClose={() => setSelectedHistoryApp(null)}
        applicationId={selectedHistoryApp?.id ?? null}
        jobTitle={selectedHistoryApp?.jobTitle}
        companyName={selectedHistoryApp?.companyName}
      />
    </div>
  );
}
