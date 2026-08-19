import { z } from 'zod';

export const userStatusUpdateSchema = z.object({
  enabled: z.boolean(),
  reason: z.string().min(5, 'Reason must be at least 5 characters').max(500),
});

export type UserStatusUpdateFormData = z.infer<typeof userStatusUpdateSchema>;

export const companyVerificationSchema = z.object({
  verificationStatus: z.enum(['VERIFIED', 'REJECTED']),
  reason: z.string().min(5, 'Reason must be at least 5 characters').max(500),
});

export type CompanyVerificationFormData = z.infer<typeof companyVerificationSchema>;

export const jobModerationSchema = z.object({
  status: z.enum(['DRAFT', 'CLOSED', 'ARCHIVED']),
  reason: z.string().min(5, 'Reason must be at least 5 characters').max(500),
});

export type JobModerationFormData = z.infer<typeof jobModerationSchema>;
