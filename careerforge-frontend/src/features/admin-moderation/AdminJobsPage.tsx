import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminService } from '@/services/admin.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge, getStatusBadgeVariant } from '@/components/ui/Badge';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { Modal } from '@/components/ui/Modal';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Briefcase, XCircle, RotateCcw, Archive, Eye } from 'lucide-react';
import { JobStatus } from '@/types/job.types';
import { AdminJobSummaryResponse } from '@/types/admin.types';

export function AdminJobsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [statusFilter, setStatusFilter] = useState<JobStatus | ''>('');

  const [selectedJob, setSelectedJob] = useState<AdminJobSummaryResponse | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isModerateModalOpen, setIsModerateModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<JobStatus>('CLOSED');
  const [reason, setReason] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const { data: jobsData, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.admin.jobs({
      page,
      size: 10,
      search: searchInput || undefined,
      status: statusFilter || undefined,
    }),
    queryFn: () =>
      adminService.getJobs({
        page,
        size: 10,
        search: searchInput || undefined,
        status: statusFilter ? (statusFilter as JobStatus) : undefined,
      }),
    placeholderData: (previousData) => previousData,
    refetchInterval: 2000,
    refetchIntervalInBackground: false,
  });

  const { data: jobDetail, isLoading: isDetailLoading } = useQuery({
    queryKey: queryKeys.admin.jobDetail(selectedJob?.id || 0),
    queryFn: () => adminService.getJobById(selectedJob!.id),
    enabled: isDetailModalOpen && !!selectedJob?.id,
    refetchInterval: isDetailModalOpen && !!selectedJob?.id ? 2500 : false,
    refetchIntervalInBackground: false,
  });

  const moderateMutation = useMutation({
    mutationFn: ({ id, status, reason }: { id: number; status: JobStatus; reason: string }) =>
      adminService.moderateJob(id, { status, reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.jobs() });
      if (selectedJob) {
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.jobDetail(selectedJob.id) });
      }
      setIsModerateModalOpen(false);
      setReason('');
      setErrorMsg(null);
    },
    onError: (err: any) => {
      setErrorMsg(err.response?.data?.message || 'Moderation transition rejected');
    },
  });

  const openModerateModal = (job: AdminJobSummaryResponse, status: JobStatus) => {
    setSelectedJob(job);
    setTargetStatus(status);
    setReason('');
    setErrorMsg(null);
    setIsModerateModalOpen(true);
  };

  const openDetailModal = (job: AdminJobSummaryResponse) => {
    setSelectedJob(job);
    setIsDetailModalOpen(true);
  };

  if (isLoading && !jobsData) return <LoadingSpinner text="Loading platform job directory..." />;
  if (isError) {
    return (
      <ErrorState
        error={error}
        onRetry={() => refetch()}
      />
    );
  }

  const jobs = jobsData?.content || [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Platform Job Moderation"
        description="Inspect, force-close, archive, or return suspicious/non-compliant job postings to draft"
      />

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Input
          placeholder="Search by job title, company name..."
          value={searchInput}
          onChange={(e) => {
            setSearchInput(e.target.value);
            setPage(0);
          }}
        />

        <Select
          options={[
            { label: 'All Lifecycle Statuses', value: '' },
            { label: 'Published Postings', value: 'PUBLISHED' },
            { label: 'Draft Postings', value: 'DRAFT' },
            { label: 'Closed Postings', value: 'CLOSED' },
            { label: 'Archived Postings', value: 'ARCHIVED' },
          ]}
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as JobStatus | '');
            setPage(0);
          }}
        />
      </div>

      <Card>
        <CardContent className="p-0 overflow-x-auto">
          {jobs.length === 0 ? (
            <EmptyState
              icon={<Briefcase className="w-8 h-8 text-slate-400" />}
              title="No jobs found"
              description="No platform job records match the current filter selection."
            />
          ) : (
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3">Job Posting</th>
                  <th className="px-6 py-3">Company</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Applicants</th>
                  <th className="px-6 py-3 text-right">Moderation Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {jobs.map((j) => (
                  <tr key={j.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="px-6 py-4">
                      <p className="font-bold text-slate-900">{j.title}</p>
                      <p className="text-xs text-slate-400">{j.location || 'Remote'} &bull; {j.workMode}</p>
                    </td>
                    <td className="px-6 py-4">
                      <p className="font-semibold text-slate-900">{j.companyName}</p>
                      <p className="text-xs text-slate-400">{j.companyVerificationStatus}</p>
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={getStatusBadgeVariant(j.status)}>{j.status}</Badge>
                    </td>
                    <td className="px-6 py-4 text-xs font-semibold text-slate-900">
                      {j.applicationsCount} Candidates
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <Button size="sm" variant="ghost" onClick={() => openDetailModal(j)}>
                        <Eye className="w-4 h-4 mr-1" />
                        Inspect
                      </Button>

                      {j.status === 'PUBLISHED' && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="text-amber-700 border-amber-200 hover:bg-amber-50"
                          onClick={() => openModerateModal(j, 'CLOSED')}
                        >
                          <XCircle className="w-3.5 h-3.5 mr-1" />
                          Force Close
                        </Button>
                      )}

                      {j.status !== 'ARCHIVED' && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="text-rose-600 border-rose-200 hover:bg-rose-50"
                          onClick={() => openModerateModal(j, 'ARCHIVED')}
                        >
                          <Archive className="w-3.5 h-3.5 mr-1" />
                          Force Archive
                        </Button>
                      )}

                      {(j.status === 'CLOSED' || j.status === 'ARCHIVED') && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="text-indigo-600 border-indigo-200 hover:bg-indigo-50"
                          onClick={() => openModerateModal(j, 'DRAFT')}
                        >
                          <RotateCcw className="w-3.5 h-3.5 mr-1" />
                          Return to Draft
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
        {jobsData && (
          <PaginationControls
            currentPage={jobsData.page ?? jobsData.number ?? 0}
            totalPages={jobsData.totalPages}
            totalElements={jobsData.totalElements}
            pageSize={jobsData.size}
            onPageChange={(newPage) => setPage(Number.isFinite(newPage) ? newPage : 0)}
          />
        )}
      </Card>

      {/* Job Inspection Modal */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        title="Platform Job Inspection"
        description="Comprehensive details, technical skill requirements, and poster identity"
        maxWidth="lg"
      >
        {isDetailLoading || !jobDetail ? (
          <LoadingSpinner text="Fetching job details..." />
        ) : (
          <div className="space-y-6">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100 text-xs">
              <div>
                <span className="text-slate-400">Company</span>
                <p className="font-bold text-slate-900 mt-0.5">{jobDetail.companyName}</p>
              </div>
              <div>
                <span className="text-slate-400">Posted By</span>
                <p className="font-bold text-slate-900 mt-0.5">{jobDetail.recruiterEmail || 'Recruiter'}</p>
              </div>
              <div>
                <span className="text-slate-400">Status</span>
                <p className="mt-0.5">
                  <Badge variant={getStatusBadgeVariant(jobDetail.status)}>{jobDetail.status}</Badge>
                </p>
              </div>
              <div>
                <span className="text-slate-400">Applicants</span>
                <p className="font-bold text-slate-900 mt-0.5">{jobDetail.applicationsCount}</p>
              </div>
            </div>

            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-1">Description</h4>
              <p className="text-xs text-slate-700 whitespace-pre-line leading-relaxed">{jobDetail.description}</p>
            </div>

            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Skill Requirements</h4>
              <div className="flex flex-wrap gap-2">
                {jobDetail.skills?.map((s) => {
                  const isRequired = s.required ?? s.isRequired;
                  return (
                    <span
                      key={s.id}
                      className="px-2.5 py-1 rounded-md text-xs bg-slate-100 text-slate-800 font-medium border border-slate-200"
                    >
                      {s.skillName} ({s.minimumProficiency}) {isRequired ? '*' : '(Opt)'}
                    </span>
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </Modal>

      {/* Moderation Transition Modal */}
      <Modal
        isOpen={isModerateModalOpen}
        onClose={() => setIsModerateModalOpen(false)}
        title={`Moderate Job to: ${targetStatus}`}
        description={`Applying administrative moderation to: ${selectedJob?.title}`}
      >
        <div className="space-y-4">
          {errorMsg && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-xs">
              {errorMsg}
            </div>
          )}

          <Textarea
            label="Mandatory Administrative Reason *"
            placeholder="Document reason for moderation action and audit logging compliance (min. 5 characters)..."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
          />

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" size="sm" onClick={() => setIsModerateModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              variant="destructive"
              onClick={() =>
                moderateMutation.mutate({
                  id: selectedJob!.id,
                  status: targetStatus,
                  reason,
                })
              }
              isLoading={moderateMutation.isPending}
              disabled={reason.trim().length < 5}
            >
              Confirm Moderation
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
