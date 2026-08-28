import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobService } from '@/services/job.service';
import { applicationService } from '@/services/application.service';
import { queryKeys } from '@/lib/queryClient';
import { useAuthStore } from '@/features/auth/authStore';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Card, CardContent } from '@/components/ui/Card';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ErrorState } from '@/components/feedback/ErrorState';
import { MatchPreviewModal } from '@/features/student/MatchPreviewModal';
import { ApplyModal } from '@/features/student/ApplyModal';
import { formatCurrency, formatDate } from '@/lib/utils';
import {
  Building2,
  Bookmark,
  BookmarkCheck,
  Sparkles,
  Send,
  ArrowLeft,
  CheckCircle2,
  Search,
} from 'lucide-react';
import { notificationService } from '@/services/notification.service';

export function JobDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();
  const queryClient = useQueryClient();

  const [isMatchModalOpen, setIsMatchModalOpen] = useState(false);
  const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);

  const { data: job, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.publicJobs.detail(slug || ''),
    queryFn: () => {
      // Slug or numeric ID support
      if (slug && !isNaN(Number(slug))) {
        return jobService.getJobById(Number(slug));
      }
      return jobService.getJobBySlug(slug || '');
    },
    enabled: !!slug,
  });

  const isStudent = isAuthenticated && user?.role === 'ROLE_STUDENT';

  // Student applications query to determine applied status
  const { data: studentApps, isLoading: isStudentAppsLoading } = useQuery({
    queryKey: ['student', 'applications'],
    queryFn: () => applicationService.getStudentApplications({ size: 100 }),
    enabled: isStudent,
  });

  // Student notifications query to detect historical application context
  const { data: notificationsData, isLoading: isNotificationsLoading } = useQuery({
    queryKey: ['notifications', 'historical'],
    queryFn: () => notificationService.getNotifications({ page: 0, size: 50 }),
    enabled: isStudent,
  });

  const existingApplication = studentApps?.content?.find(
    (app) => (app.jobSlug === slug || String(app.jobId) === slug || (job && app.jobId === job.id)) && app.status !== 'WITHDRAWN'
  );
  const hasApplied = !!existingApplication;

  // Saved job query
  const { data: isSaved = false } = useQuery({
    queryKey: queryKeys.student.isSaved(job?.id || 0),
    queryFn: () => applicationService.isJobSaved(job!.id),
    enabled: isStudent && !!job?.id,
  });

  const toggleSaveMutation = useMutation({
    mutationFn: async () => {
      if (!job) return;
      if (isSaved) {
        await applicationService.removeSavedJob(job.id);
      } else {
        await applicationService.saveJob(job.id);
      }
    },
    onSuccess: () => {
      queryClient.setQueryData(queryKeys.student.isSaved(job!.id), !isSaved);
      queryClient.invalidateQueries({ queryKey: queryKeys.student.isSaved(job!.id) });
      queryClient.invalidateQueries({ queryKey: ['student', 'saved-jobs'] });
    },
  });

  if (isLoading || (isStudent && (isStudentAppsLoading || isNotificationsLoading))) {
    return <LoadingSpinner text="Loading job dossier..." />;
  }
  if (isError || !job) {
    // 1. If the authenticated student has an existing application for this now-unavailable job:
    if (existingApplication) {
      let customTitle = 'Job Currently Unavailable';
      let customMessage = `The ${existingApplication.jobTitle} position at ${existingApplication.companyName} is currently unavailable because the recruiter has paused or unpublished this job. Your application has not been deleted and your application status remains ${existingApplication.status.replace('_', ' ')}.`;

      if (existingApplication.jobStatus === 'CLOSED') {
        customTitle = 'Job Closed';
        customMessage = `The ${existingApplication.jobTitle} position at ${existingApplication.companyName} is now closed. Your application is still intact and your current application status is ${existingApplication.status.replace('_', ' ')}.`;
      } else if (existingApplication.jobStatus === 'ARCHIVED') {
        customTitle = 'Job Archived';
        customMessage = `The ${existingApplication.jobTitle} position at ${existingApplication.companyName} has been archived. Your application and application history remain intact and are still available from your Applications page.`;
      }

      return (
        <div className="max-w-4xl mx-auto px-4 py-12">
          <ErrorState
            title={customTitle}
            message={customMessage}
            onRetry={() => navigate('/student/applications')}
            retryText="Back to Applications"
            secondaryAction={{
              label: 'Explore Other Jobs',
              onClick: () => navigate('/jobs'),
              icon: ArrowLeft,
              variant: 'outline',
            }}
          />
        </div>
      );
    }

    // 2. If the authenticated student previously applied, but that job/application was deleted/removed:
    const matchedNotif = notificationsData?.content?.find((n) => {
      const isAppOrJobNotif =
        n.type?.startsWith('APPLICATION') ||
        n.type === 'JOB_POSTING_CLOSED' ||
        n.title?.toLowerCase().includes('application') ||
        n.title?.toLowerCase().includes('job position deleted') ||
        n.title?.toLowerCase().includes('job deleted') ||
        n.message?.toLowerCase().includes('application') ||
        n.message?.toLowerCase().includes('job position deleted') ||
        n.message?.toLowerCase().includes('has been deleted due to business requirements');
      if (!isAppOrJobNotif) return false;

      if (slug && !isNaN(Number(slug)) && n.relatedEntityType === 'JOB' && n.relatedEntityId === Number(slug)) {
        return true;
      }

      if (slug) {
        const slugClean = slug.replace(/-[a-f0-9]{4,}$/i, '').replace(/-/g, ' ').toLowerCase().trim();
        if (slugClean.length >= 3 && (n.title?.toLowerCase().includes(slugClean) || n.message?.toLowerCase().includes(slugClean))) {
          return true;
        }
      }
      return false;
    });

    if (matchedNotif) {
      const match1 = matchedNotif.message.match(/for '([^']+)' at (.+?)(?:\s+has|\s+is|\.|$)/i);
      const match2 = matchedNotif.message.match(/position '([^']+)' at (.+?)(?:\s+has|\s+is|\.|$)/i);
      const match3 = matchedNotif.message.match(/The '([^']+)' position at (.+?)(?:\s+has|\s+is|\.|$)/i);
      const match4 = matchedNotif.title.match(/(?:Application (?:Submitted|Update)|Job Position Deleted): (.+)/i);

      const jobTitle =
        match3?.[1] ||
        match1?.[1] ||
        match2?.[1] ||
        match4?.[1] ||
        (slug ? slug.replace(/-[a-f0-9]{4,}$/i, '').replace(/-/g, ' ') : 'requested');
      const companyName = match3?.[2]?.trim() || match1?.[2]?.trim() || match2?.[2]?.trim() || 'the company';

      return (
        <div className="max-w-4xl mx-auto px-4 py-12">
          <ErrorState
            title="Job Position Deleted"
            message={`The '${jobTitle}' position at '${companyName}' has been deleted due to business requirements. We're sorry for the inconvenience. Your application is no longer active.`}
            primaryAction={{
              label: 'Search for Another Job',
              onClick: () => navigate('/jobs'),
              icon: Search,
              variant: 'primary',
            }}
            secondaryAction={{
              label: 'Back to Applications',
              onClick: () => navigate('/student/applications'),
              icon: ArrowLeft,
              variant: 'outline',
            }}
          />
        </div>
      );
    }

    // 3. Standard public 404 for genuinely invalid or nonexistent jobs:
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <ErrorState
          error={error}
          title={!job && !error ? 'Job Not Found' : undefined}
          message={!job && !error ? 'This job opening may have been closed or archived.' : undefined}
          onRetry={() => navigate('/jobs')}
          retryText="Back to Job Directory"
        />
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Link to="/jobs" className="inline-flex items-center text-xs font-semibold text-slate-500 hover:text-indigo-600 mb-6">
        <ArrowLeft className="w-4 h-4 mr-1" />
        Back to Job Directory
      </Link>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Job Body */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardContent className="p-6 sm:p-8 space-y-6">
              <div>
                <div className="flex items-center gap-2 flex-wrap mb-3">
                  <Badge variant="info">{job.workMode}</Badge>
                  <Badge variant="outline">{job.jobType.replace('_', ' ')}</Badge>
                  <Badge variant="default">{job.experienceLevel.replace('_', ' ')}</Badge>
                </div>
                <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">{job.title}</h1>
                <Link
                  to={`/companies/${job.companySlug || job.companyId}`}
                  className="inline-flex items-center gap-1.5 text-base font-semibold text-indigo-600 hover:text-indigo-700 mt-2"
                >
                  <Building2 className="w-4 h-4" />
                  {job.companyName}
                </Link>
              </div>

              {/* Overview grid */}
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100 text-xs">
                <div>
                  <span className="text-slate-400">Location</span>
                  <p className="font-semibold text-slate-900 mt-0.5">{job.location || 'Remote'}</p>
                </div>
                <div>
                  <span className="text-slate-400">Offered Salary</span>
                  <p className="font-semibold text-slate-900 mt-0.5">
                    {job.salaryMin || job.salaryMax
                      ? `${formatCurrency(job.salaryMin, job.currency)} - ${formatCurrency(job.salaryMax, job.currency)}`
                      : 'Competitive'}
                  </p>
                </div>
                <div>
                  <span className="text-slate-400">Application Deadline</span>
                  <p className="font-semibold text-slate-900 mt-0.5">{formatDate(job.deadline)}</p>
                </div>
              </div>

              {/* Job Description */}
              <div>
                <h3 className="text-base font-bold text-slate-900 mb-3">Job Description</h3>
                <div className="prose prose-slate max-w-none text-sm text-slate-700 whitespace-pre-line leading-relaxed">
                  {job.description}
                </div>
              </div>

              {/* Required & Optional Skills */}
              <div>
                <h3 className="text-base font-bold text-slate-900 mb-3">Target Technical Competencies</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {job.skills.map((s) => {
                    const isRequiredSkill = s.required ?? s.isRequired;
                    const statusLabel = isRequiredSkill === true ? 'REQUIRED' : isRequiredSkill === false ? 'OPTIONAL' : 'UNKNOWN';
                    return (
                      <div
                        key={s.id}
                        className={`p-3 rounded-lg border text-xs ${
                          isRequiredSkill
                            ? 'bg-indigo-50/50 border-indigo-200'
                            : 'bg-slate-50 border-slate-200'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-slate-900">{s.skillName}</span>
                          <span
                            className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${
                              isRequiredSkill
                                ? 'bg-indigo-100 text-indigo-700'
                                : 'bg-slate-200/80 text-slate-600'
                            }`}
                          >
                            {statusLabel}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-1">
                          Min. Proficiency: <span className="font-medium text-slate-700">{s.minimumProficiency}</span>
                        </p>
                      </div>
                    );
                  })}
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Action Sidebar */}
        <div className="space-y-6">
          <Card>
            <CardContent className="p-6 space-y-4">
              <h3 className="text-base font-bold text-slate-900">Application Actions</h3>

              {isStudent ? (
                <>
                  <Button
                    variant="outline"
                    className="w-full justify-center"
                    onClick={() => setIsMatchModalOpen(true)}
                  >
                    <Sparkles className="w-4 h-4 mr-2 text-indigo-600" />
                    Check Skill Match
                  </Button>

                  {hasApplied ? (
                    <Button
                      variant="outline"
                      className="w-full justify-center bg-emerald-50 border-emerald-300 text-emerald-700 font-semibold cursor-default hover:bg-emerald-50"
                      disabled
                    >
                      <CheckCircle2 className="w-4 h-4 mr-2 text-emerald-600" />
                      Applied
                    </Button>
                  ) : (
                    <Button className="w-full justify-center" onClick={() => setIsApplyModalOpen(true)}>
                      <Send className="w-4 h-4 mr-2" />
                      Apply Now
                    </Button>
                  )}

                  <Button
                    variant="ghost"
                    className="w-full justify-center text-slate-600"
                    onClick={() => toggleSaveMutation.mutate()}
                    isLoading={toggleSaveMutation.isPending}
                  >
                    {isSaved ? (
                      <>
                        <BookmarkCheck className="w-4 h-4 mr-2 text-indigo-600" />
                        Saved in Bookmarks
                      </>
                    ) : (
                      <>
                        <Bookmark className="w-4 h-4 mr-2" />
                        Bookmark Job
                      </>
                    )}
                  </Button>
                </>
              ) : isAuthenticated ? (
                <p className="text-xs text-slate-500 text-center">
                  You are signed in with an administrative or recruiter account. Sign in as a candidate to apply.
                </p>
              ) : (
                <div className="space-y-3">
                  <Button className="w-full justify-center" onClick={() => navigate('/login')}>
                    Sign In to Apply
                  </Button>
                  <Button variant="outline" className="w-full justify-center" onClick={() => navigate('/register')}>
                    Create Candidate Profile
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Company Brief Card */}
          <Card>
            <CardContent className="p-6 space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">About the Employer</h4>
              <p className="text-sm font-bold text-slate-900">{job.companyName}</p>
              <Link
                to={`/companies/${job.companySlug || job.companyId}`}
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 inline-block"
              >
                View full company dossier &rarr;
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Match Preview Modal */}
      {isStudent && (
        <MatchPreviewModal
          isOpen={isMatchModalOpen}
          onClose={() => setIsMatchModalOpen(false)}
          jobId={job.id}
        />
      )}

      {/* Apply Modal */}
      {isStudent && (
        <ApplyModal
          isOpen={isApplyModalOpen}
          onClose={() => setIsApplyModalOpen(false)}
          jobId={job.id}
          jobTitle={job.title}
          companyName={job.companyName}
        />
      )}
    </div>
  );
}
