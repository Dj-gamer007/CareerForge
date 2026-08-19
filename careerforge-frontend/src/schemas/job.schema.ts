import { z } from 'zod';

export const jobSkillItemSchema = z.object({
  skillId: z.coerce.number().min(1, 'Skill is required'),
  required: z.boolean().default(true),
  minimumProficiency: z.enum(['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT']),
});

export const jobFormSchema = z
  .object({
    title: z.string().min(3, 'Title must be at least 3 characters').max(100),
    description: z.string().min(20, 'Description must be at least 20 characters'),
    location: z.string().max(100).optional().or(z.literal('')),
    workMode: z.enum(['ONSITE', 'REMOTE', 'HYBRID']),
    jobType: z.enum(['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP']),
    experienceLevel: z.enum(['ENTRY_LEVEL', 'MID_LEVEL', 'SENIOR_LEVEL', 'EXECUTIVE']),
    salaryMin: z.coerce.number().min(0, 'Salary cannot be negative').optional(),
    salaryMax: z.coerce.number().min(0, 'Salary cannot be negative').optional(),
    currency: z.string().default('INR'),
    deadline: z.string().min(1, 'Deadline is required'),
    skills: z.array(jobSkillItemSchema).min(1, 'At least one skill is required'),
  })
  .refine(
    (data) => {
      if (data.salaryMin && data.salaryMax) {
        return data.salaryMax >= data.salaryMin;
      }
      return true;
    },
    {
      message: 'Maximum salary must be greater than or equal to minimum salary',
      path: ['salaryMax'],
    }
  );

export type JobFormData = z.infer<typeof jobFormSchema>;
