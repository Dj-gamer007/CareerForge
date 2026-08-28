import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobService, CreateJobPayload } from '@/services/job.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ArrowLeft, Plus, Trash2, AlertCircle } from 'lucide-react';
import { WorkMode, JobType, ExperienceLevel } from '@/types/job.types';

export function JobEditorPage() {
  const { id } = useParams<{ id: string }>();
  const isEditing = !!id && id !== 'new';
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [form, setForm] = useState<CreateJobPayload>({
    title: '',
    description: '',
    location: '',
    workMode: 'ONSITE',
    jobType: 'FULL_TIME',
    experienceLevel: 'ENTRY_LEVEL',
    salaryMin: undefined,
    salaryMax: undefined,
    currency: 'INR',
    deadline: '',
    skills: [],
  });

  const { data: existingJob, isLoading } = useQuery({
    queryKey: queryKeys.recruiter.jobDetail(Number(id)),
    queryFn: () => jobService.getRecruiterJobById(Number(id)),
    enabled: isEditing,
  });

  useEffect(() => {
    if (existingJob) {
      setForm({
        title: existingJob.title || '',
        description: existingJob.description || '',
        location: existingJob.location || '',
        workMode: existingJob.workMode,
        jobType: existingJob.jobType,
        experienceLevel: existingJob.experienceLevel,
        salaryMin: existingJob.salaryMin,
        salaryMax: existingJob.salaryMax,
        currency: existingJob.currency || 'INR',
        deadline: existingJob.deadline ? existingJob.deadline.split('T')[0] : '',
        skills: existingJob.skills?.map((s) => ({
          skillId: s.skillId,
          required: s.isRequired ?? s.required ?? true,
          minimumProficiency: s.minimumProficiency,
        })) || [],
      });
    }
  }, [existingJob]);

  const saveMutation = useMutation({
    mutationFn: (payload: CreateJobPayload) => {
      // Backend expects deadline with time or ISO format
      const formattedPayload = {
        ...payload,
        deadline: payload.deadline ? `${payload.deadline}T23:59:59` : undefined,
      };
      return isEditing
        ? jobService.updateJob(Number(id), formattedPayload)
        : jobService.createJob(formattedPayload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.recruiter.jobs() });
      queryClient.invalidateQueries({ queryKey: ['public', 'jobs'] });
      queryClient.invalidateQueries({ queryKey: ['student', 'match-preview'] });
      navigate('/recruiter/jobs');
    },
    onError: (err: any) => {
      const fieldErrors = err.response?.data?.fieldErrors;
      if (fieldErrors && typeof fieldErrors === 'object' && Object.keys(fieldErrors).length > 0) {
        const errorDetails = Object.entries(fieldErrors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(', ');
        setErrorMsg(errorDetails || err.response?.data?.message || 'Failed to save job posting.');
      } else {
        setErrorMsg(err.response?.data?.message || 'Failed to save job posting.');
      }
    },
  });

  const handleAddSkill = () => {
    setForm({
      ...form,
      skills: [...form.skills, { skillId: 0, required: true, minimumProficiency: 'INTERMEDIATE' }],
    });
  };

  const handleRemoveSkill = (index: number) => {
    setForm({
      ...form,
      skills: form.skills.filter((_, i) => i !== index),
    });
  };

  const handleSkillChange = (index: number, key: string, value: any) => {
    const updated = [...form.skills];
    updated[index] = { ...updated[index], [key]: value };
    setForm({ ...form, skills: updated });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    if (!form.title || !form.title.trim() || !form.description || !form.description.trim()) {
      setErrorMsg('Please provide a title and job description.');
      return;
    }
    // Filter out unselected skills where skillId <= 0 or not chosen
    const validSkills = form.skills.filter((s) => s.skillId && Number(s.skillId) > 0);

    const payload: CreateJobPayload = {
      ...form,
      title: form.title.trim(),
      description: form.description.trim(),
      location: form.location?.trim() || (form.workMode === 'REMOTE' ? 'Remote' : 'Not Specified'),
      skills: validSkills,
    };
    saveMutation.mutate(payload);
  };

  if (isEditing && isLoading) return <LoadingSpinner text="Loading job details..." />;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <Link to="/recruiter/jobs" className="inline-flex items-center text-xs font-semibold text-slate-500 hover:text-indigo-600">
        <ArrowLeft className="w-4 h-4 mr-1" />
        Back to Job Postings
      </Link>

      <PageHeader
        title={isEditing ? 'Edit Job Posting' : 'Create New Job Opportunity'}
        description="Configure role requirements, proficiency thresholds, and application deadlines"
      />

      {errorMsg && (
        <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-center gap-2 text-rose-700 text-xs">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardContent className="p-6 space-y-4">
            <Input
              label="Job Title *"
              placeholder="e.g. Senior Backend Java Engineer"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
            />

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <Select
                label="Work Mode"
                value={form.workMode}
                onChange={(e) => setForm({ ...form, workMode: e.target.value as WorkMode })}
                options={[
                  { label: 'Onsite', value: 'ONSITE' },
                  { label: 'Remote', value: 'REMOTE' },
                  { label: 'Hybrid', value: 'HYBRID' },
                ]}
              />

              <Select
                label="Job Type"
                value={form.jobType}
                onChange={(e) => setForm({ ...form, jobType: e.target.value as JobType })}
                options={[
                  { label: 'Full Time', value: 'FULL_TIME' },
                  { label: 'Part Time', value: 'PART_TIME' },
                  { label: 'Contract', value: 'CONTRACT' },
                  { label: 'Internship', value: 'INTERNSHIP' },
                ]}
              />

              <Select
                label="Experience Level"
                value={form.experienceLevel}
                onChange={(e) => setForm({ ...form, experienceLevel: e.target.value as ExperienceLevel })}
                options={[
                  { label: 'Entry Level', value: 'ENTRY_LEVEL' },
                  { label: 'Mid Level', value: 'MID_LEVEL' },
                  { label: 'Senior Level', value: 'SENIOR_LEVEL' },
                  { label: 'Executive', value: 'EXECUTIVE' },
                ]}
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <Input
                label="Location"
                placeholder="e.g. Bangalore, India"
                value={form.location}
                onChange={(e) => setForm({ ...form, location: e.target.value })}
              />

              <Input
                label="Min Salary (Annual)"
                type="number"
                value={form.salaryMin || ''}
                onChange={(e) => setForm({ ...form, salaryMin: e.target.value ? Number(e.target.value) : undefined })}
              />

              <Input
                label="Max Salary (Annual)"
                type="number"
                value={form.salaryMax || ''}
                onChange={(e) => setForm({ ...form, salaryMax: e.target.value ? Number(e.target.value) : undefined })}
              />
            </div>

            <Input
              label="Application Deadline *"
              type="date"
              value={form.deadline}
              onChange={(e) => setForm({ ...form, deadline: e.target.value })}
            />

            <Textarea
              label="Job Description & Responsibilities *"
              placeholder="Describe the role expectations, day-to-day duties, and team context..."
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              rows={6}
            />
          </CardContent>
        </Card>

        {/* Technical Skills Requirements Matrix */}
        <Card>
          <CardContent className="p-6 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Required & Optional Skills</h3>
                <p className="text-xs text-slate-500">
                  Defines the mathematical weights for candidate compatibility calculations
                </p>
              </div>
              <Button type="button" size="sm" variant="outline" onClick={handleAddSkill}>
                <Plus className="w-4 h-4 mr-1" />
                Add Skill
              </Button>
            </div>

            {form.skills.length === 0 ? (
              <div className="p-6 text-center rounded-lg border border-dashed border-slate-200 text-xs text-slate-400">
                No skills added yet. Click &quot;Add Skill&quot; above to configure role requirements.
              </div>
            ) : (
              <div className="space-y-3">
                {form.skills.map((skill, index) => (
                  <div key={index} className="p-3 rounded-lg bg-slate-50 border border-slate-200 flex items-center gap-3">
                    <div className="flex-1">
                      <Select
                        value={skill.skillId || 0}
                        onChange={(e) => handleSkillChange(index, 'skillId', Number(e.target.value))}
                        options={[
                          { label: 'Select Skill...', value: 0 },
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
                    </div>

                    <div className="w-44">
                      <Select
                        value={skill.minimumProficiency}
                        onChange={(e) => handleSkillChange(index, 'minimumProficiency', e.target.value)}
                        options={[
                          { label: 'Beginner', value: 'BEGINNER' },
                          { label: 'Intermediate', value: 'INTERMEDIATE' },
                          { label: 'Advanced', value: 'ADVANCED' },
                          { label: 'Expert', value: 'EXPERT' },
                        ]}
                      />
                    </div>

                    <div className="w-32">
                      <Select
                        value={skill.required ? 'true' : 'false'}
                        onChange={(e) => handleSkillChange(index, 'required', e.target.value === 'true')}
                        options={[
                          { label: 'Required (2x)', value: 'true' },
                          { label: 'Optional (1x)', value: 'false' },
                        ]}
                      />
                    </div>

                    <button
                      type="button"
                      className="text-slate-400 hover:text-rose-600 p-2"
                      onClick={() => handleRemoveSkill(index)}
                      title="Remove skill"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" onClick={() => navigate('/recruiter/jobs')}>
            Cancel
          </Button>
          <Button type="submit" isLoading={saveMutation.isPending}>
            {isEditing ? 'Update Job Posting' : 'Create Draft Job'}
          </Button>
        </div>
      </form>
    </div>
  );
}
