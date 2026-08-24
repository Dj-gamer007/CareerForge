import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobService } from '@/services/job.service';
import { queryKeys } from '@/lib/queryClient';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge, getStatusBadgeVariant } from '@/components/ui/Badge';
import { Select } from '@/components/ui/Select';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { formatDate } from '@/lib/utils';
import {
  Briefcase,
  Plus,
  Users,
  Edit,
  Play,
  Pause,
  XCircle,
  RotateCcw,
  Archive,
  Trash2,
} from 'lucide-react';
import { JobStatus } from '@/types/job.types';

import { recruiterService } from '@/services/recruiter.service';
import { Building2 } from 'lucide-react';

export function RecruiterJobsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<JobStatus | ''>('');
  const [activeJobId, setActiveJobId] = useState<number | null>(null);
  const [actionFeedback, setActionFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const { data: company, isLoading: isCompLoading } = useQuery({
    queryKey: queryKeys.recruiter.company,
    queryFn: () => recruiterService.getMyCompany(),
    retry: false,
  });

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.recruiter.jobs({ page, size: 10, status: statusFilter || undefined }),
    queryFn: () =>
      jobService.getRecruiterJobs({
        page,
        size: 10,
        status: statusFilter ? (statusFilter as JobStatus) : undefined,
      }),
    enabled: !!company?.id,
    retry: false,
  });

  const invalidateAllJobQueries = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.recruiter.jobs() });
    queryClient.invalidateQueries({ queryKey: queryKeys.publicJobs.list() });
  };

  // Lifecycle Mutations
  const publishMutation = useMutation({
    mutationFn: (id: number) => {
      setActiveJobId(id);
      setActionFeedback(null);
      return jobService.publishJob(id);
    },
    onSuccess: () => {
      invalidateAllJobQueries();
      setActionFeedback({ type: 'success', message: 'Job published successfully. It is now open for candidate applications.' });
      setActiveJobId(null);
    },
    onError: (err: any) => {
      setActionFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to publish job posting.' });
      setActiveJobId(null);
    },
  });

  const unpublishMutation = useMutation({
    mutationFn: (id: number) => {
      setActiveJobId(id);
      setActionFeedback(null);
      return jobService.unpublishJob(id);
    },
    onSuccess: () => {
      invalidateAllJobQueries();
      setActionFeedback({ type: 'success', message: 'Job paused and unpublished back to DRAFT.' });
      setActiveJobId(null);
    },
    onError: (err: any) => {
      setActionFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to pause job.' });
      setActiveJobId(null);
    },
  });

  const closeMutation = useMutation({
    mutationFn: (id: number) => {
      setActiveJobId(id);
      setActionFeedback(null);
      return jobService.closeJob(id);
    },
    onSuccess: () => {
      invalidateAllJobQueries();
      setActionFeedback({ type: 'success', message: 'Job closed successfully.' });
      setActiveJobId(null);
    },
    onError: (err: any) => {
      setActionFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to close job.' });
      setActiveJobId(null);
    },
  });

  const reopenMutation = useMutation({
    mutationFn: (id: number) => {
      setActiveJobId(id);
      setActionFeedback(null);
      return jobService.reopenJob(id);
    },
    onSuccess: () => {
      invalidateAllJobQueries();
      setActionFeedback({ type: 'success', message: 'Job reopened and published successfully.' });
      setActiveJobId(null);
    },
    onError: (err: any) => {
      setActionFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to reopen job.' });
      setActiveJobId(null);
    },
  });

  const archiveMutation = useMutation({
    mutationFn: (id: number) => {
      setActiveJobId(id);
      setActionFeedback(null);
      return jobService.archiveJob(id);
    },
    onSuccess: () => {
      invalidateAllJobQueries();
      setActionFeedback({ type: 'success', message: 'Job archived successfully.' });
      setActiveJobId(null);
    },
    onError: (err: any) => {
      setActionFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to archive job.' });
      setActiveJobId(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => {
      setActiveJobId(id);
      setActionFeedback(null);
      return jobService.deleteJob(id);
    },
    onSuccess: () => {
      invalidateAllJobQueries();
      setActionFeedback({ type: 'success', message: 'Job deleted successfully.' });
      setActiveJobId(null);
    },
    onError: (err: any) => {
      setActionFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to delete job.' });
      setActiveJobId(null);
    },
  });

  if (isCompLoading || (isLoading && !!company?.id)) return <LoadingSpinner text="Loading company job postings..." />;

  if (!company) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="Job Postings Management"
          description="Create, publish, pause, and track hiring stages across all open roles"
        />

        <Card>
          <CardContent className="p-8 text-center space-y-4">
            <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 mx-auto flex items-center justify-center">
              <Building2 className="w-6 h-6" />
            </div>
            <div className="space-y-1">
              <h3 className="text-base font-bold text-slate-900">Organization Registration Required</h3>
              <p className="text-xs text-slate-500 max-w-md mx-auto">
                You must register your company profile before creating, publishing, and managing job postings.
              </p>
            </div>
            <div>
              <Link to="/recruiter/company">
                <Button size="sm">
                  <Building2 className="w-4 h-4 mr-2" />
                  Register Company Profile
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Could not load jobs"
        message={(error as any)?.response?.data?.message || 'Failed to fetch company jobs'}
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Job Postings Management"
        description="Create, publish, pause, and track hiring stages across all open roles"
        actions={
          <div className="flex items-center gap-3">
            <div className="w-40">
              <Select
                options={[
                  { label: 'All Statuses', value: '' },
                  { label: 'Draft', value: 'DRAFT' },
                  { label: 'Published', value: 'PUBLISHED' },
                  { label: 'Closed', value: 'CLOSED' },
                  { label: 'Archived', value: 'ARCHIVED' },
                ]}
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value as JobStatus | '');
                  setPage(0);
                }}
              />
            </div>
            <Link to="/recruiter/jobs/new">
              <Button size="sm">
                <Plus className="w-4 h-4 mr-2" />
                Post Job
              </Button>
            </Link>
          </div>
        }
      />

      {/* Action feedback banner */}
      {actionFeedback && (
        <div
          role="alert"
          className={`p-4 rounded-xl border text-sm flex items-center justify-between transition-all ${
            actionFeedback.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : 'bg-rose-50 border-rose-200 text-rose-800'
          }`}
        >
          <span>{actionFeedback.message}</span>
          <button
            type="button"
            onClick={() => setActionFeedback(null)}
            className="text-xs font-semibold underline ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {!data || data.content.length === 0 ? (
        <EmptyState
          icon={<Briefcase className="w-8 h-8 text-slate-400" />}
          title="No job postings found"
          description="Create your first job posting to begin receiving applicants."
        />
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-4">
            {data.content.map((job) => {
              const isProcessing = activeJobId === job.id;

              return (
                <Card key={job.id}>
                  <CardContent className="p-6 flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                    <div className="space-y-2">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-bold text-slate-900 text-base">{job.title}</span>
                        <Badge variant={getStatusBadgeVariant(job.status)}>{job.status}</Badge>
                        <Badge variant="outline">{job.workMode}</Badge>
                      </div>

                      <div className="flex items-center gap-4 text-xs text-slate-500 flex-wrap">
                        <span>{job.location || 'Remote'}</span>
                        <span>Created {formatDate(job.createdAt)}</span>
                        {job.deadline && <span>Deadline: {formatDate(job.deadline)}</span>}
                        <span>{job.skills?.length || 0} Target Skills</span>
                      </div>
                    </div>

                    {/* Actions Bar */}
                    <div className="flex items-center gap-2 flex-wrap">
                      <Link to={`/recruiter/jobs/${job.id}/applications`}>
                        <Button size="sm" variant="primary">
                          <Users className="w-3.5 h-3.5 mr-1.5" />
                          ATS Applicants
                        </Button>
                      </Link>

                      <Link to={`/recruiter/jobs/${job.id}/edit`}>
                        <Button size="sm" variant="outline">
                          <Edit className="w-3.5 h-3.5 mr-1" />
                          Edit
                        </Button>
                      </Link>

                      {/* State machine buttons */}
                      {job.status === 'DRAFT' && (
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => publishMutation.mutate(job.id)}
                          isLoading={isProcessing && publishMutation.isPending}
                          disabled={isProcessing}
                        >
                          <Play className="w-3.5 h-3.5 mr-1 text-emerald-400" />
                          Publish
                        </Button>
                      )}

                      {job.status === 'PUBLISHED' && (
                        <>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => unpublishMutation.mutate(job.id)}
                            isLoading={isProcessing && unpublishMutation.isPending}
                            disabled={isProcessing}
                          >
                            <Pause className="w-3.5 h-3.5 mr-1" />
                            Pause
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-amber-700 border-amber-200 hover:bg-amber-50"
                            onClick={() => closeMutation.mutate(job.id)}
                            isLoading={isProcessing && closeMutation.isPending}
                            disabled={isProcessing}
                          >
                            <XCircle className="w-3.5 h-3.5 mr-1" />
                            Close
                          </Button>
                        </>
                      )}

                      {job.status === 'CLOSED' && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => reopenMutation.mutate(job.id)}
                          isLoading={isProcessing && reopenMutation.isPending}
                          disabled={isProcessing}
                        >
                          <RotateCcw className="w-3.5 h-3.5 mr-1" />
                          Reopen
                        </Button>
                      )}

                      {job.status !== 'ARCHIVED' && (
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-slate-500 hover:text-slate-900"
                          onClick={() => archiveMutation.mutate(job.id)}
                          isLoading={isProcessing && archiveMutation.isPending}
                          disabled={isProcessing}
                        >
                          <Archive className="w-3.5 h-3.5 mr-1" />
                          Archive
                        </Button>
                      )}

                      {(job.status === 'DRAFT' || job.status === 'ARCHIVED') && (
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-rose-600 hover:bg-rose-50"
                          onClick={() => deleteMutation.mutate(job.id)}
                          isLoading={isProcessing && deleteMutation.isPending}
                          disabled={isProcessing}
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </Button>
                      )}
                    </div>
                  </CardContent>
                </Card>
              );
            })}
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
