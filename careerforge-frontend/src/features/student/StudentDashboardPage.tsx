import { useQuery } from '@tanstack/react-query';
import { studentService } from '@/services/student.service';
import { applicationService } from '@/services/application.service';
import { queryKeys } from '@/lib/queryClient';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge, getStatusBadgeVariant } from '@/components/ui/Badge';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ErrorState } from '@/components/feedback/ErrorState';
import { ScoreGauge } from '@/components/ui/ScoreGauge';
import { formatDate } from '@/lib/utils';
import { FileCheck, Sparkles, User, ArrowRight } from 'lucide-react';

export function StudentDashboardPage() {
  const { data: profile, isLoading: isProfileLoading, isError: isProfileError } = useQuery({
    queryKey: queryKeys.student.profile,
    queryFn: () => studentService.getProfile(),
  });

  const { data: applications, isLoading: isAppsLoading, isError: isAppsError, error, refetch } = useQuery({
    queryKey: queryKeys.student.applications({ page: 0, size: 5 }),
    queryFn: () => applicationService.getStudentApplications({ page: 0, size: 5 }),
  });

  if (isProfileLoading || isAppsLoading) {
    return <LoadingSpinner text="Loading student workspace..." />;
  }

  if (isProfileError || isAppsError) {
    return (
      <ErrorState
        title="Could not load dashboard"
        message={(error as any)?.response?.data?.message || 'Failed to load your student dashboard'}
        onRetry={() => refetch()}
      />
    );
  }

  const appsList = applications?.content || [];
  const activeAppsCount = appsList.filter((a) => !['REJECTED', 'WITHDRAWN'].includes(a.status)).length;
  const activeResume = profile?.resumes?.find((r) => r.isActive);

  return (
    <div className="space-y-8">
      <PageHeader
        title={profile?.firstName ? `Welcome back, ${profile.firstName}` : 'Student Workspace'}
        description="Monitor your applications, skill profile completeness, and algorithmic job matching recommendations"
      />

      {/* Top 3 KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <Card className="hover:border-indigo-200 transition-all">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Profile Completeness</p>
              <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <User className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-3xl font-extrabold text-slate-900 mt-2">
              {profile?.profileCompletionPercentage || 0}% Complete
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              {profile?.skills?.length || 0} verified skills in profile
            </p>
          </CardContent>
        </Card>

        <Card className="hover:border-indigo-200 transition-all">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Active Applications</p>
              <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <Sparkles className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-3xl font-extrabold text-slate-900 mt-2">{activeAppsCount}</h3>
            <p className="text-xs text-slate-400 mt-1">{appsList.length} total applications submitted</p>
          </CardContent>
        </Card>

        <Card className="hover:border-indigo-200 transition-all">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Active Resume</p>
              <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
                <FileCheck className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-lg font-bold text-slate-900 mt-2 truncate">
              {activeResume ? activeResume.originalFileName : 'No Resume Uploaded'}
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              {activeResume ? `v${activeResume.version} • Active for applications` : 'Upload a PDF to apply'}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Recent Applications Pipeline Tracker */}
      <Card>
        <CardHeader className="flex items-center justify-between">
          <CardTitle>Recent Job Applications</CardTitle>
          <Link to="/student/applications" className="text-xs font-semibold text-indigo-600 hover:underline flex items-center gap-1">
            View All ({applications?.totalElements || 0}) <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </CardHeader>
        <CardContent className="p-0">
          {appsList.length === 0 ? (
            <div className="p-8 text-center">
              <p className="text-xs text-slate-500 mb-3">You haven't submitted any job applications yet.</p>
              <Link to="/jobs">
                <Button size="sm">Explore Open Positions</Button>
              </Link>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {appsList.map((app) => (
                <div key={app.id} className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-50/60 transition-colors">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <Link to={`/jobs/${app.jobSlug || app.jobId}`} className="text-sm font-bold text-slate-900 hover:text-indigo-600">
                        {app.jobTitle}
                      </Link>
                      <Badge variant={getStatusBadgeVariant(app.status)}>{app.status.replace('_', ' ')}</Badge>
                    </div>
                    <p className="text-xs text-slate-500">
                      {app.companyName} &bull; Applied on {formatDate(app.appliedAt)}
                    </p>
                  </div>

                  <div className="flex items-center gap-4">
                    <ScoreGauge score={app.matchScoreAtApplication} size="sm" />
                    <Link to="/student/applications">
                      <Button size="sm" variant="ghost">
                        Tracker &rarr;
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
