import { useQuery } from '@tanstack/react-query';
import { adminService } from '@/services/admin.service';
import { queryKeys } from '@/lib/queryClient';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ErrorState } from '@/components/feedback/ErrorState';
import {
  Users,
  Building2,
  Briefcase,
  FileText,
  ShieldCheck,
  Activity,
  BarChart3,
  AlertTriangle,
} from 'lucide-react';

export function AdminDashboardPage() {
  const { data: overview, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.admin.analyticsOverview,
    queryFn: () => adminService.getAnalyticsOverview(),
  });

  if (isLoading) return <LoadingSpinner text="Loading administrative dashboard..." />;
  if (isError) {
    return (
      <ErrorState
        title="Could not load admin overview"
        message={(error as any)?.response?.data?.message || 'Failed to retrieve platform analytics summary'}
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-8">
      <PageHeader
        title="Administrative Governance & Platform Health"
        description="System overview, moderation queues, audit metrics, and database-aggregated KPIs"
        actions={
          <div className="flex items-center gap-3">
            <Link to="/admin/analytics">
              <Button size="sm" variant="outline">
                <BarChart3 className="w-4 h-4 mr-1.5" />
                Deep Analytics
              </Button>
            </Link>
            <Link to="/admin/audit-logs">
              <Button size="sm" variant="secondary">
                <Activity className="w-4 h-4 mr-1.5" />
                Audit Trail
              </Button>
            </Link>
          </div>
        }
      />

      {/* Top 4 KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card className="hover:border-indigo-200 transition-all">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Total Users</p>
              <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <Users className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-3xl font-extrabold text-slate-900 mt-2">{overview?.totalUsers || 0}</h3>
            <div className="flex items-center gap-2 text-xs text-slate-500 mt-2">
              <span>{overview?.totalStudents || 0} Students</span>
              <span>&bull;</span>
              <span>{overview?.totalRecruiters || 0} Recruiters</span>
            </div>
          </CardContent>
        </Card>

        <Card className="hover:border-indigo-200 transition-all">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Hiring Companies</p>
              <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <Building2 className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-3xl font-extrabold text-slate-900 mt-2">{overview?.totalCompanies || 0}</h3>
            <div className="flex items-center gap-2 text-xs text-slate-500 mt-2">
              <span className="text-emerald-700 font-semibold">{overview?.verifiedCompanies || 0} Verified</span>
              <span>&bull;</span>
              <span className="text-amber-700 font-semibold">{overview?.pendingCompanies || 0} Pending</span>
            </div>
          </CardContent>
        </Card>

        <Card className="hover:border-indigo-200 transition-all">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Platform Jobs</p>
              <div className="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
                <Briefcase className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-3xl font-extrabold text-slate-900 mt-2">{overview?.totalJobs || 0}</h3>
            <div className="flex items-center gap-2 text-xs text-slate-500 mt-2">
              <span className="text-sky-700 font-semibold">{overview?.publishedJobs || 0} Published</span>
              <span>&bull;</span>
              <span>{overview?.draftJobs || 0} Drafts</span>
            </div>
          </CardContent>
        </Card>

        <Card className="hover:border-indigo-200 transition-all">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Applications</p>
              <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
                <FileText className="w-4 h-4" />
              </div>
            </div>
            <h3 className="text-3xl font-extrabold text-slate-900 mt-2">{overview?.totalApplications || 0}</h3>
            <div className="flex items-center gap-2 text-xs text-slate-500 mt-2">
              <span className="text-emerald-700 font-semibold">{overview?.acceptedApplications || 0} Placed</span>
              <span>&bull;</span>
              <span>{overview?.activeApplications || 0} In Pipeline</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Moderation Fast-Tracks */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader className="flex items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-indigo-600" />
              Company Verification Queue
            </CardTitle>
            <Link to="/admin/companies" className="text-xs font-semibold text-indigo-600 hover:underline">
              Open Queue &rarr;
            </Link>
          </CardHeader>
          <CardContent className="p-6 space-y-4">
            <div className="flex items-center justify-between p-4 rounded-xl bg-amber-50/60 border border-amber-200">
              <div className="flex items-center gap-3">
                <AlertTriangle className="w-5 h-5 text-amber-600" />
                <div>
                  <p className="text-sm font-bold text-slate-900">
                    {overview?.pendingCompanies || 0} Companies Pending Verification
                  </p>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Unverified employers cannot publish jobs to the public directory
                  </p>
                </div>
              </div>
              <Link to="/admin/companies">
                <Button size="sm" variant="outline">
                  Review
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <Users className="w-5 h-5 text-indigo-600" />
              User Account Integrity
            </CardTitle>
            <Link to="/admin/users" className="text-xs font-semibold text-indigo-600 hover:underline">
              User Directory &rarr;
            </Link>
          </CardHeader>
          <CardContent className="p-6 space-y-4">
            <div className="flex items-center justify-between p-4 rounded-xl bg-slate-50 border border-slate-200">
              <div>
                <p className="text-sm font-bold text-slate-900">
                  {overview?.activeEnabledUsers || 0} Active Enabled Accounts
                </p>
                <p className="text-xs text-slate-500 mt-0.5">
                  {overview?.disabledUsers || 0} accounts currently disabled by administration
                </p>
              </div>
              <Link to="/admin/users">
                <Button size="sm" variant="outline">
                  Manage
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
