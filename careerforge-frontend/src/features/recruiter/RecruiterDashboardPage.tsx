import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '@/features/auth/authStore';
import { recruiterService } from '@/services/recruiter.service';
import { jobService } from '@/services/job.service';
import { queryKeys } from '@/lib/queryClient';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge, getStatusBadgeVariant } from '@/components/ui/Badge';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Briefcase, Building2, Plus, Users, ShieldAlert, ArrowRight } from 'lucide-react';

export function RecruiterDashboardPage() {
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuthStore();
  const userId = user?.id;

  const {
    data: company,
    isLoading: isCompLoading,
    isError: isCompError,
    error: compError,
    refetch: refetchCompany,
  } = useQuery({
    queryKey: queryKeys.recruiter.company(userId),
    queryFn: () => recruiterService.getMyCompany(),
    enabled: isAuthenticated && !isAuthLoading && user?.role === 'ROLE_RECRUITER' && !!userId,
    retry: false,
    refetchInterval: (query) => (query.state.data ? 2000 : false),
    refetchIntervalInBackground: false,
  });

  const { data: jobsData, isLoading: isJobsLoading, isError: isJobsError, error: jobsError, refetch: refetchJobs } = useQuery({
    queryKey: queryKeys.recruiter.jobs({ page: 0, size: 5 }),
    queryFn: () => jobService.getRecruiterJobs({ page: 0, size: 5 }),
    enabled: isAuthenticated && !isAuthLoading && !!company?.id,
    retry: false,
    refetchInterval: 1500,
    refetchIntervalInBackground: false,
    placeholderData: (prev) => prev,
  });

  if (isAuthLoading || isCompLoading || (isJobsLoading && !jobsData && !!company?.id)) {
    return <LoadingSpinner text="Loading recruiter workspace..." />;
  }

  if (isCompError) {
    return (
      <ErrorState
        title="Could not load recruiter workspace"
        error={compError}
        onRetry={() => refetchCompany()}
      />
    );
  }

  // Recruiter not yet associated with a company
  if (!company) {
    return (
      <div className="space-y-8">
        <PageHeader
          title="Employer Recruitment Portal"
          description="Manage job postings, review algorithmic candidate matches, and track applicants through your hiring stages"
        />

        <Card className="border-indigo-100 bg-gradient-to-br from-indigo-50/50 via-white to-slate-50">
          <CardContent className="p-8 space-y-6">
            <div className="flex items-start gap-4">
              <div className="w-12 h-12 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shrink-0 shadow-md shadow-indigo-200">
                <Building2 className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="text-xl font-bold text-slate-900">Welcome to CareerForge Employer Portal</h3>
                <p className="text-sm text-slate-600 leading-relaxed">
                  Your recruiter account is active! Complete your organizational registration to unlock job creation, candidate pipeline tracking, and algorithmic skill match scoring.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
              <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs space-y-2">
                <div className="w-7 h-7 rounded-lg bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs">
                  1
                </div>
                <h4 className="text-sm font-bold text-slate-900">Register Company</h4>
                <p className="text-xs text-slate-500">Provide company name, industry, headquarters, and web presence.</p>
              </div>

              <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs space-y-2 opacity-80">
                <div className="w-7 h-7 rounded-lg bg-slate-100 text-slate-700 flex items-center justify-center font-bold text-xs">
                  2
                </div>
                <h4 className="text-sm font-bold text-slate-900">Verification</h4>
                <p className="text-xs text-slate-500">Platform administrators verify your organization credentials.</p>
              </div>

              <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs space-y-2 opacity-80">
                <div className="w-7 h-7 rounded-lg bg-slate-100 text-slate-700 flex items-center justify-center font-bold text-xs">
                  3
                </div>
                <h4 className="text-sm font-bold text-slate-900">Post Jobs & Hire</h4>
                <p className="text-xs text-slate-500">Publish open positions and evaluate scored student applications.</p>
              </div>
            </div>

            <div className="pt-2">
              <Link to="/recruiter/company">
                <Button size="md" className="shadow-sm">
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

  if (isJobsError) {
    return (
      <ErrorState
        error={jobsError}
        onRetry={() => refetchJobs()}
      />
    );
  }

  const jobs = jobsData?.content || [];
  const publishedCount = jobs.filter((j) => j.status === 'PUBLISHED').length;

  return (
    <div className="space-y-8">
      <PageHeader
        title="Employer Recruitment Portal"
        description="Manage job postings, review algorithmic candidate matches, and track applicants through your hiring stages"
        actions={
          <Link to="/recruiter/jobs/new">
            <Button size="sm">
              <Plus className="w-4 h-4 mr-2" />
              Post New Job
            </Button>
          </Link>
        }
      />

      {/* Verification Status Alert Banner */}
      {company && company.verificationStatus !== 'VERIFIED' && (
        <div className="p-4 rounded-xl border border-amber-200 bg-amber-50 flex items-start gap-3 text-amber-900">
          <ShieldAlert className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="space-y-1 text-xs">
            <p className="font-bold">
              Organization Status: {company.verificationStatus}
            </p>
            <p className="text-amber-700">
              Your company verification is currently pending administrative review. You can create draft jobs, but publishing requires verified organization status.
            </p>
          </div>
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Company Profile</p>
              <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <Building2 className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-xl font-bold text-slate-900 mt-2 truncate">{company?.name || 'No Company Registered'}</h3>
            <div className="mt-2">
              <Badge variant={getStatusBadgeVariant(company?.verificationStatus || 'PENDING')}>
                {company?.verificationStatus || 'UNREGISTERED'}
              </Badge>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Active Published Jobs</p>
              <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <Briefcase className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-3xl font-extrabold text-slate-900 mt-2">{publishedCount}</h3>
            <p className="text-xs text-slate-400 mt-1">{jobs.length} total postings managed</p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Recruitment Pipeline</p>
              <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
                <Users className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-3xl font-extrabold text-slate-900 mt-2">ATS Active</h3>
            <p className="text-xs text-slate-400 mt-1">Direct ATS applicant tracking</p>
          </CardContent>
        </Card>
      </div>

      {/* Recent Job Postings */}
      <Card>
        <CardHeader className="flex items-center justify-between">
          <CardTitle>Recent Job Openings</CardTitle>
          <Link to="/recruiter/jobs" className="text-xs font-semibold text-indigo-600 hover:underline flex items-center gap-1">
            View All ({jobsData?.totalElements || 0}) <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </CardHeader>
        <CardContent className="p-0">
          {jobs.length === 0 ? (
            <div className="p-8 text-center">
              <p className="text-xs text-slate-500 mb-3">You have not created any job postings yet.</p>
              <Link to="/recruiter/jobs/new">
                <Button size="sm">Create First Job Posting</Button>
              </Link>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {jobs.map((job) => (
                <div key={job.id} className="p-5 flex items-center justify-between gap-4 hover:bg-slate-50/60 transition-colors">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-slate-900">{job.title}</h4>
                      <Badge variant={getStatusBadgeVariant(job.status)}>{job.status}</Badge>
                      <Badge variant="outline">{job.workMode}</Badge>
                    </div>
                    <p className="text-xs text-slate-500">
                      {job.location || 'Remote'} &bull; {job.skills?.length || 0} Target Skills
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <Link to={`/recruiter/jobs/${job.id}/applications`}>
                      <Button size="sm" variant="secondary">
                        <Users className="w-3.5 h-3.5 mr-1.5" />
                        ATS Pipeline
                      </Button>
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
