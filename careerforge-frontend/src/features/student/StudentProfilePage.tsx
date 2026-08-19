import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { studentService } from '@/services/student.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { Modal } from '@/components/ui/Modal';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { formatFileSize, formatDate } from '@/lib/utils';
import {
  User,
  Plus,
  Trash2,
  FileCheck,
  UploadCloud,
  Sparkles,
} from 'lucide-react';
import { ProficiencyLevel } from '@/types/student.types';

export function StudentProfilePage() {
  const queryClient = useQueryClient();

  const { data: profile, isLoading } = useQuery({
    queryKey: queryKeys.student.profile,
    queryFn: () => studentService.getProfile(),
  });

  // Profile Edit State
  const [isEditingBio, setIsEditingBio] = useState(false);
  const [bioForm, setBioForm] = useState({
    firstName: '',
    lastName: '',
    phone: '',
    location: '',
    bio: '',
    educationSummary: '',
    githubUrl: '',
    linkedinUrl: '',
    portfolioUrl: '',
  });

  // Modal States
  const [isSkillModalOpen, setIsSkillModalOpen] = useState(false);
  const [skillForm, setSkillForm] = useState<{ skillId: number; proficiency: ProficiencyLevel }>({
    skillId: 1,
    proficiency: 'INTERMEDIATE',
  });

  // Mutations
  const updateBioMutation = useMutation({
    mutationFn: (payload: any) =>
      profile?.firstName ? studentService.updateProfile(payload) : studentService.createProfile(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.student.profile });
      setIsEditingBio(false);
    },
  });

  const addSkillMutation = useMutation({
    mutationFn: (payload: any) => studentService.addSkill(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.student.profile });
      setIsSkillModalOpen(false);
    },
  });

  const deleteSkillMutation = useMutation({
    mutationFn: (skillId: number) => studentService.deleteSkill(skillId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.student.profile }),
  });

  const resumeUploadMutation = useMutation({
    mutationFn: (file: File) => studentService.uploadResume(file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.student.profile }),
  });

  const setActiveResumeMutation = useMutation({
    mutationFn: (id: number) => studentService.setActiveResume(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.student.profile }),
  });

  const deleteResumeMutation = useMutation({
    mutationFn: (id: number) => studentService.deleteResume(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.student.profile }),
  });

  if (isLoading) return <LoadingSpinner text="Loading candidate dossier..." />;

  const startEditBio = () => {
    setBioForm({
      firstName: profile?.firstName || '',
      lastName: profile?.lastName || '',
      phone: profile?.phone || '',
      location: profile?.location || '',
      bio: profile?.bio || '',
      educationSummary: profile?.educationSummary || '',
      githubUrl: profile?.githubUrl || '',
      linkedinUrl: profile?.linkedinUrl || '',
      portfolioUrl: profile?.portfolioUrl || '',
    });
    setIsEditingBio(true);
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      resumeUploadMutation.mutate(file);
    }
  };

  return (
    <div className="space-y-8">
      <PageHeader
        title="Candidate Profile & Dossier"
        description="Maintain your professional bio, verified skills, and resume portfolio for deterministic skill matching"
      />

      {/* Completion Progress Gauge */}
      <Card className="bg-gradient-to-r from-indigo-900 to-slate-900 text-white border-0 shadow-lg">
        <CardContent className="p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="space-y-2 text-center sm:text-left">
            <span className="text-xs uppercase font-bold tracking-widest text-indigo-300">Profile Completeness</span>
            <h2 className="text-3xl font-extrabold">{profile?.profileCompletionPercentage || 0}% Complete</h2>
            <p className="text-xs text-indigo-200 max-w-md">
              A complete profile with verified skills and an active resume increases your compatibility score across verified recruiters.
            </p>
          </div>
          <div className="w-20 h-20 rounded-full border-4 border-indigo-400/30 border-t-indigo-400 flex items-center justify-center font-extrabold text-xl">
            {profile?.profileCompletionPercentage || 0}%
          </div>
        </CardContent>
      </Card>

      {/* Basic Info & Bio */}
      <Card>
        <CardHeader className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <User className="w-5 h-5 text-indigo-600" />
            Personal & Contact Information
          </CardTitle>
          {!isEditingBio && (
            <Button variant="outline" size="sm" onClick={startEditBio}>
              Edit Bio
            </Button>
          )}
        </CardHeader>
        <CardContent className="p-6">
          {isEditingBio ? (
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="First Name"
                  value={bioForm.firstName}
                  onChange={(e) => setBioForm({ ...bioForm, firstName: e.target.value })}
                />
                <Input
                  label="Last Name"
                  value={bioForm.lastName}
                  onChange={(e) => setBioForm({ ...bioForm, lastName: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Phone Number"
                  value={bioForm.phone}
                  onChange={(e) => setBioForm({ ...bioForm, phone: e.target.value })}
                />
                <Input
                  label="Location (City, Country)"
                  value={bioForm.location}
                  onChange={(e) => setBioForm({ ...bioForm, location: e.target.value })}
                />
              </div>

              <Textarea
                label="Professional Summary / Bio"
                value={bioForm.bio}
                onChange={(e) => setBioForm({ ...bioForm, bio: e.target.value })}
                rows={3}
              />

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <Input
                  label="LinkedIn URL"
                  value={bioForm.linkedinUrl}
                  onChange={(e) => setBioForm({ ...bioForm, linkedinUrl: e.target.value })}
                />
                <Input
                  label="GitHub URL"
                  value={bioForm.githubUrl}
                  onChange={(e) => setBioForm({ ...bioForm, githubUrl: e.target.value })}
                />
                <Input
                  label="Portfolio URL"
                  value={bioForm.portfolioUrl}
                  onChange={(e) => setBioForm({ ...bioForm, portfolioUrl: e.target.value })}
                />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <Button variant="outline" size="sm" onClick={() => setIsEditingBio(false)}>
                  Cancel
                </Button>
                <Button
                  size="sm"
                  onClick={() => updateBioMutation.mutate(bioForm)}
                  isLoading={updateBioMutation.isPending}
                >
                  Save Profile
                </Button>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-sm">
              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">Full Name</span>
                <p className="text-slate-900 font-bold mt-0.5">
                  {profile?.firstName && profile?.lastName
                    ? `${profile.firstName} ${profile.lastName}`
                    : 'Profile details not set yet'}
                </p>
              </div>
              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">Email</span>
                <p className="text-slate-900 font-bold mt-0.5">{profile?.email}</p>
              </div>
              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">Location</span>
                <p className="text-slate-700 mt-0.5">{profile?.location || 'Not specified'}</p>
              </div>
              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">Phone</span>
                <p className="text-slate-700 mt-0.5">{profile?.phone || 'Not specified'}</p>
              </div>
              {profile?.bio && (
                <div className="sm:col-span-2">
                  <span className="text-xs text-slate-400 font-semibold uppercase">Bio</span>
                  <p className="text-slate-700 mt-0.5 whitespace-pre-line">{profile.bio}</p>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Skills Matrix */}
      <Card>
        <CardHeader className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-indigo-600" />
            Verified Technical Skills ({profile?.skills?.length || 0})
          </CardTitle>
          <Button size="sm" onClick={() => setIsSkillModalOpen(true)}>
            <Plus className="w-4 h-4 mr-1" />
            Add Skill
          </Button>
        </CardHeader>
        <CardContent className="p-6">
          {!profile?.skills || profile.skills.length === 0 ? (
            <p className="text-xs text-slate-400">No skills added yet. Add skills to enable matching algorithms.</p>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
              {profile.skills.map((s) => (
                <div
                  key={s.id}
                  className="p-3 rounded-lg bg-slate-50 border border-slate-200 flex items-center justify-between text-xs"
                >
                  <div>
                    <span className="font-bold text-slate-900">{s.skillName}</span>
                    <p className="text-[10px] text-slate-500">{s.proficiency}</p>
                  </div>
                  <button
                    type="button"
                    className="text-slate-400 hover:text-rose-600 transition-colors"
                    onClick={() => deleteSkillMutation.mutate(s.skillId)}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Resumes Section */}
      <Card>
        <CardHeader className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <FileCheck className="w-5 h-5 text-indigo-600" />
            Resume Portfolio
          </CardTitle>
          <div>
            <input
              id="resume-upload"
              type="file"
              accept=".pdf,application/pdf"
              className="hidden"
              onChange={handleFileUpload}
              disabled={resumeUploadMutation.isPending}
            />
            <label htmlFor="resume-upload">
              <Button size="sm" type="button" onClick={() => document.getElementById('resume-upload')?.click()} isLoading={resumeUploadMutation.isPending}>
                <UploadCloud className="w-4 h-4 mr-1" />
                Upload PDF Resume
              </Button>
            </label>
          </div>
        </CardHeader>
        <CardContent className="p-6 space-y-3">
          {!profile?.resumes || profile.resumes.length === 0 ? (
            <p className="text-xs text-slate-400">No resumes uploaded yet. Upload a PDF resume (max 5MB).</p>
          ) : (
            profile.resumes.map((r) => (
              <div
                key={r.id}
                className={`p-4 rounded-xl border flex items-center justify-between ${
                  r.isActive ? 'bg-indigo-50/50 border-indigo-200' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-slate-900">{r.originalFileName}</span>
                    {r.isActive && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-600 text-white">
                        ACTIVE RESUME
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    v{r.version} &bull; {formatFileSize(r.fileSize)} &bull; Uploaded {formatDate(r.uploadedAt)}
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  {!r.isActive && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setActiveResumeMutation.mutate(r.id)}
                    >
                      Set Active
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-rose-600 hover:bg-rose-50"
                    onClick={() => deleteResumeMutation.mutate(r.id)}
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      {/* Add Skill Modal */}
      <Modal
        isOpen={isSkillModalOpen}
        onClose={() => setIsSkillModalOpen(false)}
        title="Add Profile Skill"
        description="Select a foundational technology and proficiency level"
      >
        <div className="space-y-4">
          <Select
            label="Skill"
            value={skillForm.skillId}
            onChange={(e) => setSkillForm({ ...skillForm, skillId: Number(e.target.value) })}
            options={[
              { label: 'Java', value: 1 },
              { label: 'Spring Boot', value: 2 },
              { label: 'MySQL', value: 3 },
              { label: 'Git', value: 4 },
              { label: 'Docker', value: 5 },
              { label: 'REST API', value: 6 },
              { label: 'React', value: 7 },
              { label: 'TypeScript', value: 8 },
              { label: 'Python', value: 9 },
              { label: 'Microservices', value: 10 },
            ]}
          />

          <Select
            label="Proficiency Level"
            value={skillForm.proficiency}
            onChange={(e) => setSkillForm({ ...skillForm, proficiency: e.target.value as ProficiencyLevel })}
            options={[
              { label: 'Beginner (1.0x)', value: 'BEGINNER' },
              { label: 'Intermediate (2.0x)', value: 'INTERMEDIATE' },
              { label: 'Advanced (3.0x)', value: 'ADVANCED' },
              { label: 'Expert (4.0x)', value: 'EXPERT' },
            ]}
          />

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" size="sm" onClick={() => setIsSkillModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={() => addSkillMutation.mutate(skillForm)}
              isLoading={addSkillMutation.isPending}
            >
              Add Skill
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
