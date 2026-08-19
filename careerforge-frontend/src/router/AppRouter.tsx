import { Routes, Route, Navigate } from 'react-router-dom';
import { PublicLayout } from '@/components/layout/PublicLayout';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { ProtectedRoute } from '@/router/ProtectedRoute';
import { PublicRoute } from '@/router/PublicRoute';

// Public Feature Pages
import { LandingPage } from '@/features/jobs/LandingPage';
import { JobDiscoveryPage } from '@/features/jobs/JobDiscoveryPage';
import { JobDetailPage } from '@/features/jobs/JobDetailPage';
import { CompanyDirectoryPage } from '@/features/recruiter/CompanyDirectoryPage';
import { CompanyPublicDetailPage } from '@/features/recruiter/CompanyPublicDetailPage';

// Auth Pages
import { LoginPage } from '@/features/auth/LoginPage';
import { RegisterPage } from '@/features/auth/RegisterPage';

// Student Pages
import { StudentDashboardPage } from '@/features/student/StudentDashboardPage';
import { StudentProfilePage } from '@/features/student/StudentProfilePage';
import { StudentApplicationsPage } from '@/features/student/StudentApplicationsPage';
import { StudentSavedJobsPage } from '@/features/student/StudentSavedJobsPage';

// Recruiter Pages
import { RecruiterDashboardPage } from '@/features/recruiter/RecruiterDashboardPage';
import { RecruiterJobsPage } from '@/features/recruiter/RecruiterJobsPage';
import { JobEditorPage } from '@/features/recruiter/JobEditorPage';
import { RecruiterCompanyPage } from '@/features/recruiter/RecruiterCompanyPage';
import { RecruiterProfilePage } from '@/features/recruiter/RecruiterProfilePage';
import { RecruiterATSPage } from '@/features/recruiter/RecruiterATSPage';

// Notifications
import { NotificationListPage } from '@/features/notifications/NotificationListPage';

// Admin Pages
import { AdminDashboardPage } from '@/features/admin-analytics/AdminDashboardPage';
import { AdminUsersPage } from '@/features/admin-users/AdminUsersPage';
import { AdminCompaniesPage } from '@/features/admin-moderation/AdminCompaniesPage';
import { AdminJobsPage } from '@/features/admin-moderation/AdminJobsPage';
import { AdminAuditLogsPage } from '@/features/admin-audit/AdminAuditLogsPage';
import { AdminAnalyticsPage } from '@/features/admin-analytics/AdminAnalyticsPage';

export function AppRouter() {
  return (
    <Routes>
      {/* Public Pages */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<LandingPage />} />
        <Route path="/jobs" element={<JobDiscoveryPage />} />
        <Route path="/jobs/:slug" element={<JobDetailPage />} />
        <Route path="/companies" element={<CompanyDirectoryPage />} />
        <Route path="/companies/:slug" element={<CompanyPublicDetailPage />} />
      </Route>

      {/* Auth Unauthenticated Only */}
      <Route element={<PublicRoute />}>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>
      </Route>

      {/* Notifications (Any Authenticated Persona) */}
      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path="/notifications" element={<NotificationListPage />} />
        </Route>
      </Route>

      {/* Student Portal (ROLE_STUDENT) */}
      <Route element={<ProtectedRoute allowedRoles={['ROLE_STUDENT']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/student/dashboard" element={<StudentDashboardPage />} />
          <Route path="/student/profile" element={<StudentProfilePage />} />
          <Route path="/student/applications" element={<StudentApplicationsPage />} />
          <Route path="/student/saved-jobs" element={<StudentSavedJobsPage />} />
        </Route>
      </Route>

      {/* Recruiter Portal (ROLE_RECRUITER) */}
      <Route element={<ProtectedRoute allowedRoles={['ROLE_RECRUITER']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/recruiter/dashboard" element={<RecruiterDashboardPage />} />
          <Route path="/recruiter/jobs" element={<RecruiterJobsPage />} />
          <Route path="/recruiter/jobs/new" element={<JobEditorPage />} />
          <Route path="/recruiter/jobs/:id/edit" element={<JobEditorPage />} />
          <Route path="/recruiter/jobs/:jobId/applications" element={<RecruiterATSPage />} />
          <Route path="/recruiter/company" element={<RecruiterCompanyPage />} />
          <Route path="/recruiter/profile" element={<RecruiterProfilePage />} />
        </Route>
      </Route>

      {/* Admin Portal (ROLE_ADMIN) */}
      <Route element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
          <Route path="/admin/users" element={<AdminUsersPage />} />
          <Route path="/admin/companies" element={<AdminCompaniesPage />} />
          <Route path="/admin/jobs" element={<AdminJobsPage />} />
          <Route path="/admin/audit-logs" element={<AdminAuditLogsPage />} />
          <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
        </Route>
      </Route>

      {/* Fallback Catch-All */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
