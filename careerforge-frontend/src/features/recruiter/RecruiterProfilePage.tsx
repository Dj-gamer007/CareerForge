import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { recruiterService } from '@/services/recruiter.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { User, ShieldCheck } from 'lucide-react';

export function RecruiterProfilePage() {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    designation: '',
    department: '',
    phone: '',
  });

  const { data: profile, isLoading } = useQuery({
    queryKey: queryKeys.recruiter.profile,
    queryFn: () => recruiterService.getProfile(),
  });

  const saveMutation = useMutation({
    mutationFn: (payload: any) =>
      profile?.firstName ? recruiterService.updateProfile(payload) : recruiterService.createProfile(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.recruiter.profile });
      setIsEditing(false);
    },
  });

  if (isLoading) return <LoadingSpinner text="Loading recruiter profile..." />;

  const startEdit = () => {
    setForm({
      firstName: profile?.firstName || '',
      lastName: profile?.lastName || '',
      designation: profile?.designation || '',
      department: profile?.department || '',
      phone: profile?.phone || '',
    });
    setIsEditing(true);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Recruiter Representative Profile"
        description="Manage your individual recruiter credentials and departmental designation"
      />

      <Card>
        <CardHeader className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
              <User className="w-5 h-5" />
            </div>
            <div>
              <CardTitle>
                {profile?.firstName && profile?.lastName
                  ? `${profile.firstName} ${profile.lastName}`
                  : 'Recruiter Profile'}
              </CardTitle>
              {profile?.isCompanyAdmin && (
                <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-indigo-600 mt-0.5">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  Designated Company Administrator
                </span>
              )}
            </div>
          </div>
          {!isEditing && (
            <Button size="sm" variant="outline" onClick={startEdit}>
              {profile?.firstName ? 'Edit Profile' : 'Complete Profile'}
            </Button>
          )}
        </CardHeader>
        <CardContent className="p-6">
          {isEditing ? (
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="First Name *"
                  value={form.firstName}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                />
                <Input
                  label="Last Name *"
                  value={form.lastName}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <Input
                  label="Designation / Title *"
                  value={form.designation}
                  onChange={(e) => setForm({ ...form, designation: e.target.value })}
                />
                <Input
                  label="Department"
                  value={form.department}
                  onChange={(e) => setForm({ ...form, department: e.target.value })}
                />
                <Input
                  label="Contact Phone"
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <Button variant="outline" size="sm" onClick={() => setIsEditing(false)}>
                  Cancel
                </Button>
                <Button
                  size="sm"
                  onClick={() => saveMutation.mutate(form)}
                  isLoading={saveMutation.isPending}
                >
                  Save Profile
                </Button>
              </div>
            </div>
          ) : profile?.firstName ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-sm">
              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">Official Email</span>
                <p className="text-slate-900 font-bold mt-0.5">{profile.email}</p>
              </div>
              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">Designation</span>
                <p className="text-slate-900 font-bold mt-0.5">{profile.designation}</p>
              </div>
              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">Department</span>
                <p className="text-slate-700 mt-0.5">{profile.department || 'Not specified'}</p>
              </div>
              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">Contact Phone</span>
                <p className="text-slate-700 mt-0.5">{profile.phone || 'Not specified'}</p>
              </div>
            </div>
          ) : (
            <div className="text-center py-8">
              <p className="text-sm text-slate-500 mb-4">Please set up your recruiter representative details.</p>
              <Button size="sm" onClick={startEdit}>
                Setup Profile
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
