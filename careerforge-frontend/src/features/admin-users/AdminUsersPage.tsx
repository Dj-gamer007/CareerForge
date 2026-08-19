import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminService } from '@/services/admin.service';
import { queryKeys } from '@/lib/queryClient';
import { useAuthStore } from '@/features/auth/authStore';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { Modal } from '@/components/ui/Modal';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { formatDate } from '@/lib/utils';
import { Users, ShieldAlert, CheckCircle, Ban, Eye } from 'lucide-react';
import { Role } from '@/types/auth.types';
import { AdminUserSummaryResponse } from '@/types/admin.types';

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((state) => state.user);

  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [roleFilter, setRoleFilter] = useState<Role | ''>('');
  const [enabledFilter, setEnabledFilter] = useState<string>('');

  // Selected User for Detail View / Status Change
  const [selectedUser, setSelectedUser] = useState<AdminUserSummaryResponse | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [statusReason, setStatusReason] = useState('');
  const [targetEnabledState, setTargetEnabledState] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);

  const { data: usersData, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.admin.users({
      page,
      size: 10,
      search: searchInput || undefined,
      role: roleFilter || undefined,
      enabled: enabledFilter !== '' ? enabledFilter === 'true' : undefined,
    }),
    queryFn: () =>
      adminService.getUsers({
        page,
        size: 10,
        search: searchInput || undefined,
        role: roleFilter ? (roleFilter as Role) : undefined,
        enabled: enabledFilter !== '' ? enabledFilter === 'true' : undefined,
      }),
  });

  const { data: userDetail, isLoading: isDetailLoading } = useQuery({
    queryKey: queryKeys.admin.userDetail(selectedUser?.id || 0),
    queryFn: () => adminService.getUserById(selectedUser!.id),
    enabled: isDetailModalOpen && !!selectedUser?.id,
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, enabled, reason }: { id: number; enabled: boolean; reason: string }) =>
      adminService.updateUserStatus(id, { enabled, reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
      if (selectedUser) {
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.userDetail(selectedUser.id) });
      }
      setIsStatusModalOpen(false);
      setStatusReason('');
      setStatusError(null);
    },
    onError: (err: any) => {
      setStatusError(err.response?.data?.message || 'Failed to update user status');
    },
  });

  const openStatusModal = (user: AdminUserSummaryResponse, enabled: boolean) => {
    setSelectedUser(user);
    setTargetEnabledState(enabled);
    setStatusReason('');
    setStatusError(null);
    setIsStatusModalOpen(true);
  };

  const openDetailModal = (user: AdminUserSummaryResponse) => {
    setSelectedUser(user);
    setIsDetailModalOpen(true);
  };

  if (isLoading) return <LoadingSpinner text="Loading user directory..." />;
  if (isError) {
    return (
      <ErrorState
        title="Could not load users"
        message={(error as any)?.response?.data?.message || 'Failed to fetch user accounts'}
        onRetry={() => refetch()}
      />
    );
  }

  const users = usersData?.content || [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="User Directory & Access Governance"
        description="Search platform accounts, inspect linked student/recruiter profiles, and manage active status"
      />

      {/* Filter Controls */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Input
          placeholder="Search by email, name..."
          value={searchInput}
          onChange={(e) => {
            setSearchInput(e.target.value);
            setPage(0);
          }}
        />

        <Select
          options={[
            { label: 'All Roles', value: '' },
            { label: 'Students', value: 'ROLE_STUDENT' },
            { label: 'Recruiters', value: 'ROLE_RECRUITER' },
            { label: 'Admins', value: 'ROLE_ADMIN' },
          ]}
          value={roleFilter}
          onChange={(e) => {
            setRoleFilter(e.target.value as Role | '');
            setPage(0);
          }}
        />

        <Select
          options={[
            { label: 'All Account Statuses', value: '' },
            { label: 'Enabled Accounts', value: 'true' },
            { label: 'Disabled Accounts', value: 'false' },
          ]}
          value={enabledFilter}
          onChange={(e) => {
            setEnabledFilter(e.target.value);
            setPage(0);
          }}
        />
      </div>

      {/* Users Table */}
      <Card>
        <CardContent className="p-0 overflow-x-auto">
          {users.length === 0 ? (
            <EmptyState
              icon={<Users className="w-8 h-8 text-slate-400" />}
              title="No users found"
              description="No user accounts match the current filter criteria."
            />
          ) : (
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3">User</th>
                  <th className="px-6 py-3">Role</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Registered Date</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {users.map((u) => {
                  const isSelf = currentUser?.id === u.id;

                  return (
                    <tr key={u.id} className="hover:bg-slate-50/60 transition-colors">
                      <td className="px-6 py-4">
                        <p className="font-bold text-slate-900">{u.email}</p>
                        {u.fullName && <p className="text-xs text-slate-400">{u.fullName}</p>}
                      </td>
                      <td className="px-6 py-4">
                        <Badge variant="outline">{u.role.replace('ROLE_', '')}</Badge>
                      </td>
                      <td className="px-6 py-4">
                        <Badge variant={u.enabled ? 'success' : 'destructive'}>
                          {u.enabled ? 'Enabled' : 'Disabled'}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-xs">{formatDate(u.createdAt)}</td>
                      <td className="px-6 py-4 text-right space-x-2">
                        <Button size="sm" variant="ghost" onClick={() => openDetailModal(u)}>
                          <Eye className="w-4 h-4 mr-1" />
                          Inspect
                        </Button>

                        {/* Self-disablement prevention guard in UI */}
                        {u.enabled ? (
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-rose-600 border-rose-200 hover:bg-rose-50"
                            disabled={isSelf}
                            title={isSelf ? 'Cannot disable your own administrative account' : 'Disable account'}
                            onClick={() => openStatusModal(u, false)}
                          >
                            <Ban className="w-3.5 h-3.5 mr-1" />
                            Disable
                          </Button>
                        ) : (
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-emerald-700 border-emerald-200 hover:bg-emerald-50"
                            onClick={() => openStatusModal(u, true)}
                          >
                            <CheckCircle className="w-3.5 h-3.5 mr-1" />
                            Enable
                          </Button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </CardContent>
        {usersData && (
          <PaginationControls
            currentPage={usersData.number}
            totalPages={usersData.totalPages}
            totalElements={usersData.totalElements}
            pageSize={usersData.size}
            onPageChange={(newPage) => setPage(newPage)}
          />
        )}
      </Card>

      {/* User Detail Inspection Modal */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        title="User Account Details"
        description="Comprehensive profile, role permissions, and linked entities"
        maxWidth="lg"
      >
        {isDetailLoading || !userDetail ? (
          <LoadingSpinner text="Fetching user details..." />
        ) : (
          <div className="space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100 text-xs">
              <div>
                <span className="text-slate-400">User ID</span>
                <p className="font-bold text-slate-900 mt-0.5">#{userDetail.id}</p>
              </div>
              <div>
                <span className="text-slate-400">Email</span>
                <p className="font-bold text-slate-900 mt-0.5">{userDetail.email}</p>
              </div>
              <div>
                <span className="text-slate-400">System Role</span>
                <p className="font-bold text-slate-900 mt-0.5">{userDetail.role}</p>
              </div>
              <div>
                <span className="text-slate-400">Status</span>
                <p className="font-bold mt-0.5">
                  <Badge variant={userDetail.enabled ? 'success' : 'destructive'}>
                    {userDetail.enabled ? 'Active / Enabled' : 'Disabled'}
                  </Badge>
                </p>
              </div>
            </div>

            {/* Linked Student Profile */}
            {userDetail.studentProfile && (
              <div className="space-y-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">Student Profile Information</h4>
                <div className="p-4 rounded-xl border border-slate-200 text-xs space-y-2">
                  <p>
                    <span className="font-bold">Name:</span> {userDetail.studentProfile.firstName} {userDetail.studentProfile.lastName}
                  </p>
                  <p>
                    <span className="font-bold">Completion:</span> {userDetail.studentProfile.profileCompletionPercentage}%
                  </p>
                  <p>
                    <span className="font-bold">Skills:</span> {userDetail.studentProfile.skills?.length || 0} verified skills
                  </p>
                  <p>
                    <span className="font-bold">Resumes:</span> {userDetail.studentProfile.resumes?.length || 0} uploaded
                  </p>
                </div>
              </div>
            )}

            {/* Linked Recruiter Profile */}
            {userDetail.recruiterProfile && (
              <div className="space-y-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">Recruiter Profile Information</h4>
                <div className="p-4 rounded-xl border border-slate-200 text-xs space-y-2">
                  <p>
                    <span className="font-bold">Name:</span> {userDetail.recruiterProfile.firstName} {userDetail.recruiterProfile.lastName}
                  </p>
                  <p>
                    <span className="font-bold">Designation:</span> {userDetail.recruiterProfile.designation} ({userDetail.recruiterProfile.department || 'N/A'})
                  </p>
                  {userDetail.recruiterProfile.company && (
                    <p>
                      <span className="font-bold">Affiliated Company:</span> {userDetail.recruiterProfile.company.name} ({userDetail.recruiterProfile.company.verificationStatus})
                    </p>
                  )}
                </div>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Enable / Disable Status Mutation Modal */}
      <Modal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title={targetEnabledState ? 'Enable User Account' : 'Disable User Account'}
        description={`Updating status for account: ${selectedUser?.email}`}
      >
        <div className="space-y-4">
          {statusError && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-center gap-2 text-rose-700 text-xs">
              <ShieldAlert className="w-4 h-4 shrink-0" />
              <span>{statusError}</span>
            </div>
          )}

          <Textarea
            label="Mandatory Administrative Reason *"
            placeholder="Document reason for audit logging compliance (min. 5 characters)..."
            value={statusReason}
            onChange={(e) => setStatusReason(e.target.value)}
            rows={3}
          />

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" size="sm" onClick={() => setIsStatusModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              variant={targetEnabledState ? 'primary' : 'destructive'}
              onClick={() =>
                updateStatusMutation.mutate({
                  id: selectedUser!.id,
                  enabled: targetEnabledState,
                  reason: statusReason,
                })
              }
              isLoading={updateStatusMutation.isPending}
              disabled={statusReason.trim().length < 5}
            >
              {targetEnabledState ? 'Confirm Enable' : 'Confirm Disable'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
