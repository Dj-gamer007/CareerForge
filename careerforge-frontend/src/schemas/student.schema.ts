import { z } from 'zod';

export const studentProfileSchema = z.object({
  firstName: z.string().min(1, 'First name is required').max(50),
  lastName: z.string().min(1, 'Last name is required').max(50),
  phone: z.string().max(20).optional().or(z.literal('')),
  location: z.string().max(100).optional().or(z.literal('')),
  bio: z.string().max(1000).optional().or(z.literal('')),
  educationSummary: z.string().max(500).optional().or(z.literal('')),
  githubUrl: z.string().url('Invalid GitHub URL').optional().or(z.literal('')),
  linkedinUrl: z.string().url('Invalid LinkedIn URL').optional().or(z.literal('')),
  portfolioUrl: z.string().url('Invalid Portfolio URL').optional().or(z.literal('')),
});

export type StudentProfileFormData = z.infer<typeof studentProfileSchema>;

export const studentSkillSchema = z.object({
  skillId: z.coerce.number().min(1, 'Skill is required'),
  proficiency: z.enum(['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT']),
});

export type StudentSkillFormData = z.infer<typeof studentSkillSchema>;

export const educationSchema = z.object({
  institution: z.string().min(2, 'Institution is required').max(150),
  degree: z.string().min(2, 'Degree is required').max(100),
  fieldOfStudy: z.string().min(2, 'Field of study is required').max(100),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().optional().or(z.literal('')),
  current: z.boolean().default(false),
  grade: z.string().max(20).optional().or(z.literal('')),
  description: z.string().max(1000).optional().or(z.literal('')),
});

export type EducationFormData = z.infer<typeof educationSchema>;

export const projectSchema = z.object({
  title: z.string().min(2, 'Project title is required').max(100),
  description: z.string().max(1000).optional().or(z.literal('')),
  projectUrl: z.string().url('Invalid Project URL').optional().or(z.literal('')),
  githubUrl: z.string().url('Invalid GitHub URL').optional().or(z.literal('')),
  technologies: z.string().max(255).optional().or(z.literal('')),
  startDate: z.string().optional().or(z.literal('')),
  endDate: z.string().optional().or(z.literal('')),
});

export type ProjectFormData = z.infer<typeof projectSchema>;

export const certificationSchema = z.object({
  name: z.string().min(2, 'Certification name is required').max(150),
  issuingOrganization: z.string().min(2, 'Issuing organization is required').max(150),
  issueDate: z.string().optional().or(z.literal('')),
  expirationDate: z.string().optional().or(z.literal('')),
  credentialId: z.string().max(100).optional().or(z.literal('')),
  credentialUrl: z.string().url('Invalid Credential URL').optional().or(z.literal('')),
});

export type CertificationFormData = z.infer<typeof certificationSchema>;
