import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminService } from '@/services/admin.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Select } from '@/components/ui/Select';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  LineChart,
  Line,
  CartesianGrid,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { BarChart3, TrendingUp, Users, Briefcase } from 'lucide-react';

const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

export function AdminAnalyticsPage() {
  const [trendsWindowDays, setTrendsWindowDays] = useState(30);

  // Overview Query
  const { isLoading: isOverviewLoading } = useQuery({
    queryKey: queryKeys.admin.analyticsOverview,
    queryFn: () => adminService.getAnalyticsOverview(),
  });

  // Funnel Query
  const { data: funnel, isLoading: isFunnelLoading } = useQuery({
    queryKey: queryKeys.admin.analyticsFunnel(),
    queryFn: () => adminService.getAnalyticsFunnel(),
  });

  // Job Analytics Query
  const { data: jobAnalytics, isLoading: isJobsLoading } = useQuery({
    queryKey: queryKeys.admin.analyticsJobs(),
    queryFn: () => adminService.getAnalyticsJobs(),
  });

  // Company Analytics Query
  const { isLoading: isCompsLoading } = useQuery({
    queryKey: queryKeys.admin.analyticsCompanies,
    queryFn: () => adminService.getAnalyticsCompanies(),
  });

  // User Analytics Query
  const { data: userAnalytics, isLoading: isUsersLoading } = useQuery({
    queryKey: queryKeys.admin.analyticsUsers,
    queryFn: () => adminService.getAnalyticsUsers(),
  });

  // Trends Query
  const { data: trends, isLoading: isTrendsLoading } = useQuery({
    queryKey: queryKeys.admin.analyticsTrends(trendsWindowDays),
    queryFn: () => adminService.getAnalyticsTrends(trendsWindowDays),
  });

  const isLoading =
    isOverviewLoading ||
    isFunnelLoading ||
    isJobsLoading ||
    isCompsLoading ||
    isUsersLoading ||
    isTrendsLoading;

  if (isLoading) return <LoadingSpinner text="Aggregating platform metrics and time-series trends..." />;

  // Prepare Funnel Data
  const funnelChartData = funnel
    ? [
        { stage: 'Applied', count: funnel.appliedCount },
        { stage: 'Under Review', count: funnel.underReviewCount },
        { stage: 'Shortlisted', count: funnel.shortlistedCount },
        { stage: 'Interview', count: funnel.interviewScheduledCount },
        { stage: 'Accepted', count: funnel.acceptedCount },
        { stage: 'Rejected', count: funnel.rejectedCount },
        { stage: 'Withdrawn', count: funnel.withdrawnCount },
      ]
    : [];

  // Prepare WorkMode Distribution Data
  const workModeData = jobAnalytics?.jobsByWorkMode
    ? Object.entries(jobAnalytics.jobsByWorkMode).map(([mode, count]) => ({
        name: mode,
        value: count,
      }))
    : [];

  // Prepare Role Distribution Data
  const roleData = userAnalytics?.usersByRole
    ? Object.entries(userAnalytics.usersByRole).map(([role, count]) => ({
        name: role.replace('ROLE_', ''),
        value: count,
      }))
    : [];

  // Prepare Time Series Trend Data
  const trendDates = Array.from(
    new Set([
      ...(trends?.userRegistrations?.map((d) => d.date) || []),
      ...(trends?.jobPostings?.map((d) => d.date) || []),
      ...(trends?.applicationSubmissions?.map((d) => d.date) || []),
    ])
  ).sort();

  const timeSeriesData = trendDates.map((date) => ({
    date: date.substring(5), // MM-DD
    Registrations: trends?.userRegistrations?.find((d) => d.date === date)?.count || 0,
    Jobs: trends?.jobPostings?.find((d) => d.date === date)?.count || 0,
    Applications: trends?.applicationSubmissions?.find((d) => d.date === date)?.count || 0,
  }));

  return (
    <div className="space-y-8">
      <PageHeader
        title="Platform Analytics & Business Intelligence"
        description="Real-time server-side aggregations, application conversion funnels, and time-series trends"
        actions={
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-slate-500 whitespace-nowrap">Trend Window:</span>
            <Select
              options={[
                { label: '7 Days', value: 7 },
                { label: '14 Days', value: 14 },
                { label: '30 Days', value: 30 },
                { label: '90 Days', value: 90 },
                { label: '365 Days', value: 365 },
              ]}
              value={trendsWindowDays}
              onChange={(e) => setTrendsWindowDays(Number(e.target.value))}
            />
          </div>
        }
      />

      {/* Funnel Metrics & Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-indigo-600" />
            Application Pipeline & Lifecycle Conversion Funnel
          </CardTitle>
        </CardHeader>
        <CardContent className="p-6 space-y-6">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200 text-xs">
            <div>
              <span className="text-slate-400 font-medium">Total Applications</span>
              <p className="text-xl font-bold text-slate-900 mt-0.5">{funnel?.totalApplications || 0}</p>
            </div>
            <div>
              <span className="text-slate-400 font-medium">Interview Rate</span>
              <p className="text-xl font-bold text-indigo-600 mt-0.5">
                {funnel?.interviewRatePercentage?.toFixed(1)}%
              </p>
            </div>
            <div>
              <span className="text-slate-400 font-medium">Acceptance Rate</span>
              <p className="text-xl font-bold text-emerald-600 mt-0.5">
                {funnel?.acceptanceRatePercentage?.toFixed(1)}%
              </p>
            </div>
            <div>
              <span className="text-slate-400 font-medium">Rejection Rate</span>
              <p className="text-xl font-bold text-rose-600 mt-0.5">
                {funnel?.rejectionRatePercentage?.toFixed(1)}%
              </p>
            </div>
          </div>

          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={funnelChartData} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="stage" stroke="#64748b" fontSize={12} />
                <YAxis stroke="#64748b" fontSize={12} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#ffffff', borderRadius: '8px', border: '1px solid #e2e8f0' }}
                />
                <Bar dataKey="count" fill="#4f46e5" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>

      {/* Time-Series Trends Graph */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <BarChart3 className="w-5 h-5 text-indigo-600" />
            Platform Activity Trends ({trendsWindowDays} Days)
          </CardTitle>
        </CardHeader>
        <CardContent className="p-6">
          <div className="h-80 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={timeSeriesData} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="date" stroke="#64748b" fontSize={12} />
                <YAxis stroke="#64748b" fontSize={12} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#ffffff', borderRadius: '8px', border: '1px solid #e2e8f0' }}
                />
                <Legend />
                <Line type="monotone" dataKey="Registrations" stroke="#4f46e5" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="Jobs" stroke="#06b6d4" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="Applications" stroke="#10b981" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>

      {/* Marketplace Distributions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Briefcase className="w-5 h-5 text-indigo-600" />
              Jobs by Work Mode Distribution
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={workModeData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {workModeData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Users className="w-5 h-5 text-indigo-600" />
              User Demographic Roles
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={roleData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {roleData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
