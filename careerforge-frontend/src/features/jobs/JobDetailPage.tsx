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
} from 'lucide-react';

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
      queryClient.invalidateQueries({ queryKey: queryKeys.student.isSaved(job!.id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.student.savedJobs() });
    },
  });

  if (isLoading) return <LoadingSpinner text="Loading job dossier..." />;
  if (isError || !job) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <ErrorState
          title="Job Not Found"
          message={(error as any)?.response?.data?.message || 'This job opening may have been closed or archived.'}
          onRetry={() => navigate('/jobs')}
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
                  {job.skills.map((s) => (
                    <div
                      key={s.id}
                      className={`p-3 rounded-lg border text-xs ${
                        s.isRequired ? 'bg-indigo-50/50 border-indigo-100' : 'bg-slate-50 border-slate-200'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-slate-900">{s.skillName}</span>
                        <span className="text-[10px] font-semibold uppercase text-slate-500">
                          {s.isRequired ? 'Required' : 'Optional'}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-500 mt-1">
                        Min. Proficiency: <span className="font-medium text-slate-700">{s.minimumProficiency}</span>
                      </p>
                    </div>
                  ))}
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

                  <Button className="w-full justify-center" onClick={() => setIsApplyModalOpen(true)}>
                    <Send className="w-4 h-4 mr-2" />
                    Apply Now
                  </Button>

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
