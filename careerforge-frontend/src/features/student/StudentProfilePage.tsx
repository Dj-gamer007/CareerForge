import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { studentService } from '@/services/student.service';
import { queryKeys } from '@/lib/queryClient';

import { PageHeader } from '@/components/layout/PageHeader';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/Card';
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
  GraduationCap,
  FolderKanban,
  Award,
  ExternalLink,
  FileText,
  X,
} from 'lucide-react';

import { ProficiencyLevel } from '@/types/student.types';

export function StudentProfilePage() {
  const queryClient = useQueryClient();

  // =========================================================
  // PROFILE QUERY
  // =========================================================

  const {
    data: profile,
    isLoading,
  } = useQuery({
    queryKey: queryKeys.student.profile,
    queryFn: () => studentService.getProfile(),
  });

  // =========================================================
  // SKILLS QUERY
  // =========================================================

  const {
    data: skills = [],
    isLoading: isSkillsLoading,
  } = useQuery({
    queryKey: ['student', 'skills'],
    queryFn: () => studentService.getSkills(),
  });

  // =========================================================
  // RESUMES QUERY
  // =========================================================

  const {
    data: resumes = [],
    isLoading: isResumesLoading,
  } = useQuery({
    queryKey: ['student', 'resumes'],
    queryFn: () => studentService.getResumes(),
  });

  // =========================================================
  // EDUCATION QUERY
  // =========================================================

  const {
    data: education = [],
    isLoading: isEducationLoading,
  } = useQuery({
    queryKey: ['student', 'education'],
    queryFn: () => studentService.getEducation(),
  });

  // =========================================================
  // PROJECTS QUERY
  // =========================================================

  const {
    data: projects = [],
    isLoading: isProjectsLoading,
  } = useQuery({
    queryKey: ['student', 'projects'],
    queryFn: () => studentService.getProjects(),
  });

  // =========================================================
  // CERTIFICATIONS QUERY
  // =========================================================

  const {
    data: certifications = [],
    isLoading: isCertificationsLoading,
  } = useQuery({
    queryKey: ['student', 'certifications'],
    queryFn: () => studentService.getCertifications(),
  });

  // =========================================================
  // PROFILE EDIT STATE
  // =========================================================

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

  // =========================================================
  // SKILL MODAL STATE
  // =========================================================

  const [isSkillModalOpen, setIsSkillModalOpen] = useState(false);

  const [skillForm, setSkillForm] = useState<{
    skillId: number;
    proficiency: ProficiencyLevel;
  }>({
    skillId: 1,
    proficiency: 'INTERMEDIATE',
  });

  // =========================================================
  // EDUCATION MODAL STATE
  // =========================================================

  const [isEducationModalOpen, setIsEducationModalOpen] = useState(false);

  const [educationForm, setEducationForm] = useState({
    institution: '',
    degree: '',
    fieldOfStudy: '',
    startDate: '',
    endDate: '',
    current: false,
    grade: '',
    description: '',
  });

  // =========================================================
  // PROJECT MODAL STATE
  // =========================================================

  const [isProjectModalOpen, setIsProjectModalOpen] = useState(false);

  const [projectForm, setProjectForm] = useState({
    title: '',
    description: '',
    projectUrl: '',
    githubUrl: '',
    technologies: '',
    startDate: '',
    endDate: '',
  });

  // =========================================================
  // CERTIFICATION MODAL STATE
  // =========================================================

  const [isCertificationModalOpen, setIsCertificationModalOpen] =
    useState(false);

  const [certificationForm, setCertificationForm] = useState({
    name: '',
    issuingOrganization: '',
    issueDate: '',
    expirationDate: '',
    credentialId: '',
    credentialUrl: '',
  });

  // =========================================================
  // UPDATE PROFILE MUTATION
  // =========================================================

  const updateBioMutation = useMutation({
    mutationFn: (payload: {
      firstName: string;
      lastName: string;
      phone?: string;
      location?: string;
      bio?: string;
      educationSummary?: string;
      githubUrl?: string;
      linkedinUrl?: string;
      portfolioUrl?: string;
    }) =>
      profile?.firstName
        ? studentService.updateProfile(payload)
        : studentService.createProfile(payload),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });

      setIsEditingBio(false);
    },
  });

  // =========================================================
  // ADD SKILL MUTATION
  // =========================================================

  const addSkillMutation = useMutation({
    mutationFn: (payload: {
      skillId: number;
      proficiency: ProficiencyLevel;
    }) => studentService.addSkill(payload),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['student', 'skills'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });

      setIsSkillModalOpen(false);
    },
  });

  // =========================================================
  // DELETE SKILL MUTATION
  // =========================================================

  const deleteSkillMutation = useMutation({
    mutationFn: (skillId: number) =>
      studentService.deleteSkill(skillId),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['student', 'skills'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });
    },
  });

  // =========================================================
  // ADD EDUCATION MUTATION
  // =========================================================

  const addEducationMutation = useMutation({
    mutationFn: () =>
      studentService.addEducation({
        institution: educationForm.institution,
        degree: educationForm.degree,
        fieldOfStudy: educationForm.fieldOfStudy,
        startDate: educationForm.startDate,
        endDate: educationForm.current
          ? undefined
          : educationForm.endDate || undefined,
        current: educationForm.current,
        grade: educationForm.grade || undefined,
        description: educationForm.description || undefined,
      }),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['student', 'education'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });

      setIsEducationModalOpen(false);

      setEducationForm({
        institution: '',
        degree: '',
        fieldOfStudy: '',
        startDate: '',
        endDate: '',
        current: false,
        grade: '',
        description: '',
      });
    },
  });

  // =========================================================
  // DELETE EDUCATION MUTATION
  // =========================================================

  const deleteEducationMutation = useMutation({
    mutationFn: (id: number) =>
      studentService.deleteEducation(id),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['student', 'education'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });
    },
  });

  // =========================================================
  // ADD PROJECT MUTATION
  // =========================================================

  const addProjectMutation = useMutation({
    mutationFn: () =>
      studentService.addProject({
        title: projectForm.title,
        description: projectForm.description || undefined,
        projectUrl: projectForm.projectUrl || undefined,
        githubUrl: projectForm.githubUrl || undefined,
        technologies: projectForm.technologies || undefined,
        startDate: projectForm.startDate || undefined,
        endDate: projectForm.endDate || undefined,
      }),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['student', 'projects'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });

      setIsProjectModalOpen(false);

      setProjectForm({
        title: '',
        description: '',
        projectUrl: '',
        githubUrl: '',
        technologies: '',
        startDate: '',
        endDate: '',
      });
    },
  });

  // =========================================================
  // DELETE PROJECT MUTATION
  // =========================================================

  const deleteProjectMutation = useMutation({
    mutationFn: (id: number) =>
      studentService.deleteProject(id),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['student', 'projects'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });
    },
  });

  // =========================================================
  // ADD CERTIFICATION MUTATION
  // =========================================================

  const addCertificationMutation = useMutation({
    mutationFn: () =>
      studentService.addCertification({
        name: certificationForm.name,
        issuingOrganization:
          certificationForm.issuingOrganization,
        issueDate:
          certificationForm.issueDate || undefined,
        expirationDate:
          certificationForm.expirationDate || undefined,
        credentialId:
          certificationForm.credentialId || undefined,
        credentialUrl:
          certificationForm.credentialUrl || undefined,
      }),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['student', 'certifications'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });

      setIsCertificationModalOpen(false);

      setCertificationForm({
        name: '',
        issuingOrganization: '',
        issueDate: '',
        expirationDate: '',
        credentialId: '',
        credentialUrl: '',
      });
    },
  });

  // =========================================================
  // DELETE CERTIFICATION MUTATION
  // =========================================================

  const deleteCertificationMutation = useMutation({
    mutationFn: (id: number) =>
      studentService.deleteCertification(id),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['student', 'certifications'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });
    },
  });

  // =========================================================
  // RESUME MUTATION STATE & FEEDBACK
  // =========================================================

  const [deletingResumeId, setDeletingResumeId] = useState<number | null>(null);
  const [resumeActionFeedback, setResumeActionFeedback] = useState<{
    type: 'success' | 'error';
    message: string;
  } | null>(null);

  // =========================================================
  // RESUME UPLOAD MUTATION
  // =========================================================

  const resumeUploadMutation = useMutation({
    mutationFn: (file: File) =>
      studentService.uploadResume(file),

    onMutate: () => {
      setResumeActionFeedback(null);
    },

    onSuccess: () => {
      setResumeActionFeedback({
        type: 'success',
        message: 'Resume uploaded successfully.',
      });

      queryClient.invalidateQueries({
        queryKey: ['student', 'resumes'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });
    },

    onError: (err: any) => {
      setResumeActionFeedback({
        type: 'error',
        message: err?.response?.data?.message || 'Failed to upload resume.',
      });
    },
  });

  // =========================================================
  // SET ACTIVE RESUME MUTATION
  // =========================================================

  const setActiveResumeMutation = useMutation({
    mutationFn: (id: number) =>
      studentService.setActiveResume(id),

    onMutate: () => {
      setResumeActionFeedback(null);
    },

    onSuccess: () => {
      setResumeActionFeedback({
        type: 'success',
        message: 'Active resume updated successfully.',
      });

      queryClient.invalidateQueries({
        queryKey: ['student', 'resumes'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });
    },

    onError: (err: any) => {
      setResumeActionFeedback({
        type: 'error',
        message: err?.response?.data?.message || 'Failed to update active resume.',
      });
    },
  });

  // =========================================================
  // DELETE RESUME MUTATION
  // =========================================================

  const deleteResumeMutation = useMutation({
    mutationFn: (id: number) =>
      studentService.deleteResume(id),

    onMutate: (id: number) => {
      setDeletingResumeId(id);
      setResumeActionFeedback(null);
    },

    onSuccess: () => {
      setResumeActionFeedback({
        type: 'success',
        message: 'Resume removed successfully.',
      });

      queryClient.invalidateQueries({
        queryKey: ['student', 'resumes'],
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.student.profile,
      });
    },

    onError: (err: any) => {
      setResumeActionFeedback({
        type: 'error',
        message: err?.response?.data?.message || 'Failed to delete resume.',
      });
    },

    onSettled: () => {
      setDeletingResumeId(null);
    },
  });

  // =========================================================
  // INITIAL LOADING
  // =========================================================

  if (isLoading) {
    return (
      <LoadingSpinner text="Loading candidate dossier..." />
    );
  }

  // =========================================================
  // PROFILE COMPLETION VALUE
  // =========================================================

  const completionPercentage = Math.min(
    100,
    Math.max(
      0,
      profile?.profileCompletionPercentage || 0
    )
  );

  // =========================================================
  // START EDIT PROFILE
  // =========================================================

  const startEditBio = () => {
    setBioForm({
      firstName: profile?.firstName || '',
      lastName: profile?.lastName || '',
      phone: profile?.phone || '',
      location: profile?.location || '',
      bio: profile?.bio || '',
      educationSummary:
        profile?.educationSummary || '',
      githubUrl: profile?.githubUrl || '',
      linkedinUrl: profile?.linkedinUrl || '',
      portfolioUrl: profile?.portfolioUrl || '',
    });

    setIsEditingBio(true);
  };

  // =========================================================
  // HANDLE RESUME FILE UPLOAD
  // =========================================================

  const handleFileUpload = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = e.target.files?.[0];

    if (file) {
      if (file.type !== 'application/pdf') {
        alert('Please upload a PDF file.');
        e.target.value = '';
        return;
      }

      if (file.size > 5 * 1024 * 1024) {
        alert('Resume must be smaller than 5MB.');
        e.target.value = '';
        return;
      }

      resumeUploadMutation.mutate(file);
    }

    e.target.value = '';
  };

  // =========================================================
  // RENDER
  // =========================================================

  return (
    <div className="space-y-8">

      {/* =====================================================
          PAGE HEADER
      ====================================================== */}

      <PageHeader
        title="Candidate Profile & Dossier"
        description="Maintain your professional profile, education, skills, projects, certifications, and resume portfolio for deterministic skill matching"
      />

      {/* =====================================================
          PROFILE COMPLETION
      ====================================================== */}

      <Card className="bg-gradient-to-r from-indigo-900 to-slate-900 text-white border-0 shadow-lg">
        <CardContent className="p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-6">

          <div className="space-y-2 text-center sm:text-left">

            <span className="text-xs uppercase font-bold tracking-widest text-indigo-300">
              Profile Completeness
            </span>

            <h2 className="text-3xl font-extrabold">
              {completionPercentage}% Complete
            </h2>

            <p className="text-xs text-indigo-200 max-w-md">
              Complete your personal information, education,
              skills, projects, certifications, and active resume
              to build a complete candidate profile.
            </p>

          </div>

          {/* =================================================
              PROFILE COMPLETION CIRCLE
          ================================================== */}

          <div
            className="relative w-24 h-24 rounded-full flex items-center justify-center shrink-0"
            style={{
              background: `conic-gradient(
                #818cf8 ${completionPercentage}%,
                #273268 ${completionPercentage}% 100%
              )`,
            }}
          >
            {/* Inner circle */}
            <div className="absolute inset-[6px] rounded-full bg-[#111a38] flex items-center justify-center">
              <span className="text-xl font-extrabold text-white">
                {completionPercentage}%
              </span>
            </div>
          </div>

        </CardContent>
      </Card>

      {/* =====================================================
          PERSONAL INFORMATION
      ====================================================== */}

      <Card>

        <CardHeader className="flex items-center justify-between">

          <CardTitle className="flex items-center gap-2">
            <User className="w-5 h-5 text-indigo-600" />
            Personal & Contact Information
          </CardTitle>

          {!isEditingBio && (
            <Button
              variant="outline"
              size="sm"
              onClick={startEditBio}
            >
              Edit Profile
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
                  onChange={(e) =>
                    setBioForm({
                      ...bioForm,
                      firstName: e.target.value,
                    })
                  }
                />

                <Input
                  label="Last Name"
                  value={bioForm.lastName}
                  onChange={(e) =>
                    setBioForm({
                      ...bioForm,
                      lastName: e.target.value,
                    })
                  }
                />

              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

                <Input
                  label="Phone Number"
                  value={bioForm.phone}
                  onChange={(e) =>
                    setBioForm({
                      ...bioForm,
                      phone: e.target.value,
                    })
                  }
                />

                <Input
                  label="Location (City, Country)"
                  value={bioForm.location}
                  onChange={(e) =>
                    setBioForm({
                      ...bioForm,
                      location: e.target.value,
                    })
                  }
                />

              </div>

              <Textarea
                label="Professional Summary / Bio"
                value={bioForm.bio}
                onChange={(e) =>
                  setBioForm({
                    ...bioForm,
                    bio: e.target.value,
                  })
                }
                rows={3}
              />

              <Textarea
                label="Education Summary"
                value={bioForm.educationSummary}
                onChange={(e) =>
                  setBioForm({
                    ...bioForm,
                    educationSummary: e.target.value,
                  })
                }
                rows={3}
              />

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">

                <Input
                  label="LinkedIn URL"
                  value={bioForm.linkedinUrl}
                  onChange={(e) =>
                    setBioForm({
                      ...bioForm,
                      linkedinUrl: e.target.value,
                    })
                  }
                />

                <Input
                  label="GitHub URL"
                  value={bioForm.githubUrl}
                  onChange={(e) =>
                    setBioForm({
                      ...bioForm,
                      githubUrl: e.target.value,
                    })
                  }
                />

                <Input
                  label="Portfolio URL"
                  value={bioForm.portfolioUrl}
                  onChange={(e) =>
                    setBioForm({
                      ...bioForm,
                      portfolioUrl: e.target.value,
                    })
                  }
                />

              </div>

              <div className="flex justify-end gap-3 pt-2">

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    setIsEditingBio(false)
                  }
                >
                  Cancel
                </Button>

                <Button
                  size="sm"
                  onClick={() =>
                    updateBioMutation.mutate(bioForm)
                  }
                  isLoading={
                    updateBioMutation.isPending
                  }
                >
                  Save Profile
                </Button>

              </div>

            </div>

          ) : (

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-sm">

              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">
                  Full Name
                </span>

                <p className="text-slate-900 font-bold mt-0.5">
                  {profile?.firstName &&
                  profile?.lastName
                    ? `${profile.firstName} ${profile.lastName}`
                    : 'Profile details not set yet'}
                </p>
              </div>

              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">
                  Email
                </span>

                <p className="text-slate-900 font-bold mt-0.5">
                  {profile?.email}
                </p>
              </div>

              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">
                  Location
                </span>

                <p className="text-slate-700 mt-0.5">
                  {profile?.location ||
                    'Not specified'}
                </p>
              </div>

              <div>
                <span className="text-xs text-slate-400 font-semibold uppercase">
                  Phone
                </span>

                <p className="text-slate-700 mt-0.5">
                  {profile?.phone ||
                    'Not specified'}
                </p>
              </div>

              {profile?.bio && (
                <div className="sm:col-span-2">

                  <span className="text-xs text-slate-400 font-semibold uppercase">
                    Professional Summary / Bio
                  </span>

                  <p className="text-slate-700 mt-0.5 whitespace-pre-line">
                    {profile.bio}
                  </p>

                </div>
              )}

              {profile?.educationSummary && (
                <div className="sm:col-span-2">

                  <span className="text-xs text-slate-400 font-semibold uppercase">
                    Education Summary
                  </span>

                  <p className="text-slate-700 mt-0.5 whitespace-pre-line">
                    {profile.educationSummary}
                  </p>

                </div>
              )}

              {(profile?.linkedinUrl ||
                profile?.githubUrl ||
                profile?.portfolioUrl) && (

                <div className="sm:col-span-2">

                  <span className="text-xs text-slate-400 font-semibold uppercase">
                    Professional Links
                  </span>

                  <div className="flex flex-wrap gap-4 mt-2">

                    {profile?.linkedinUrl && (
                      <a
                        href={profile.linkedinUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-sm text-indigo-600 hover:underline flex items-center gap-1"
                      >
                        LinkedIn
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    )}

                    {profile?.githubUrl && (
                      <a
                        href={profile.githubUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-sm text-indigo-600 hover:underline flex items-center gap-1"
                      >
                        GitHub
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    )}

                    {profile?.portfolioUrl && (
                      <a
                        href={profile.portfolioUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-sm text-indigo-600 hover:underline flex items-center gap-1"
                      >
                        Portfolio
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    )}

                  </div>

                </div>
              )}

            </div>

          )}

        </CardContent>
      </Card>

      {/* =====================================================
          EDUCATION
      ====================================================== */}

      <Card>

        <CardHeader className="flex items-center justify-between">

          <CardTitle className="flex items-center gap-2">
            <GraduationCap className="w-5 h-5 text-indigo-600" />
            Education ({education.length})
          </CardTitle>

          <Button
            size="sm"
            onClick={() =>
              setIsEducationModalOpen(true)
            }
          >
            <Plus className="w-4 h-4 mr-1" />
            Add Education
          </Button>

        </CardHeader>

        <CardContent className="p-6">

          {isEducationLoading ? (

            <LoadingSpinner text="Loading education..." />

          ) : education.length === 0 ? (

            <p className="text-xs text-slate-400">
              No education records added yet. Add your degree
              or academic qualification to improve profile
              completeness.
            </p>

          ) : (

            <div className="space-y-4">

              {education.map((item) => (

                <div
                  key={item.id}
                  className="p-4 rounded-xl border border-slate-200 bg-slate-50"
                >

                  <div className="flex items-start justify-between gap-4">

                    <div className="space-y-1">

                      <h3 className="text-sm font-bold text-slate-900">
                        {item.degree}
                      </h3>

                      <p className="text-sm font-semibold text-indigo-600">
                        {item.fieldOfStudy}
                      </p>

                      <p className="text-xs text-slate-600">
                        {item.institution}
                      </p>

                      <p className="text-xs text-slate-400">
                        {item.startDate}
                        {' - '}
                        {item.current
                          ? 'Present'
                          : item.endDate ||
                            'Present'}
                      </p>

                      {item.grade && (
                        <p className="text-xs text-slate-500">
                          Grade: {item.grade}
                        </p>
                      )}

                      {item.description && (
                        <p className="text-xs text-slate-600 mt-2 whitespace-pre-line">
                          {item.description}
                        </p>
                      )}

                    </div>

                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-rose-600 hover:bg-rose-50"
                      onClick={() =>
                        deleteEducationMutation.mutate(
                          item.id
                        )
                      }
                      isLoading={
                        deleteEducationMutation.isPending
                      }
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>

                  </div>

                </div>

              ))}

            </div>

          )}

        </CardContent>
      </Card>

      {/* =====================================================
          SKILLS
      ====================================================== */}

      <Card>

        <CardHeader className="flex items-center justify-between">

          <CardTitle className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-indigo-600" />
            Verified Technical Skills ({skills.length})
          </CardTitle>

          <Button
            size="sm"
            onClick={() =>
              setIsSkillModalOpen(true)
            }
          >
            <Plus className="w-4 h-4 mr-1" />
            Add Skill
          </Button>

        </CardHeader>

        <CardContent className="p-6">

          {isSkillsLoading ? (

            <LoadingSpinner text="Loading skills..." />

          ) : skills.length === 0 ? (

            <p className="text-xs text-slate-400">
              No skills added yet. Add skills to enable
              matching algorithms.
            </p>

          ) : (

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">

              {skills.map((s) => (

                <div
                  key={s.id}
                  className="p-3 rounded-lg bg-slate-50 border border-slate-200 flex items-center justify-between text-xs"
                >

                  <div>

                    <span className="font-bold text-slate-900">
                      {s.skillName}
                    </span>

                    <p className="text-[10px] text-slate-500">
                      {s.proficiency}
                    </p>

                  </div>

                  <button
                    type="button"
                    className="text-slate-400 hover:text-rose-600 transition-colors"
                    onClick={() =>
                      deleteSkillMutation.mutate(
                        s.skillId
                      )
                    }
                    disabled={
                      deleteSkillMutation.isPending
                    }
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>

                </div>

              ))}

            </div>

          )}

        </CardContent>
      </Card>

      {/* =====================================================
          PROJECTS
      ====================================================== */}

      <Card>

        <CardHeader className="flex items-center justify-between">

          <CardTitle className="flex items-center gap-2">
            <FolderKanban className="w-5 h-5 text-indigo-600" />
            Projects ({projects.length})
          </CardTitle>

          <Button
            size="sm"
            onClick={() =>
              setIsProjectModalOpen(true)
            }
          >
            <Plus className="w-4 h-4 mr-1" />
            Add Project
          </Button>

        </CardHeader>

        <CardContent className="p-6">

          {isProjectsLoading ? (

            <LoadingSpinner text="Loading projects..." />

          ) : projects.length === 0 ? (

            <p className="text-xs text-slate-400">
              No projects added yet. Add your projects to
              improve your profile completeness.
            </p>

          ) : (

            <div className="space-y-4">

              {projects.map((project) => (

                <div
                  key={project.id}
                  className="p-4 rounded-xl border border-slate-200 bg-slate-50"
                >

                  <div className="flex items-start justify-between gap-4">

                    <div className="space-y-1 min-w-0">

                      <h3 className="text-sm font-bold text-slate-900">
                        {project.title}
                      </h3>

                      {project.description && (
                        <p className="text-xs text-slate-600 whitespace-pre-line">
                          {project.description}
                        </p>
                      )}

                      {project.technologies && (
                        <p className="text-xs text-slate-500">
                          <span className="font-semibold">
                            Technologies:
                          </span>{' '}
                          {project.technologies}
                        </p>
                      )}

                      {(project.startDate ||
                        project.endDate) && (
                        <p className="text-xs text-slate-400">
                          {project.startDate || ''}

                          {project.startDate &&
                            project.endDate
                            ? ' - '
                            : ''}

                          {project.endDate || ''}
                        </p>
                      )}

                      <div className="flex flex-wrap gap-4 pt-1">

                        {project.githubUrl && (
                          <a
                            href={project.githubUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="text-xs text-indigo-600 hover:underline flex items-center gap-1"
                          >
                            GitHub
                            <ExternalLink className="w-3 h-3" />
                          </a>
                        )}

                        {project.projectUrl && (
                          <a
                            href={project.projectUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="text-xs text-indigo-600 hover:underline flex items-center gap-1"
                          >
                            Project Link
                            <ExternalLink className="w-3 h-3" />
                          </a>
                        )}

                      </div>

                    </div>

                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-rose-600 hover:bg-rose-50"
                      onClick={() =>
                        deleteProjectMutation.mutate(
                          project.id
                        )
                      }
                      isLoading={
                        deleteProjectMutation.isPending
                      }
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>

                  </div>

                </div>

              ))}

            </div>

          )}

        </CardContent>
      </Card>

      {/* =====================================================
          CERTIFICATIONS
      ====================================================== */}

      <Card>

        <CardHeader className="flex items-center justify-between">

          <CardTitle className="flex items-center gap-2">
            <Award className="w-5 h-5 text-indigo-600" />
            Certifications ({certifications.length})
          </CardTitle>

          <Button
            size="sm"
            onClick={() =>
              setIsCertificationModalOpen(true)
            }
          >
            <Plus className="w-4 h-4 mr-1" />
            Add Certification
          </Button>

        </CardHeader>

        <CardContent className="p-6">

          {isCertificationsLoading ? (

            <LoadingSpinner text="Loading certifications..." />

          ) : certifications.length === 0 ? (

            <p className="text-xs text-slate-400">
              No certifications added yet. Add your
              certifications to complete your professional
              profile.
            </p>

          ) : (

            <div className="space-y-4">

              {certifications.map((certification) => (

                <div
                  key={certification.id}
                  className="p-4 rounded-xl border border-slate-200 bg-slate-50"
                >

                  <div className="flex items-start justify-between gap-4">

                    <div className="space-y-1">

                      <h3 className="text-sm font-bold text-slate-900">
                        {certification.name}
                      </h3>

                      <p className="text-xs font-semibold text-indigo-600">
                        {certification.issuingOrganization}
                      </p>

                      {certification.issueDate && (
                        <p className="text-xs text-slate-500">
                          Issued:{' '}
                          {certification.issueDate}
                        </p>
                      )}

                      {certification.expirationDate && (
                        <p className="text-xs text-slate-500">
                          Expires:{' '}
                          {certification.expirationDate}
                        </p>
                      )}

                      {certification.credentialId && (
                        <p className="text-xs text-slate-500">
                          Credential ID:{' '}
                          {certification.credentialId}
                        </p>
                      )}

                      {certification.credentialUrl && (
                        <a
                          href={
                            certification.credentialUrl
                          }
                          target="_blank"
                          rel="noreferrer"
                          className="text-xs text-indigo-600 hover:underline flex items-center gap-1"
                        >
                          View Credential
                          <ExternalLink className="w-3 h-3" />
                        </a>
                      )}

                    </div>

                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-rose-600 hover:bg-rose-50"
                      onClick={() =>
                        deleteCertificationMutation.mutate(
                          certification.id
                        )
                      }
                      isLoading={
                        deleteCertificationMutation.isPending
                      }
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>

                  </div>

                </div>

              ))}

            </div>

          )}

        </CardContent>
      </Card>

      {/* =====================================================
          RESUME PORTFOLIO
      ====================================================== */}

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
              disabled={
                resumeUploadMutation.isPending
              }
            />

            <Button
              size="sm"
              type="button"
              onClick={() =>
                document
                  .getElementById('resume-upload')
                  ?.click()
              }
              isLoading={
                resumeUploadMutation.isPending
              }
            >
              <UploadCloud className="w-4 h-4 mr-1" />
              Upload PDF Resume
            </Button>

          </div>

        </CardHeader>

        <CardContent className="p-6 space-y-4">

          {resumeActionFeedback && (
            <div
              className={`p-3 rounded-lg text-xs flex items-center justify-between transition-all ${
                resumeActionFeedback.type === 'success'
                  ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                  : 'bg-rose-50 text-rose-800 border border-rose-200'
              }`}
            >
              <span>{resumeActionFeedback.message}</span>
              <button
                type="button"
                onClick={() => setResumeActionFeedback(null)}
                className="text-slate-400 hover:text-slate-600 focus:outline-none"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {isResumesLoading ? (

            <LoadingSpinner text="Loading resumes..." />

          ) : resumes.length === 0 ? (

            <div className="text-center py-6 border-2 border-dashed border-slate-200 rounded-xl">
              <FileText className="w-8 h-8 text-slate-300 mx-auto mb-2" />
              <p className="text-xs font-semibold text-slate-700">
                No resumes uploaded yet
              </p>
              <p className="text-[11px] text-slate-400 mt-1">
                Upload a PDF resume (max 5MB) to apply for opportunities and complete your profile.
              </p>
            </div>

          ) : (

            resumes.map((r) => (

              <div
                key={r.id}
                className={`p-4 rounded-xl border flex items-center justify-between ${
                  r.active
                    ? 'bg-indigo-50/50 border-indigo-200'
                    : 'bg-slate-50 border-slate-200'
                }`}
              >

                <div>

                  <div className="flex items-center gap-2">

                    <span className="text-sm font-bold text-slate-900">
                      {r.originalFileName}
                    </span>

                    {r.active && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-600 text-white">
                        ACTIVE RESUME
                      </span>
                    )}

                  </div>

                  <p className="text-xs text-slate-500 mt-1">
                    v{r.version}
                    &bull; {formatFileSize(r.fileSize)}
                    &bull; Uploaded{' '}
                    {formatDate(r.uploadedAt)}
                  </p>

                </div>

                <div className="flex items-center gap-2">

                  {!r.active && (

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        setActiveResumeMutation.mutate(
                          r.id
                        )
                      }
                      isLoading={
                        setActiveResumeMutation.isPending
                      }
                      disabled={
                        setActiveResumeMutation.isPending ||
                        deleteResumeMutation.isPending
                      }
                    >
                      Set Active
                    </Button>

                  )}

                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-rose-600 hover:bg-rose-50"
                    onClick={() =>
                      deleteResumeMutation.mutate(
                        r.id
                      )
                    }
                    isLoading={
                      deletingResumeId === r.id
                    }
                    disabled={
                      deleteResumeMutation.isPending ||
                      setActiveResumeMutation.isPending
                    }
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>

                </div>

              </div>

            ))

          )}

        </CardContent>
      </Card>

      {/* =====================================================
          ADD SKILL MODAL
      ====================================================== */}

      <Modal
        isOpen={isSkillModalOpen}
        onClose={() =>
          setIsSkillModalOpen(false)
        }
        title="Add Profile Skill"
        description="Select a foundational technology and proficiency level"
      >

        <div className="space-y-4">

          <Select
            label="Skill"
            value={skillForm.skillId}
            onChange={(e) =>
              setSkillForm({
                ...skillForm,
                skillId: Number(e.target.value),
              })
            }
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
            onChange={(e) =>
              setSkillForm({
                ...skillForm,
                proficiency:
                  e.target.value as ProficiencyLevel,
              })
            }
            options={[
              {
                label: 'Beginner (1.0x)',
                value: 'BEGINNER',
              },
              {
                label: 'Intermediate (2.0x)',
                value: 'INTERMEDIATE',
              },
              {
                label: 'Advanced (3.0x)',
                value: 'ADVANCED',
              },
              {
                label: 'Expert (4.0x)',
                value: 'EXPERT',
              },
            ]}
          />

          <div className="flex justify-end gap-3 pt-2">

            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                setIsSkillModalOpen(false)
              }
            >
              Cancel
            </Button>

            <Button
              size="sm"
              onClick={() =>
                addSkillMutation.mutate(skillForm)
              }
              isLoading={
                addSkillMutation.isPending
              }
            >
              Add Skill
            </Button>

          </div>

        </div>

      </Modal>

      {/* =====================================================
          ADD EDUCATION MODAL
      ====================================================== */}

      <Modal
        isOpen={isEducationModalOpen}
        onClose={() =>
          setIsEducationModalOpen(false)
        }
        title="Add Education"
        description="Add your academic qualification"
      >

        <div className="space-y-4">

          <Input
            label="Institution"
            placeholder="e.g. Rajalakshmi Institute of Technology"
            value={educationForm.institution}
            onChange={(e) =>
              setEducationForm({
                ...educationForm,
                institution: e.target.value,
              })
            }
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

            <Input
              label="Degree"
              placeholder="e.g. B.Tech"
              value={educationForm.degree}
              onChange={(e) =>
                setEducationForm({
                  ...educationForm,
                  degree: e.target.value,
                })
              }
            />

            <Input
              label="Field of Study"
              placeholder="e.g. AI & Data Science"
              value={educationForm.fieldOfStudy}
              onChange={(e) =>
                setEducationForm({
                  ...educationForm,
                  fieldOfStudy: e.target.value,
                })
              }
            />

          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

            <Input
              label="Start Date"
              type="date"
              value={educationForm.startDate}
              onChange={(e) =>
                setEducationForm({
                  ...educationForm,
                  startDate: e.target.value,
                })
              }
            />

            <Input
              label="End Date"
              type="date"
              value={educationForm.endDate}
              disabled={educationForm.current}
              onChange={(e) =>
                setEducationForm({
                  ...educationForm,
                  endDate: e.target.value,
                })
              }
            />

          </div>

          <label className="flex items-center gap-2 text-sm text-slate-700">

            <input
              type="checkbox"
              checked={educationForm.current}
              onChange={(e) =>
                setEducationForm({
                  ...educationForm,
                  current: e.target.checked,
                  endDate: e.target.checked
                    ? ''
                    : educationForm.endDate,
                })
              }
              className="w-4 h-4"
            />

            Currently studying here

          </label>

          <Input
            label="Grade / CGPA"
            placeholder="e.g. 8.5 CGPA"
            value={educationForm.grade}
            onChange={(e) =>
              setEducationForm({
                ...educationForm,
                grade: e.target.value,
              })
            }
          />

          <Textarea
            label="Description"
            placeholder="Optional details about your education"
            rows={3}
            value={educationForm.description}
            onChange={(e) =>
              setEducationForm({
                ...educationForm,
                description: e.target.value,
              })
            }
          />

          <div className="flex justify-end gap-3 pt-2">

            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                setIsEducationModalOpen(false)
              }
            >
              Cancel
            </Button>

            <Button
              size="sm"
              onClick={() =>
                addEducationMutation.mutate()
              }
              isLoading={
                addEducationMutation.isPending
              }
              disabled={
                !educationForm.institution ||
                !educationForm.degree ||
                !educationForm.fieldOfStudy ||
                !educationForm.startDate
              }
            >
              Add Education
            </Button>

          </div>

        </div>

      </Modal>

      {/* =====================================================
          ADD PROJECT MODAL
      ====================================================== */}

      <Modal
        isOpen={isProjectModalOpen}
        onClose={() =>
          setIsProjectModalOpen(false)
        }
        title="Add Project"
        description="Add a project to your professional portfolio"
      >

        <div className="space-y-4">

          <Input
            label="Project Title"
            placeholder="e.g. CareerForge"
            value={projectForm.title}
            onChange={(e) =>
              setProjectForm({
                ...projectForm,
                title: e.target.value,
              })
            }
          />

          <Textarea
            label="Description"
            placeholder="Describe your project and your contribution"
            rows={4}
            value={projectForm.description}
            onChange={(e) =>
              setProjectForm({
                ...projectForm,
                description: e.target.value,
              })
            }
          />

          <Input
            label="Technologies"
            placeholder="e.g. Java, Spring Boot, React, MySQL"
            value={projectForm.technologies}
            onChange={(e) =>
              setProjectForm({
                ...projectForm,
                technologies: e.target.value,
              })
            }
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

            <Input
              label="Start Date"
              type="date"
              value={projectForm.startDate}
              onChange={(e) =>
                setProjectForm({
                  ...projectForm,
                  startDate: e.target.value,
                })
              }
            />

            <Input
              label="End Date"
              type="date"
              value={projectForm.endDate}
              onChange={(e) =>
                setProjectForm({
                  ...projectForm,
                  endDate: e.target.value,
                })
              }
            />

          </div>

          <Input
            label="GitHub URL"
            placeholder="https://github.com/..."
            value={projectForm.githubUrl}
            onChange={(e) =>
              setProjectForm({
                ...projectForm,
                githubUrl: e.target.value,
              })
            }
          />

          <Input
            label="Project URL"
            placeholder="https://..."
            value={projectForm.projectUrl}
            onChange={(e) =>
              setProjectForm({
                ...projectForm,
                projectUrl: e.target.value,
              })
            }
          />

          <div className="flex justify-end gap-3 pt-2">

            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                setIsProjectModalOpen(false)
              }
            >
              Cancel
            </Button>

            <Button
              size="sm"
              onClick={() =>
                addProjectMutation.mutate()
              }
              isLoading={
                addProjectMutation.isPending
              }
              disabled={!projectForm.title}
            >
              Add Project
            </Button>

          </div>

        </div>

      </Modal>

      {/* =====================================================
          ADD CERTIFICATION MODAL
      ====================================================== */}

      <Modal
        isOpen={isCertificationModalOpen}
        onClose={() =>
          setIsCertificationModalOpen(false)
        }
        title="Add Certification"
        description="Add a professional or technical certification"
      >

        <div className="space-y-4">

          <Input
            label="Certification Name"
            placeholder="e.g. Java Programming Certification"
            value={certificationForm.name}
            onChange={(e) =>
              setCertificationForm({
                ...certificationForm,
                name: e.target.value,
              })
            }
          />

          <Input
            label="Issuing Organization"
            placeholder="e.g. Oracle"
            value={
              certificationForm.issuingOrganization
            }
            onChange={(e) =>
              setCertificationForm({
                ...certificationForm,
                issuingOrganization:
                  e.target.value,
              })
            }
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

            <Input
              label="Issue Date"
              type="date"
              value={certificationForm.issueDate}
              onChange={(e) =>
                setCertificationForm({
                  ...certificationForm,
                  issueDate: e.target.value,
                })
              }
            />

            <Input
              label="Expiration Date"
              type="date"
              value={
                certificationForm.expirationDate
              }
              onChange={(e) =>
                setCertificationForm({
                  ...certificationForm,
                  expirationDate:
                    e.target.value,
                })
              }
            />

          </div>

          <Input
            label="Credential ID"
            placeholder="Optional"
            value={certificationForm.credentialId}
            onChange={(e) =>
              setCertificationForm({
                ...certificationForm,
                credentialId: e.target.value,
              })
            }
          />

          <Input
            label="Credential URL"
            placeholder="https://..."
            value={certificationForm.credentialUrl}
            onChange={(e) =>
              setCertificationForm({
                ...certificationForm,
                credentialUrl: e.target.value,
              })
            }
          />

          <div className="flex justify-end gap-3 pt-2">

            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                setIsCertificationModalOpen(false)
              }
            >
              Cancel
            </Button>

            <Button
              size="sm"
              onClick={() =>
                addCertificationMutation.mutate()
              }
              isLoading={
                addCertificationMutation.isPending
              }
              disabled={
                !certificationForm.name ||
                !certificationForm.issuingOrganization
              }
            >
              Add Certification
            </Button>

          </div>

        </div>

      </Modal>

    </div>
  );
}

export default StudentProfilePage;