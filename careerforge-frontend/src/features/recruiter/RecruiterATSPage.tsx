import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { applicationService } from '@/services/application.service';
import { jobService } from '@/services/job.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge, getStatusBadgeVariant } from '@/components/ui/Badge';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import { Modal } from '@/components/ui/Modal';
import { ScoreGauge } from '@/components/ui/ScoreGauge';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ErrorState } from '@/components/feedback/ErrorState';
import { formatDate } from '@/lib/utils';
import {
  LayoutGrid,
  List,
  Download,
  FileText,
  MessageSquare,
  ArrowLeft,
} from 'lucide-react';
import { ApplicationStatus, RecruiterApplicationResponse } from '@/types/application.types';

const ATS_STAGES: Array<{ status: ApplicationStatus; label: string }> = [
  { status: 'APPLIED', label: 'Applied' },
  { status: 'UNDER_REVIEW', label: 'Under Review' },
  { status: 'SHORTLISTED', label: 'Shortlisted' },
  { status: 'INTERVIEW_SCHEDULED', label: 'Interview Scheduled' },
  { status: 'ACCEPTED', label: 'Offer Extended' },
  { status: 'REJECTED', label: 'Declined' },
];

export function RecruiterATSPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const parsedJobId = Number(jobId);
  const queryClient = useQueryClient();

  const [viewMode, setViewMode] = useState<'kanban' | 'table'>('kanban');
  const [selectedAppId, setSelectedAppId] = useState<number | null>(null);

  // Status transition modal
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<ApplicationStatus>('UNDER_REVIEW');
  const [interviewDate, setInterviewDate] = useState('');

  // Candidate dossier drawer
  const [isDossierOpen, setIsDossierOpen] = useState(false);
  const [recruiterNotes, setRecruiterNotes] = useState('');

  const { data: job } = useQuery({
    queryKey: queryKeys.recruiter.jobDetail(parsedJobId),
    queryFn: () => jobService.getRecruiterJobById(parsedJobId),
    enabled: !!parsedJobId,
  });

  const { data: appsData, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.recruiter.applications(parsedJobId),
    queryFn: () => applicationService.getRecruiterApplications(parsedJobId, { size: 100 }),
    enabled: !!parsedJobId,
  });

  const { data: appDetail, isLoading: isDetailLoading } = useQuery({
    queryKey: queryKeys.recruiter.applicationDetail(selectedAppId || 0),
    queryFn: () => applicationService.getRecruiterApplicationDetail(selectedAppId!),
    enabled: !!selectedAppId && isDossierOpen,
  });

  // Mutations
  const updateStatusMutation = useMutation({
    mutationFn: ({ appId, status, interviewScheduledAt }: any) =>
      applicationService.updateApplicationStatus(appId, {
        status,
        interviewScheduledAt: interviewScheduledAt ? `${interviewScheduledAt}:00` : undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.recruiter.applications(parsedJobId) });
      if (selectedAppId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.recruiter.applicationDetail(selectedAppId) });
      }
      setIsStatusModalOpen(false);
    },
  });

  const saveNotesMutation = useMutation({
    mutationFn: ({ appId, notes }: any) => applicationService.updateApplicationNotes(appId, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.recruiter.applicationDetail(selectedAppId!) });
    },
  });

  const openStatusModal = (appId: number, nextStatus: ApplicationStatus) => {
    setSelectedAppId(appId);
    setTargetStatus(nextStatus);
    setInterviewDate('');
    setIsStatusModalOpen(true);
  };

  const openDossier = (app: RecruiterApplicationResponse) => {
    setSelectedAppId(app.id);
    setIsDossierOpen(true);
  };

  const handleDownloadResume = async (appId: number, candidateName: string) => {
    try {
      const blob = await applicationService.downloadCandidateResume(appId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${candidateName.replace(/\s+/g, '_')}_Resume.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch {
      alert('Resume download failed or no resume file attached.');
    }
  };

  if (isLoading) return <LoadingSpinner text="Loading applicant tracking pipeline..." />;
  if (isError) {
    return (
      <ErrorState
        title="Could not load ATS applicants"
        message={(error as any)?.response?.data?.message || 'Failed to fetch candidate pipeline'}
        onRetry={() => refetch()}
      />
    );
  }

  const applications = appsData?.content || [];

  return (
    <div className="space-y-6">
      <Link to="/recruiter/jobs" className="inline-flex items-center text-xs font-semibold text-slate-500 hover:text-indigo-600">
        <ArrowLeft className="w-4 h-4 mr-1" />
        Back to Jobs
      </Link>

      <PageHeader
        title={`ATS Pipeline: ${job?.title || 'Job Applications'}`}
        description="Track candidates through deterministic score rankings, stage transitions, and interview scheduling"
        actions={
          <div className="flex items-center gap-2 bg-slate-100 p-1 rounded-lg border border-slate-200">
            <button
              type="button"
              className={`p-1.5 rounded-md text-xs font-semibold flex items-center gap-1.5 transition-colors ${
                viewMode === 'kanban' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
              onClick={() => setViewMode('kanban')}
            >
              <LayoutGrid className="w-4 h-4" />
              Kanban
            </button>
            <button
              type="button"
              className={`p-1.5 rounded-md text-xs font-semibold flex items-center gap-1.5 transition-colors ${
                viewMode === 'table' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
              onClick={() => setViewMode('table')}
            >
              <List className="w-4 h-4" />
              Table
            </button>
          </div>
        }
      />

      {/* Kanban Board View */}
      {viewMode === 'kanban' ? (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4 overflow-x-auto pb-4">
          {ATS_STAGES.map((stage) => {
            const stageApps = applications.filter((a) => a.status === stage.status);

            return (
              <div key={stage.status} className="bg-slate-100/70 rounded-xl p-3 flex flex-col min-w-[240px]">
                <div className="flex items-center justify-between mb-3 px-1">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-700">{stage.label}</span>
                  <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-white text-slate-600 border border-slate-200">
                    {stageApps.length}
                  </span>
                </div>

                <div className="space-y-3 flex-1 overflow-y-auto">
                  {stageApps.length === 0 ? (
                    <div className="p-4 text-center text-xs text-slate-400 border border-dashed border-slate-200 rounded-lg">
                      No candidates
                    </div>
                  ) : (
                    stageApps.map((app) => (
                      <Card
                        key={app.id}
                        className="cursor-pointer hover:border-indigo-300 hover:shadow-md transition-all"
                        onClick={() => openDossier(app)}
                      >
                        <CardContent className="p-3.5 space-y-2">
                          <div className="flex items-start justify-between gap-1">
                            <h4 className="text-xs font-bold text-slate-900">{app.candidateName}</h4>
                            <ScoreGauge score={app.matchScoreAtApplication} size="sm" showLabel={false} />
                          </div>

                          <p className="text-[11px] text-slate-500 truncate">{app.candidateEmail}</p>

                          <div className="flex items-center justify-between text-[10px] text-slate-400 pt-1 border-t border-slate-100">
                            <span>Applied: {formatDate(app.appliedAt)}</span>
                            {app.hasResume && <FileText className="w-3.5 h-3.5 text-indigo-600" />}
                          </div>

                          {/* Quick Transition Trigger */}
                          <div className="pt-2 flex items-center gap-1.5">
                            {app.status === 'APPLIED' && (
                              <Button
                                size="sm"
                                variant="outline"
                                className="w-full text-[10px] py-1 h-7"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  openStatusModal(app.id, 'UNDER_REVIEW');
                                }}
                              >
                                Review
                              </Button>
                            )}

                            {app.status === 'UNDER_REVIEW' && (
                              <Button
                                size="sm"
                                variant="primary"
                                className="w-full text-[10px] py-1 h-7"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  openStatusModal(app.id, 'SHORTLISTED');
                                }}
                              >
                                Shortlist
                              </Button>
                            )}

                            {app.status === 'SHORTLISTED' && (
                              <Button
                                size="sm"
                                variant="secondary"
                                className="w-full text-[10px] py-1 h-7"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  openStatusModal(app.id, 'INTERVIEW_SCHEDULED');
                                }}
                              >
                                Schedule Interview
                              </Button>
                            )}

                            {app.status === 'INTERVIEW_SCHEDULED' && (
                              <div className="flex items-center gap-1 w-full">
                                <Button
                                  size="sm"
                                  variant="primary"
                                  className="w-1/2 text-[10px] py-1 h-7 bg-emerald-600 hover:bg-emerald-700"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    openStatusModal(app.id, 'ACCEPTED');
                                  }}
                                >
                                  Offer
                                </Button>
                                <Button
                                  size="sm"
                                  variant="destructive"
                                  className="w-1/2 text-[10px] py-1 h-7"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    openStatusModal(app.id, 'REJECTED');
                                  }}
                                >
                                  Decline
                                </Button>
                              </div>
                            )}
                          </div>
                        </CardContent>
                      </Card>
                    ))
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* Table View */
        <Card>
          <CardContent className="p-0 overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3">Candidate</th>
                  <th className="px-6 py-3">Match Snapshot</th>
                  <th className="px-6 py-3">Stage</th>
                  <th className="px-6 py-3">Applied Date</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {applications.map((app) => (
                  <tr key={app.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="px-6 py-4">
                      <p className="font-bold text-slate-900">{app.candidateName}</p>
                      <p className="text-xs text-slate-400">{app.candidateEmail}</p>
                    </td>
                    <td className="px-6 py-4">
                      <ScoreGauge score={app.matchScoreAtApplication} size="sm" />
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={getStatusBadgeVariant(app.status)}>{app.status.replace('_', ' ')}</Badge>
                    </td>
                    <td className="px-6 py-4 text-xs">{formatDate(app.appliedAt)}</td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <Button size="sm" variant="outline" onClick={() => openDossier(app)}>
                        Dossier
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      {/* Candidate Dossier & Confidential Notes Drawer */}
      <Modal
        isOpen={isDossierOpen}
        onClose={() => setIsDossierOpen(false)}
        title="Candidate Dossier & Evaluation"
        description="Inspect candidate details, resume attachment, and private recruiter notes"
        maxWidth="lg"
      >
        {isDetailLoading || !appDetail ? (
          <LoadingSpinner text="Fetching candidate dossier..." />
        ) : (
          <div className="space-y-6">
            <div className="flex items-center justify-between p-4 rounded-xl bg-slate-50 border border-slate-100">
              <div>
                <h3 className="text-lg font-bold text-slate-900">{appDetail.candidateName}</h3>
                <p className="text-xs text-slate-500">{appDetail.candidateEmail} &bull; {appDetail.candidatePhone || 'No phone'}</p>
                <div className="mt-2">
                  <Badge variant={getStatusBadgeVariant(appDetail.status)}>{appDetail.status.replace('_', ' ')}</Badge>
                </div>
              </div>
              <ScoreGauge score={appDetail.matchScoreAtApplication} size="md" />
            </div>

            {/* Resume Action */}
            {appDetail.hasResume && (
              <div className="flex items-center justify-between p-3 rounded-lg border border-indigo-100 bg-indigo-50/50 text-xs">
                <span className="font-semibold text-indigo-900 flex items-center gap-1.5">
                  <FileText className="w-4 h-4 text-indigo-600" />
                  Candidate Resume Attached
                </span>
                <Button size="sm" onClick={() => handleDownloadResume(appDetail.id, appDetail.candidateName)}>
                  <Download className="w-3.5 h-3.5 mr-1" />
                  Download PDF
                </Button>
              </div>
            )}

            {/* Cover Letter */}
            {appDetail.coverLetter && (
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-1">Candidate Cover Letter</h4>
                <p className="p-3 bg-slate-50 rounded-lg text-xs text-slate-700 whitespace-pre-line border border-slate-200">
                  {appDetail.coverLetter}
                </p>
              </div>
            )}

            {/* Confidential Recruiter Notes */}
            <div className="space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                <MessageSquare className="w-3.5 h-3.5" />
                Confidential Recruiter Notes (Private)
              </h4>
              <Textarea
                placeholder="Internal interview remarks, salary negotiations, or candidate ratings (strictly hidden from candidate)..."
                defaultValue={appDetail.recruiterNotes || ''}
                onChange={(e) => setRecruiterNotes(e.target.value)}
                rows={3}
              />
              <div className="flex justify-end">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => saveNotesMutation.mutate({ appId: appDetail.id, notes: recruiterNotes })}
                  isLoading={saveNotesMutation.isPending}
                >
                  Save Internal Notes
                </Button>
              </div>
            </div>
          </div>
        )}
      </Modal>

      {/* Transition & Interview Scheduler Modal */}
      <Modal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title={`Transition Stage to: ${targetStatus.replace('_', ' ')}`}
        description="Update candidate status in applicant tracking pipeline"
      >
        <div className="space-y-4">
          {targetStatus === 'INTERVIEW_SCHEDULED' && (
            <Input
              label="Interview Date & Time *"
              type="datetime-local"
              value={interviewDate}
              onChange={(e) => setInterviewDate(e.target.value)}
            />
          )}

          <p className="text-xs text-slate-500">
            Confirming this transition will automatically notify the candidate with relevant status updates.
          </p>

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" size="sm" onClick={() => setIsStatusModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={() =>
                updateStatusMutation.mutate({
                  appId: selectedAppId,
                  status: targetStatus,
                  interviewScheduledAt: interviewDate || undefined,
                })
              }
              isLoading={updateStatusMutation.isPending}
              disabled={targetStatus === 'INTERVIEW_SCHEDULED' && !interviewDate}
            >
              Confirm Transition
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
