import { z } from 'zod';

export const companyFormSchema = z.object({
  name: z.string().min(2, 'Company name must be at least 2 characters').max(100),
  website: z.string().url('Invalid website URL').optional().or(z.literal('')),
  logoUrl: z.string().url('Invalid logo URL').optional().or(z.literal('')),
  description: z.string().max(2000).optional().or(z.literal('')),
  industry: z.string().max(50).optional().or(z.literal('')),
  companySize: z.string().max(50).optional().or(z.literal('')),
  location: z.string().max(100).optional().or(z.literal('')),
});

export type CompanyFormData = z.infer<typeof companyFormSchema>;

export const recruiterProfileSchema = z.object({
  firstName: z.string().min(1, 'First name is required').max(50),
  lastName: z.string().min(1, 'Last name is required').max(50),
  designation: z.string().min(1, 'Designation is required').max(100),
  department: z.string().max(100).optional().or(z.literal('')),
  phone: z.string().max(20).optional().or(z.literal('')),
});

export type RecruiterProfileFormData = z.infer<typeof recruiterProfileSchema>;
